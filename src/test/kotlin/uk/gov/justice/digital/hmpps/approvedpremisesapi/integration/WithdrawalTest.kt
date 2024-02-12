package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawableType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.jsonForObject
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

/**
 * This test class tests common withdrawal functionality that spans multiple entity types and accordingly,
 * multiple API endpoints. Specifically:
 *
 * 1. Getting a list of withdrawable elements
 * 2. Cascading withdrawals
 *
 * When considering withdrawals, an application can be considered as a tree or elements:
 *
 * ```
 * application
 *  - assessment
 *    - placement request
 *      - booking
 *    - placement application
 *      - placement request
 *        - booking
 * ```
 *
 * Withdrawals should cascade down the tree
 *
 * Note : The general functionality of each entities' withdrawal endpoint is tested in the corresponding API Test class
 */
class WithdrawalTest : IntegrationTestBase() {

  @Nested
  inner class GetWithdrawables {

    @Test
    fun `Get withdrawables for an application returns empty list if no associated withdrawables`() {
      `Given a User` { applicationCreator, _ ->
        `Given a User` { _, jwt ->
          `Given an Offender` { offenderDetails, _ ->
            val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, applicationCreator, "TEAM")

            webTestClient.get()
              .uri("/applications/${application.id}/withdrawables")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json("[]")
          }
        }
      }
    }

    @Test
    fun `Get withdrawables for an application returns the application if user is application creator`() {
      `Given a User` { applicationCreator, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, applicationCreator, "TEAM")

          val expected = listOf(
            Withdrawable(
              application.id,
              WithdrawableType.application,
              emptyList(),
            ),
          )

          webTestClient.get()
            .uri("/applications/${application.id}/withdrawables")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonForObject(expected)
        }
      }
    }

    @Test
    fun `Get withdrawables for an application returns withdrawable placement requests for application creator`() {
      `Given a User` { applicant, jwt ->
        `Given a User` { allocatedTo, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val (application, _) = produceAndPersistApplicationAndAssessment(applicant, allocatedTo, offenderDetails)

            val placementRequest1 = produceAndPersistPlacementRequest(application)
            val placementRequest2 = produceAndPersistPlacementRequest(application)

            produceAndPersistPlacementRequest(application) {
              withReallocatedAt(OffsetDateTime.now())
            }

            val placementRequestWithBooking = produceAndPersistPlacementRequest(application) {
              val premises = approvedPremisesEntityFactory.produceAndPersist {
                withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
                withYieldedProbationRegion { probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } } }
              }

              withBooking(
                bookingEntityFactory.produceAndPersist {
                  withPremises(premises)
                },
              )
            }

            produceAndPersistPlacementRequest(application) {
              withIsWithdrawn(true)
            }

            val expected = listOf(
              Withdrawable(
                application.id,
                WithdrawableType.application,
                emptyList(),
              ),
              Withdrawable(
                placementRequest1.id,
                WithdrawableType.placementRequest,
                listOf(datePeriodForDuration(placementRequest1.expectedArrival, placementRequest1.duration)),
              ),
              Withdrawable(
                placementRequest2.id,
                WithdrawableType.placementRequest,
                listOf(datePeriodForDuration(placementRequest2.expectedArrival, placementRequest2.duration)),
              ),
              Withdrawable(
                placementRequestWithBooking.id,
                WithdrawableType.placementRequest,
                listOf(datePeriodForDuration(placementRequestWithBooking.expectedArrival, placementRequestWithBooking.duration)),
              ),
            )

            webTestClient.get()
              .uri("/applications/${application.id}/withdrawables")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .jsonForObject(expected)
          }
        }
      }
    }

    @Test
    fun `Get withdrawables for an application returns withdrawable placement applications`() {
      `Given a User` { applicant, jwt ->
        `Given a User` { allocatedTo, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val (application, _) = produceAndPersistApplicationAndAssessment(applicant, allocatedTo, offenderDetails)

            val submittedApplication1ExpectedArrival1 = LocalDate.now().plusDays(1)
            val submittedApplication1Duration1 = 5
            val submittedApplication1ExpectedArrival2 = LocalDate.now().plusDays(10)
            val submittedApplication1Duration2 = 10

            val submittedPlacementApplication1 = produceAndPersistPlacementApplication(
              application,
              listOf(
                submittedApplication1ExpectedArrival1 to submittedApplication1Duration1,
                submittedApplication1ExpectedArrival2 to submittedApplication1Duration2,
              ),
            )

            val submittedApplication2ExpectedArrival = LocalDate.now().plusDays(50)
            val submittedApplication2Duration = 6
            val submittedPlacementApplication2 = produceAndPersistPlacementApplication(
              application,
              listOf(submittedApplication2ExpectedArrival to submittedApplication2Duration),
            )

            val unsubmittedApplicationExpectedArrival = LocalDate.now().plusDays(50)
            val unsubmittedApplicationDuration = 6
            val unsubmittedPlacementApplication = produceAndPersistPlacementApplication(
              application,
              listOf(unsubmittedApplicationExpectedArrival to unsubmittedApplicationDuration),
            ) {
              withSubmittedAt(null)
            }

            produceAndPersistPlacementApplication(application, listOf(LocalDate.now() to 2)) {
              withReallocatedAt(OffsetDateTime.now())
            }

            val applicationWithAcceptedDecisionExpectedArrival = LocalDate.now().plusDays(50)
            val applicationWithAcceptedDecisionDuration = 6
            val applicationWithAcceptedDecision = produceAndPersistPlacementApplication(
              application,
              listOf(applicationWithAcceptedDecisionExpectedArrival to applicationWithAcceptedDecisionDuration),
            ) {
              withDecision(PlacementApplicationDecision.ACCEPTED)
            }

            produceAndPersistPlacementApplication(application, listOf(LocalDate.now() to 2)) {
              withDecision(PlacementApplicationDecision.WITHDRAW)
            }

            produceAndPersistPlacementApplication(application, listOf(LocalDate.now() to 2)) {
              withDecision(PlacementApplicationDecision.WITHDRAWN_BY_PP)
            }

            val applicationWithRejectedDecisionExpectedArrival = LocalDate.now().plusDays(50)
            val applicationWithRejectedDecisionDuration = 6
            val applicationWithRejectedDecision = produceAndPersistPlacementApplication(
              application,
              listOf(applicationWithRejectedDecisionExpectedArrival to applicationWithRejectedDecisionDuration),
            ) {
              withDecision(PlacementApplicationDecision.REJECTED)
            }

            val expected = listOf(
              Withdrawable(
                application.id,
                WithdrawableType.application,
                emptyList(),
              ),
              Withdrawable(
                submittedPlacementApplication1.id,
                WithdrawableType.placementApplication,
                listOf(
                  datePeriodForDuration(submittedApplication1ExpectedArrival1, submittedApplication1Duration1),
                  datePeriodForDuration(submittedApplication1ExpectedArrival2, submittedApplication1Duration2),
                ),
              ),
              Withdrawable(
                submittedPlacementApplication2.id,
                WithdrawableType.placementApplication,
                listOf(datePeriodForDuration(submittedApplication2ExpectedArrival, submittedApplication2Duration)),
              ),
              Withdrawable(
                unsubmittedPlacementApplication.id,
                WithdrawableType.placementApplication,
                listOf(datePeriodForDuration(unsubmittedApplicationExpectedArrival, unsubmittedApplicationDuration)),
              ),
              Withdrawable(
                applicationWithAcceptedDecision.id,
                WithdrawableType.placementApplication,
                listOf(datePeriodForDuration(applicationWithAcceptedDecisionExpectedArrival, applicationWithAcceptedDecisionDuration)),
              ),
              Withdrawable(
                applicationWithRejectedDecision.id,
                WithdrawableType.placementApplication,
                listOf(datePeriodForDuration(applicationWithRejectedDecisionExpectedArrival, applicationWithRejectedDecisionDuration)),
              ),
            )

            webTestClient.get()
              .uri("/applications/${application.id}/withdrawables")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .jsonForObject(expected)
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_WORKFLOW_MANAGER"])
    fun `Get withdrawables for an application returns withdrawable bookings when a user can manage bookings`(role: UserRole) {
      `Given a User` { applicant, _ ->
        `Given a User`(roles = listOf(role)) { allocatedTo, jwt ->
          `Given an Offender` { offenderDetails, _ ->

            val (application, _) = produceAndPersistApplicationAndAssessment(applicant, allocatedTo, offenderDetails)

            val booking1expectedArrival = LocalDate.now().plusDays(1)
            val booking1expectedDeparture = LocalDate.now().plusDays(6)
            val booking1 = produceAndPersistBooking(
              application,
              booking1expectedArrival,
              booking1expectedDeparture,
            )

            val booking2expectedArrival = LocalDate.now().plusDays(1)
            val booking2expectedDeparture = LocalDate.now().plusDays(6)
            val booking2 = produceAndPersistBooking(
              application,
              booking2expectedArrival,
              booking2expectedDeparture,
            )

            val cancelledBooking = produceAndPersistBooking(
              application,
              LocalDate.now(),
              LocalDate.now().plusDays(1),
            )
            cancellationEntityFactory.produceAndPersist {
              withBooking(cancelledBooking)
              withReason(cancellationReasonEntityFactory.produceAndPersist())
            }

            val bookingWithArrival = produceAndPersistBooking(
              application,
              LocalDate.now(),
              LocalDate.now().plusDays(1),
            )
            arrivalEntityFactory.produceAndPersist() {
              withBooking(bookingWithArrival)
            }

            val expected = listOfNotNull(
              if (role == UserRole.CAS1_WORKFLOW_MANAGER) {
                Withdrawable(
                  application.id,
                  WithdrawableType.application,
                  emptyList(),
                )
              } else {
                null
              },
              Withdrawable(
                booking1.id,
                WithdrawableType.booking,
                listOf(DatePeriod(booking1expectedArrival, booking1expectedDeparture)),
              ),
              Withdrawable(
                booking2.id,
                WithdrawableType.booking,
                listOf(DatePeriod(booking2expectedArrival, booking2expectedDeparture)),
              ),
            )

            webTestClient.get()
              .uri("/applications/${application.id}/withdrawables")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .jsonForObject(expected)
          }
        }
      }
    }

    @Test
    fun `Get withdrawables for all possible types when a user can manage bookings`() {
      `Given a User` { applicant, _ ->
        `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { allocatedTo, jwt ->
          `Given an Offender` { offenderDetails, _ ->

            val (application, _) = produceAndPersistApplicationAndAssessment(applicant, allocatedTo, offenderDetails)

            val booking1ExpectedArrival = LocalDate.now().plusDays(1)
            val booking1ExpectedDeparture = LocalDate.now().plusDays(6)
            val booking1 = produceAndPersistBooking(
              application,
              booking1ExpectedArrival,
              booking1ExpectedDeparture,
            )

            val placementApplicationExpectedArrival = LocalDate.now().plusDays(1)
            val placementApplicationDuration = 5

            val placementApplication = produceAndPersistPlacementApplication(
              application,
              listOf(placementApplicationExpectedArrival to placementApplicationDuration),
            )

            val placementRequest = produceAndPersistPlacementRequest(application)

            val expected = listOf(
              Withdrawable(
                application.id,
                WithdrawableType.application,
                emptyList(),
              ),
              Withdrawable(
                booking1.id,
                WithdrawableType.booking,
                listOf(DatePeriod(booking1ExpectedArrival, booking1ExpectedDeparture)),
              ),
              Withdrawable(
                placementApplication.id,
                WithdrawableType.placementApplication,
                listOf(datePeriodForDuration(placementApplicationExpectedArrival, placementApplicationDuration)),
              ),
              Withdrawable(
                placementRequest.id,
                WithdrawableType.placementRequest,
                listOf(datePeriodForDuration(placementRequest.expectedArrival, placementRequest.duration)),
              ),
            )

            webTestClient.get()
              .uri("/applications/${application.id}/withdrawables")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .jsonForObject(expected)
          }
        }
      }
    }

    @Test
    fun `Get withdrawables for all possible types filters out bookings when a user cannot manage bookings`() {
      `Given a User` { applicant, jwt ->
        `Given an Offender` { offenderDetails, _ ->

          val (application, _) = produceAndPersistApplicationAndAssessment(applicant, applicant, offenderDetails)

          val booking1ExpectedArrival = LocalDate.now().plusDays(1)
          val booking1ExpectedDeparture = LocalDate.now().plusDays(6)
          produceAndPersistBooking(
            application,
            booking1ExpectedArrival,
            booking1ExpectedDeparture,
          )

          val placementApplicationExpectedArrival = LocalDate.now().plusDays(1)
          val placementApplicationDuration = 5

          val placementApplication = produceAndPersistPlacementApplication(
            application,
            listOf(placementApplicationExpectedArrival to placementApplicationDuration),
          )

          val placementRequest = produceAndPersistPlacementRequest(application)

          val expected = listOf(
            Withdrawable(
              application.id,
              WithdrawableType.application,
              emptyList(),
            ),
            Withdrawable(
              placementApplication.id,
              WithdrawableType.placementApplication,
              listOf(datePeriodForDuration(placementApplicationExpectedArrival, placementApplicationDuration)),
            ),
            Withdrawable(
              placementRequest.id,
              WithdrawableType.placementRequest,
              listOf(datePeriodForDuration(placementRequest.expectedArrival, placementRequest.duration)),
            ),
          )

          webTestClient.get()
            .uri("/applications/${application.id}/withdrawables")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonForObject(expected)
        }
      }
    }
  }

  @Nested
  inner class WithdrawalCascading {

    @Test
    fun `Withdrawing an application withdraws all related entities`() {
      `Given a User` { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val (application, assessment) = produceAndPersistApplicationAndAssessment(user, user, offenderDetails)

          val placementApplicationExpectedArrival = LocalDate.now().plusDays(1)
          val placementApplicationDuration = 5

          val placementApplication = produceAndPersistPlacementApplication(
            application,
            listOf(placementApplicationExpectedArrival to placementApplicationDuration),
          )

          val placementRequest1 = produceAndPersistPlacementRequest(application)

          val booking1NoArrival = produceAndPersistBooking(
            application,
            startDate = LocalDate.now().plusDays(1),
            endDate = LocalDate.now().plusDays(6),
          )

          placementRequest1.booking = booking1NoArrival
          placementRequestRepository.save(placementRequest1)

          val placementRequest2 = produceAndPersistPlacementRequest(application)
          val booking2HasArrival = produceAndPersistBooking(
            application,
            LocalDate.now(),
            LocalDate.now().plusDays(1),
          )
          arrivalEntityFactory.produceAndPersist {
            withBooking(booking2HasArrival)
          }

          placementRequest2.booking = booking2HasArrival
          placementRequestRepository.save(placementRequest2)

          webTestClient.post()
            .uri("/applications/${application.id}/withdrawal")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewWithdrawal(
                reason = WithdrawalReason.duplicateApplication,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(application.id)!!
          Assertions.assertThat(updatedApplication.isWithdrawn).isTrue

          val updatedAssessment = approvedPremisesAssessmentRepository.findByIdOrNull(assessment.id)!!
          Assertions.assertThat(updatedAssessment.isWithdrawn).isTrue

          val updatedPlacementApplication = placementApplicationRepository.findByIdOrNull(placementApplication.id)!!
          Assertions.assertThat(updatedPlacementApplication.decision)
            .isEqualTo(PlacementApplicationDecision.WITHDRAWN_BY_PP)

          val updatedPlacementRequest1 = placementRequestRepository.findByIdOrNull(placementRequest1.id)!!
          Assertions.assertThat(updatedPlacementRequest1.isWithdrawn).isEqualTo(true)
          Assertions.assertThat(updatedPlacementRequest1.withdrawalReason)
            .isEqualTo(PlacementRequestWithdrawalReason.WITHDRAWN_BY_PP)

          val updatedPlacementRequest2 = placementRequestRepository.findByIdOrNull(placementRequest2.id)!!
          Assertions.assertThat(updatedPlacementRequest2.isWithdrawn).isEqualTo(true)
          Assertions.assertThat(updatedPlacementRequest2.withdrawalReason)
            .isEqualTo(PlacementRequestWithdrawalReason.WITHDRAWN_BY_PP)

          val updatedBooking1 = bookingRepository.findByIdOrNull(booking1NoArrival.id)!!
          Assertions.assertThat(updatedBooking1.isCancelled).isTrue()
          Assertions.assertThat(updatedBooking1.cancellation!!.reason.name)
            .isEqualTo("The probation practitioner requested it")

          val updatedBooking2WithArrival = bookingRepository.findByIdOrNull(booking2HasArrival.id)!!
          Assertions.assertThat(updatedBooking2WithArrival.isCancelled).isFalse()
        }
      }
    }
  }

  private fun produceAndPersistApplicationAndAssessment(
    applicant: UserEntity,
    assignee: UserEntity,
    offenderDetails: OffenderDetailSummary,
  ): Pair<ApprovedPremisesApplicationEntity, AssessmentEntity> {
    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
      withAddedAt(OffsetDateTime.now())
    }

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(applicant)
      withApplicationSchema(applicationSchema)
      withSubmittedAt(OffsetDateTime.now())
    }

    val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withAllocatedToUser(assignee)
      withApplication(application)
      withAssessmentSchema(assessmentSchema)
    }

    assessment.schemaUpToDate = true
    application.assessments.add(assessment)

    return Pair(application, assessment)
  }

  private fun produceAndPersistBasicApplication(
    crn: String,
    userEntity: UserEntity,
    managingTeamCode: String,
  ): ApplicationEntity {
    val jsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
      withSchema(
        """
        {
          "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
          "${"\$id"}": "https://example.com/product.schema.json",
          "title": "Thing",
          "description": "A thing",
          "type": "object",
          "properties": {
            "thingId": {
              "description": "The unique identifier for a thing",
              "type": "integer"
            }
          },
          "required": [ "thingId" ]
        }
        """,
      )
    }

    val application =
      approvedPremisesApplicationEntityFactory.produceAndPersist {
        withApplicationSchema(jsonSchema)
        withCrn(crn)
        withCreatedByUser(userEntity)
        withData(
          """
          {
             "thingId": 123
          }
          """,
        )
      }

    application.teamCodes += applicationTeamCodeRepository.save(
      ApplicationTeamCodeEntity(
        id = UUID.randomUUID(),
        application = application,
        teamCode = managingTeamCode,
      ),
    )

    return application
  }

  private fun produceAndPersistPlacementApplication(
    application: ApprovedPremisesApplicationEntity,
    arrivalAndDurations: List<Pair<LocalDate, Int>>,
    configuration: (PlacementApplicationEntityFactory.() -> Unit)? = null,
  ): PlacementApplicationEntity {
    val placementApplication = placementApplicationFactory.produceAndPersist {
      withCreatedByUser(application.createdByUser)
      withAllocatedToUser(application.createdByUser)
      withApplication(application)
      withSchemaVersion(
        approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        },
      )
      withSubmittedAt(OffsetDateTime.now())
      withDecision(null)
      withPlacementType(PlacementType.ADDITIONAL_PLACEMENT)
      configuration?.invoke(this)
    }

    arrivalAndDurations.forEach { (start, duration) ->
      placementDateFactory.produceAndPersist {
        withPlacementApplication(placementApplication)
        withExpectedArrival(start)
        withDuration(duration)
      }
    }

    return placementApplication
  }

  private fun produceAndPersistPlacementRequest(
    application: ApprovedPremisesApplicationEntity,
    configuration: (PlacementRequestEntityFactory.() -> Unit)? = null,
  ) =
    placementRequestFactory.produceAndPersist {
      val assessment = application.assessments.get(0)

      val placementRequirements = placementRequirementsFactory.produceAndPersist {
        withApplication(application)
        withAssessment(assessment)
        withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
        withDesirableCriteria(
          characteristicEntityFactory.produceAndPersistMultiple(5),
        )
        withEssentialCriteria(
          characteristicEntityFactory.produceAndPersistMultiple(3),
        )
      }

      withAllocatedToUser(application.createdByUser)
      withApplication(application)
      withAssessment(assessment)
      withPlacementRequirements(placementRequirements)
      configuration?.invoke(this)
    }

  private fun produceAndPersistBooking(
    application: ApprovedPremisesApplicationEntity,
    startDate: LocalDate,
    endDate: LocalDate,
    configuration: (BookingEntityFactory.() -> Unit)? = null,
  ): BookingEntity {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } } }
    }

    return bookingEntityFactory.produceAndPersist {
      withApplication(application)
      withPremises(premises)
      withCrn(application.crn)
      withServiceName(ServiceName.approvedPremises)
      withArrivalDate(startDate)
      withDepartureDate(endDate)
      configuration?.invoke(this)
    }
  }

  private fun datePeriodForDuration(start: LocalDate, duration: Int) = DatePeriod(start, start.plusDays(duration.toLong()))
}
