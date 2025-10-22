package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplicationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3SubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision as AssessmentDecisionApi

class Cas3ApplicationTest : InitialiseDatabasePerClassTestBase() {

  @Nested
  inner class GetApplications {
    @Test
    fun `Get all applications without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas3/applications")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @SuppressWarnings("CyclomaticComplexMethod")
    @ParameterizedTest
    @CsvSource(
      "CAS3_REFERRER",
      "CAS3_ASSESSOR",
    )
    fun `Get all applications returns 200 and returns all applications for user`(userRole: UserRole) {
      givenAProbationRegion { probationRegion ->
        givenAUser(roles = listOf(userRole), probationRegion = probationRegion) { otherUser, _ ->
          givenAUser(
            roles = listOf(UserRole.CAS3_REFERRER),
            probationRegion = probationRegion,
          ) { referrerUser, jwt ->
            givenAnOffender { offenderDetails, _ ->

              val applicationInProgress =
                createApplicationEntity(referrerUser, offenderDetails, probationRegion, null)

              val applicationSubmitted =
                createApplicationEntity(
                  referrerUser,
                  offenderDetails,
                  probationRegion,
                  OffsetDateTime.now().randomDateTimeBefore(5),
                )

              val applicationRejected =
                createApplicationEntity(
                  referrerUser,
                  offenderDetails,
                  probationRegion,
                  OffsetDateTime.now().randomDateTimeBefore(15),
                )

              temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
                withApplication(applicationRejected)
                withSubmittedAt(OffsetDateTime.now().minusDays(10))
                withDecision(AssessmentDecision.REJECTED)
              }

              val anotherUsersApplication =
                createApplicationEntity(otherUser, offenderDetails, probationRegion, null)

              apDeliusContextAddResponseToUserAccessCall(
                listOf(
                  CaseAccessFactory()
                    .withCrn(offenderDetails.otherIds.crn)
                    .produce(),
                ),
                referrerUser.deliusUsername,
              )

              val responseBody = webTestClient.get()
                .uri("/cas3/applications")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .bodyAsListOfObjects<Cas3ApplicationSummary>()

              assertApplicationSummaryResponse(
                applicationInProgress,
                responseBody.firstOrNull { it.id == applicationInProgress.id },
                ApplicationStatus.inProgress,
              )
              assertApplicationSummaryResponse(
                applicationSubmitted,
                responseBody.firstOrNull { it.id == applicationSubmitted.id },
                ApplicationStatus.submitted,
              )
              assertApplicationSummaryResponse(
                applicationRejected,
                responseBody.firstOrNull { it.id == applicationRejected.id },
                ApplicationStatus.rejected,
              )

              assertThat(responseBody).noneMatch {
                anotherUsersApplication.id == it.id
              }
            }
          }
        }
      }
    }

    private fun createApplicationEntity(
      user: UserEntity,
      offenderDetails: OffenderDetailSummary,
      probationRegion: ProbationRegionEntity,
      submittedAt: OffsetDateTime?,
    ): TemporaryAccommodationApplicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(user)
      withSubmittedAt(submittedAt)
      withCrn(offenderDetails.otherIds.crn)
      withData("{}")
      withProbationRegion(probationRegion)
    }

    private fun assertApplicationSummaryResponse(
      application: TemporaryAccommodationApplicationEntity,
      applicationSummary: Cas3ApplicationSummary?,
      status: ApplicationStatus,
    ) {
      assertThat(applicationSummary).isNotNull()
      assertThat(applicationSummary?.id).isEqualTo(application.id)
      assertThat(applicationSummary?.person?.crn).isEqualTo(application.crn)
      assertThat(applicationSummary?.createdAt).isEqualTo(application.createdAt.toInstant())
      assertThat(applicationSummary?.createdByUserId).isEqualTo(application.createdByUser.id)
      assertThat(applicationSummary?.submittedAt).isEqualTo(application.submittedAt?.toInstant())
      assertThat(applicationSummary?.status).isEqualTo(status)
    }
  }

  @Nested
  inner class GetApplication {
    @Test
    fun `Get single application returns 200 with correct body when requesting user created application`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->

          val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
            withProbationRegion(userEntity.probationRegion)
            withData(
              """
            {
               "thingId": 123
            }
            """,
            )
          }

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            userEntity.deliusUsername,
          )

          callCas3ApiAndAssertApiResponse(jwt, applicationEntity)
        }
      }
    }

    @Test
    fun `Get single application returns 200 with correct body when a user with the CAS3_ASSESSOR role requests a submitted application in their region`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAUser(probationRegion = userEntity.probationRegion) { createdByUser, _ ->
          givenAnOffender { offenderDetails, _ ->

            val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(createdByUser)
              withProbationRegion(createdByUser.probationRegion)
              withSubmittedAt(OffsetDateTime.parse("2023-06-01T12:34:56.789+01:00"))
              withData(
                """
              {
                 "thingId": 123
              }
              """,
              )
            }

            apDeliusContextAddResponseToUserAccessCall(
              listOf(
                CaseAccessFactory()
                  .withCrn(offenderDetails.otherIds.crn)
                  .produce(),
              ),
              userEntity.deliusUsername,
            )

            callCas3ApiAndAssertApiResponse(jwt, applicationEntity)
          }
        }
      }
    }

    @Test
    fun `Get single LAO application for application creator with LAO access returns 200`() {
      givenAUser { createdByUser, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withCurrentRestriction(true)
          },
        ) { offenderDetails, _ ->
          val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(createdByUser)
            withProbationRegion(createdByUser.probationRegion)
            withSubmittedAt(OffsetDateTime.parse("2023-06-01T12:34:56.789+01:00"))
            withData(
              """
            {
               "thingId": 123
            }
            """,
            )
          }

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            createdByUser.deliusUsername,
          )

          val cas3ApiResponseBody = callCas3Api(jwt, applicationEntity.id)
            .expectStatus()
            .isOk
            .expectBody(Cas3Application::class.java)
            .returnResult()
            .responseBody

          assertThat(cas3ApiResponseBody?.person).isInstanceOf(FullPerson::class.java)
        }
      }
    }

    @Test
    fun `Get single LAO application for user who is not creator but has LAO Qualification returns RestrictedPerson`() {
      givenAUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        qualifications = listOf(UserQualification.LAO),
      ) { otherUser, otherUserJwt ->
        givenAUser(probationRegion = otherUser.probationRegion) { createdByUser, _ ->
          givenAnOffender(
            offenderDetailsConfigBlock = {
              withCurrentRestriction(true)
            },
          ) { offenderDetails, _ ->

            val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(createdByUser)
              withProbationRegion(createdByUser.probationRegion)
              withSubmittedAt(OffsetDateTime.parse("2023-06-01T12:34:56.789+01:00"))
              withData(
                """
              {
                 "thingId": 123
              }
              """,
              )
            }

            val cas3ApiResult = callCas3Api(otherUserJwt, applicationEntity.id)
              .expectStatus()
              .isOk
              .expectBody(Cas3Application::class.java)
              .returnResult()
              .responseBody

            assertThat(cas3ApiResult!!.person.type).isEqualTo(PersonType.restrictedPerson)
          }
        }
      }
    }

    @Test
    fun `Get single application returns 403 Forbidden when a user with the CAS3_ASSESSOR role requests an application not in their region`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAUser { createdByUser, _ ->
          givenAnOffender { offenderDetails, _ ->

            val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(createdByUser)
              withProbationRegion(createdByUser.probationRegion)
              withSubmittedAt(OffsetDateTime.now())
              withData(
                """
              {
                 "thingId": 123
              }
              """,
              )
            }

            apDeliusContextAddResponseToUserAccessCall(
              listOf(
                CaseAccessFactory()
                  .withCrn(offenderDetails.otherIds.crn)
                  .produce(),
              ),
              userEntity.deliusUsername,
            )

            callCas3Api(jwt, applicationEntity.id)
              .expectStatus()
              .isForbidden
          }
        }
      }
    }

    @Test
    fun `Get single application returns 403 Forbidden when a user without the CAS3_ASSESSOR role requests an application not created by them`() {
      givenAUser { userEntity, jwt ->
        givenAUser(probationRegion = userEntity.probationRegion) { createdByUser, _ ->
          givenAnOffender { offenderDetails, _ ->

            val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(createdByUser)
              withProbationRegion(createdByUser.probationRegion)
              withSubmittedAt(OffsetDateTime.now())
              withData(
                """
              {
                 "thingId": 123
              }
              """,
              )
            }

            apDeliusContextAddResponseToUserAccessCall(
              listOf(
                CaseAccessFactory()
                  .withCrn(offenderDetails.otherIds.crn)
                  .produce(),
              ),
              userEntity.deliusUsername,
            )

            callCas3Api(jwt, applicationEntity.id)
              .expectStatus()
              .isForbidden
          }
        }
      }
    }

    @Test
    fun `Get single application returns 404 Not Found when the application was deleted`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->

          val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
            withProbationRegion(userEntity.probationRegion)
            withDeletedAt(OffsetDateTime.now().minusDays(15))
            withData(
              """
            {
               "thingId": 123
            }
            """,
            )
          }

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            userEntity.deliusUsername,
          )

          callCas3Api(jwt, applicationEntity.id)
            .expectStatus()
            .isNotFound
        }
      }
    }

    @Test
    fun `GET submitted CAS3 application includes assessmentId in the response`() {
      givenAUser { submittingUser, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")
          val offenderName = "${offenderDetails.firstName} ${offenderDetails.surname}"

          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(submittingUser.probationRegion)
          }

          temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withId(applicationId)
            withCreatedByUser(submittingUser)
            withProbationRegion(submittingUser.probationRegion)
            withName(offenderName)
            withData("{}")
          }

          webTestClient.post()
            .uri("/cas3/applications/$applicationId/submission")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .bodyValue(
              SubmitTemporaryAccommodationApplication(
                translatedDocument = {},
                type = "CAS3",
                arrivalDate = LocalDate.now(),
                summaryData = object {
                  val num = 50
                  val text = "Hello world!"
                },
                probationDeliveryUnitId = probationDeliveryUnit.id,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          val cas3ApiResult = callCas3Api(jwt, applicationId)
            .expectStatus()
            .isOk
            .expectBody(Cas3Application::class.java)
            .returnResult()
            .responseBody

          assertThat(cas3ApiResult!!.assessmentId).isNotNull()
          assertThat(cas3ApiResult.assessmentDecision).isNull()
        }
      }
    }

    @Test
    fun `GET submitted CAS3 application return the last assessment decision in the response`() {
      givenAUser { submittingUser, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")
          val offenderName = "${offenderDetails.firstName} ${offenderDetails.surname}"

          val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withId(applicationId)
            withCreatedByUser(submittingUser)
            withProbationRegion(submittingUser.probationRegion)
            withName(offenderName)
            withData("{}")
          }

          temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withDecision(AssessmentDecision.ACCEPTED)
            withCreatedAt(OffsetDateTime.now().minusDays(30))
          }

          // last assessment
          temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withDecision(AssessmentDecision.REJECTED)
            withCreatedAt(OffsetDateTime.now().minusDays(20))
          }

          val cas3ApiResult = callCas3Api(jwt, applicationId)
            .expectStatus()
            .isOk
            .expectBody(Cas3Application::class.java)
            .returnResult()
            .responseBody

          assertThat(cas3ApiResult!!.assessmentId).isNotNull()
          assertThat(cas3ApiResult.assessmentDecision).isEqualTo(AssessmentDecisionApi.rejected)
        }
      }
    }

    private fun callCas3Api(jwt: String, applicationId: UUID) = webTestClient.get()
      .uri("/cas3/applications/$applicationId")
      .header("Authorization", "Bearer $jwt")
      .exchange()

    private fun callCas3ApiAndAssertApiResponse(
      jwt: String,
      applicationEntity: TemporaryAccommodationApplicationEntity,
    ) {
      val responseBody = callCas3Api(jwt, applicationEntity.id)
        .expectStatus()
        .isOk
        .expectBody(Cas3Application::class.java)
        .returnResult()
        .responseBody

      assertThat(responseBody).matches {
        applicationEntity.id == it.id &&
          applicationEntity.crn == it.person.crn &&
          applicationEntity.createdAt.toInstant() == it.createdAt &&
          applicationEntity.createdByUser.id == it.createdByUserId &&
          applicationEntity.submittedAt?.toInstant() == it.submittedAt &&
          serializableToJsonNode(applicationEntity.data) == serializableToJsonNode(it.data)
      }
    }
  }

  @Nested
  inner class CreateApplication {

    @Test
    fun `Create new application returns 403 when user isn't  CAS3_REFERRER role`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        givenAnOffender { offenderDetails, _ ->

          callCasApi(jwt, offenderDetails.otherIds.crn, "789")
            .expectStatus()
            .isForbidden

          callCas3Api(jwt, offenderDetails.otherIds.crn, "789")
            .expectStatus()
            .isForbidden
        }
      }
    }

    @Test
    fun `Should get 403 forbidden error when create new application with user CAS3_REPORTER`() {
      givenAUser(roles = listOf(UserRole.CAS3_REPORTER)) { _, jwt ->
        givenAnOffender { offenderDetails, _ ->

          callCasApi(jwt, offenderDetails.otherIds.crn, "789")
            .expectStatus()
            .isForbidden

          callCas3Api(jwt, offenderDetails.otherIds.crn, "789")
            .expectStatus()
            .isForbidden
        }
      }
    }

    @Test
    fun `Create new application returns 201 with correct body and Location header`() {
      givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { _, jwt ->
        givenAnOffender { offenderDetails, _ ->

          callCasApiAndAssertResponse(jwt, offenderDetails.otherIds.crn)
          callCas3ApiAndAssertResponse(jwt, offenderDetails.otherIds.crn)
        }
      }
    }

    @Test
    fun `Create new application returns successfully when a person has no NOMS number`() {
      givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { _, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = { withoutNomsNumber() },
        ) { offenderDetails, _ ->

          callCasApiAndAssertResponse(jwt, offenderDetails.otherIds.crn)
          callCas3ApiAndAssertResponse(jwt, offenderDetails.otherIds.crn)
        }
      }
    }

    @Test
    fun `Create new application returns 201 with correct body and store prison-name in DB`() {
      givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { _, jwt ->
        val agencyName = "HMP Bristol"
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withCrn("CRN")
            withDateOfBirth(LocalDate.parse("1985-05-05"))
            withNomsNumber("NOMS321")
            withFirstName("James")
            withLastName("Someone")
            withGender("Male")
            withEthnicity("White British")
            withNationality("English")
            withReligionOrBelief("Judaism")
            withGenderIdentity("Prefer to self-describe")
            withSelfDescribedGenderIdentity("This is a self described identity")
          },
          inmateDetailsConfigBlock = {
            withOffenderNo("NOMS321")
            withCustodyStatus(InmateStatus.IN)
            withAssignedLivingUnit(
              AssignedLivingUnit(
                agencyId = "BRI",
                locationId = 5,
                description = "B-2F-004",
                agencyName = agencyName,
              ),
            )
          },
        ) { offenderDetails, _ ->
          callCasApiAndAssertResponse(jwt, offenderDetails.otherIds.crn, agencyName)
          callCas3ApiAndAssertResponse(jwt, offenderDetails.otherIds.crn, agencyName)
        }
      }
    }

    private fun callCasApiAndAssertResponse(
      jwt: String,
      crn: String,
      agencyName: String? = null,
    ) {
      val offenceId = "789"

      val result = callCasApi(jwt, crn, offenceId)
        .expectStatus()
        .isCreated
        .returnResult(TemporaryAccommodationApplication::class.java)

      assertThat(result.responseHeaders["Location"]).anyMatch {
        it.matches(Regex("/applications/.+"))
      }

      val blockFirst = result.responseBody.blockFirst()
      assertThat(blockFirst).matches {
        it.person.crn == crn &&
          it.offenceId == offenceId
      }

      if (agencyName != null) {
        val accommodationApplicationEntity =
          temporaryAccommodationApplicationRepository.findByIdOrNull(blockFirst.id)
        assertThat(accommodationApplicationEntity!!.prisonNameOnCreation).isNotNull()
        assertThat(accommodationApplicationEntity!!.prisonNameOnCreation).isEqualTo(agencyName)
      }
    }

    private fun callCasApi(jwt: String, crn: String, offenceId: String) = webTestClient.post()
      .uri("/applications")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .bodyValue(
        NewApplication(
          crn = crn,
          convictionId = 123,
          deliusEventNumber = "1",
          offenceId = offenceId,
        ),
      )
      .exchange()

    private fun callCas3ApiAndAssertResponse(
      jwt: String,
      crn: String,
      agencyName: String? = null,
    ) {
      val offenceId = "789"

      val result = callCas3Api(jwt, crn, offenceId)
        .expectStatus()
        .isCreated
        .returnResult(Cas3Application::class.java)

      assertThat(result.responseHeaders["Location"]).anyMatch {
        it.matches(Regex("/applications/.+"))
      }

      val blockFirst = result.responseBody.blockFirst()
      assertThat(blockFirst).matches {
        it.person.crn == crn &&
          it.offenceId == offenceId
      }

      if (agencyName != null) {
        val accommodationApplicationEntity =
          temporaryAccommodationApplicationRepository.findByIdOrNull(blockFirst.id)
        assertThat(accommodationApplicationEntity!!.prisonNameOnCreation).isNotNull()
        assertThat(accommodationApplicationEntity!!.prisonNameOnCreation).isEqualTo(agencyName)
      }
    }

    private fun callCas3Api(jwt: String, crn: String, offenceId: String) = webTestClient.post()
      .uri("/cas3/applications")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        Cas3NewApplication(
          crn = crn,
          convictionId = 123,
          deliusEventNumber = "1",
          offenceId = offenceId,
        ),
      )
      .exchange()
  }

  @Nested
  inner class UpdateApplication {
    @Test
    fun `Update existing application returns 200 with correct body`() {
      givenAUser { userEntity, jwt ->
        givenAUser(
          roles = listOf(UserRole.CAS3_REFERRER),
        ) { _, _ ->
          givenAnOffender { offenderDetails, _ ->

            val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(userEntity)
              withProbationRegion(userEntity.probationRegion)
              withData(
                """
            {
               "thingId": 123
            }
            """,
              )
            }

            callCasApiAndAssertResponse(jwt, applicationEntity.id, offenderDetails.otherIds.crn)
            callCas3ApiAndAssertResponse(jwt, applicationEntity.id, offenderDetails.otherIds.crn)
          }
        }
      }
    }

    @Test
    fun `Update existing application which was deleted returns 400`() {
      givenAUser { userEntity, jwt ->
        givenAUser(
          roles = listOf(UserRole.CAS3_REFERRER),
        ) { _, _ ->
          givenAnOffender { offenderDetails, _ ->

            val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(userEntity)
              withProbationRegion(userEntity.probationRegion)
              withDeletedAt(OffsetDateTime.now().minusDays(26))
              withData(
                """
            {
               "thingId": 123
            }
            """,
              )
            }

            callCasApi(jwt, applicationEntity.id)
              .expectStatus()
              .isBadRequest
              .expectBody()
              .jsonPath("$.status").isEqualTo("400")
              .jsonPath("$.detail").isEqualTo("This application has already been deleted")

            callCas3Api(jwt, applicationEntity.id)
              .expectStatus()
              .isBadRequest
              .expectBody()
              .jsonPath("$.status").isEqualTo("400")
              .jsonPath("$.detail").isEqualTo("This application has already been deleted")
          }
        }
      }
    }

    private fun callCasApiAndAssertResponse(
      jwt: String,
      applicationId: UUID,
      crn: String,
    ) {
      val casApiResult = callCasApi(jwt, applicationId)
        .expectStatus()
        .isOk
        .returnResult(String::class.java)
        .responseBody
        .blockFirst()

      val result = objectMapper.readValue(casApiResult, TemporaryAccommodationApplication::class.java)

      assertThat(result.person.crn).isEqualTo(crn)
      assertThat(result.data.toString()).isEqualTo("""{thingId=345}""")
    }

    private fun callCasApi(jwt: String, applicationId: UUID) = webTestClient.put()
      .uri("/applications/$applicationId")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        UpdateTemporaryAccommodationApplication(
          data = mapOf("thingId" to 345),
          type = UpdateApplicationType.CAS3,
        ),
      )
      .exchange()

    private fun callCas3ApiAndAssertResponse(
      jwt: String,
      applicationId: UUID,
      crn: String,
    ) {
      val casApiResult = callCas3Api(jwt, applicationId)
        .expectStatus()
        .isOk
        .returnResult(String::class.java)
        .responseBody
        .blockFirst()

      val result = objectMapper.readValue(casApiResult, Cas3Application::class.java)

      assertThat(result.person.crn).isEqualTo(crn)
      assertThat(result.data.toString()).isEqualTo("""{thingId=345}""")
    }

    private fun callCas3Api(jwt: String, applicationId: UUID) = webTestClient.put()
      .uri("/cas3/applications/$applicationId")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        Cas3UpdateApplication(
          data = mapOf("thingId" to 345),
        ),
      )
      .exchange()
  }

  @Nested
  inner class SubmitApplication {

    @Test
    fun `Submit an application returns 200`() {
      givenAUser { submittingUser, jwt ->
        givenAUser { _, _ ->
          givenAnOffender { offenderDetails, _ ->
            val applicationId = UUID.randomUUID()
            val offenderName = "${offenderDetails.firstName} ${offenderDetails.surname}"

            val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(submittingUser.probationRegion)
            }

            temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withId(applicationId)
              withCreatedByUser(submittingUser)
              withProbationRegion(submittingUser.probationRegion)
              withName(offenderName)
              withData(
                """
                {}
              """,
              )
            }

            val cas3SubmitApplication = Cas3SubmitApplication(
              translatedDocument = {},
              arrivalDate = LocalDate.now(),
              summaryData = object {
                val num = 50
                val text = "Hello world!"
              },
              probationDeliveryUnitId = probationDeliveryUnit.id,
            )

            callApiAndAssertApiResponse(jwt, applicationId, cas3SubmitApplication)
              .expectStatus()
              .isOk

            val persistedApplication = temporaryAccommodationApplicationRepository.findByIdOrNull(applicationId)!!
            val persistedAssessment = persistedApplication.getLatestAssessment() as TemporaryAccommodationAssessmentEntity

            assertThat(persistedAssessment.summaryData).isEqualTo("{\"num\":50,\"text\":\"Hello world!\"}")
            assertThat(persistedApplication.name).isEqualTo(offenderName)
          }
        }
      }
    }

    @Test
    fun `Submit an application returns 200 with optional elements in the request`() {
      givenAUser { submittingUser, jwt ->
        givenAUser { user, _ ->
          givenAnOffender { offenderDetails, _ ->
            val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

            val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(user.probationRegion)
            }

            temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withId(applicationId)
              withCreatedByUser(submittingUser)
              withProbationRegion(submittingUser.probationRegion)
              withProbationDeliveryUnit(probationDeliveryUnit)
              withHasHistoryOfSexualOffence(true)
              withIsConcerningSexualBehaviour(true)
              withIsConcerningArsonBehaviour(true)
              withData(
                """
              {}
            """,
              )
            }

            val pdu = probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(
                probationRegionEntityFactory.produceAndPersist {
                  withId(submittingUser.probationRegion.id)
                },
              )
            }

            val cas3SubmitApplication = Cas3SubmitApplication(
              translatedDocument = {},
              arrivalDate = LocalDate.now(),
              summaryData = object {
                val num = 50
                val text = "Hello world!"
              },
              personReleaseDate = LocalDate.now(),
              isHistoryOfSexualOffence = true,
              isConcerningSexualBehaviour = true,
              isConcerningArsonBehaviour = true,
              dutyToReferOutcome = "Accepted – Prevention/ Relief Duty",
              prisonReleaseTypes = listOf(
                "Parole",
                "CRD licence",
              ),
              probationDeliveryUnitId = pdu.id,
            )

            callApiAndAssertApiResponse(jwt, applicationId, cas3SubmitApplication)
              .expectStatus()
              .isOk

            val persistedApplication = temporaryAccommodationApplicationRepository.findByIdOrNull(applicationId)!!
            val persistedAssessment =
              persistedApplication.getLatestAssessment() as TemporaryAccommodationAssessmentEntity

            assertThat(persistedAssessment.summaryData).isEqualTo("{\"num\":50,\"text\":\"Hello world!\"}")
            assertThat(persistedApplication.personReleaseDate).isEqualTo(LocalDate.now())
            assertThat(persistedApplication.dutyToReferOutcome).isEqualTo("Accepted – Prevention/ Relief Duty")
            assertThat(persistedApplication.prisonReleaseTypes).isEqualTo("Parole,CRD licence")
            assertThat(persistedApplication.probationDeliveryUnit!!.id).isEqualTo(pdu.id)
            assertThat(persistedApplication.probationDeliveryUnit!!.name).isEqualTo(pdu.name)
          }
        }
      }
    }

    @Test
    fun `Submit an application returns 400 when the application was deleted`() {
      givenAUser { submittingUser, jwt ->
        givenAUser { _, _ ->
          givenAnOffender { offenderDetails, _ ->
            val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")
            val offenderName = "${offenderDetails.firstName} ${offenderDetails.surname}"

            val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(submittingUser.probationRegion)
            }

            temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withId(applicationId)
              withCreatedByUser(submittingUser)
              withProbationRegion(submittingUser.probationRegion)
              withName(offenderName)
              withDeletedAt(OffsetDateTime.now().minusDays(32))
              withData(
                """
                {}
              """,
              )
            }

            val cas3SubmitApplication = Cas3SubmitApplication(
              translatedDocument = {},
              arrivalDate = LocalDate.now(),
              summaryData = object {
                val num = 50
                val text = "Hello world!"
              },
              probationDeliveryUnitId = probationDeliveryUnit.id,
            )

            callApiAndAssertApiResponse(jwt, applicationId, cas3SubmitApplication)
              .expectStatus()
              .isBadRequest
              .expectBody()
              .jsonPath("$.status").isEqualTo("400")
              .jsonPath("$.detail").isEqualTo("This application has already been deleted")
          }
        }
      }
    }

    @Test
    fun `Submit an application with out of region fields returns 200 and persists out of region data`() {
      givenAUser { submittingUser, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val applicationId = UUID.randomUUID()
          val offenderName = "${offenderDetails.firstName} ${offenderDetails.surname}"

          val mainProbationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(submittingUser.probationRegion)
            withName("Main PDU")
          }

          val outOfRegionProbationRegion = givenAProbationRegion(name = "Out Of Region Probation Region")

          val outOfRegionProbationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(outOfRegionProbationRegion)
            withName("Out Of Region PDU")
          }

          temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withId(applicationId)
            withCreatedByUser(submittingUser)
            withProbationRegion(submittingUser.probationRegion)
            withName(offenderName)
            withData(
              """
                {}
              """,
            )
          }

          val cas3SubmitApplication = Cas3SubmitApplication(
            translatedDocument = {},
            arrivalDate = LocalDate.now(),
            summaryData = object {
              val num = 50
              val text = "Out of region test!"
            },
            probationDeliveryUnitId = mainProbationDeliveryUnit.id,
            outOfRegionProbationRegionId = outOfRegionProbationRegion.id,
            outOfRegionPduId = outOfRegionProbationDeliveryUnit.id,
          )

          callApiAndAssertApiResponse(jwt, applicationId, cas3SubmitApplication)
            .expectStatus()
            .isOk

          val persistedApplication = temporaryAccommodationApplicationRepository.findByIdOrNull(applicationId)!!
          val persistedAssessment = persistedApplication.getLatestAssessment() as TemporaryAccommodationAssessmentEntity

          assertThat(persistedAssessment.summaryData).isEqualTo("{\"num\":50,\"text\":\"Out of region test!\"}")
          assertThat(persistedApplication.name).isEqualTo(offenderName)

          assertThat(persistedApplication.probationRegion.id).isEqualTo(outOfRegionProbationRegion.id)
          assertThat(persistedApplication.probationDeliveryUnit!!.id).isEqualTo(outOfRegionProbationDeliveryUnit.id)

          assertThat(persistedApplication.previousReferralProbationRegion).isNotNull
          assertThat(persistedApplication.previousReferralProbationRegion!!.id).isEqualTo(submittingUser.probationRegion.id)

          assertThat(persistedApplication.previousReferralProbationDeliveryUnit).isNotNull
          assertThat(persistedApplication.previousReferralProbationDeliveryUnit!!.id).isEqualTo(mainProbationDeliveryUnit.id)
          assertThat(persistedApplication.previousReferralProbationDeliveryUnit!!.probationRegion.id).isEqualTo(submittingUser.probationRegion.id)
        }
      }
    }

    @Test
    fun `Submit an application without out of region fields returns 200 and persists null out of region data`() {
      givenAUser { submittingUser, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val applicationId = UUID.randomUUID()
          val offenderName = "${offenderDetails.firstName} ${offenderDetails.surname}"

          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(submittingUser.probationRegion)
            withName("Main PDU")
          }

          temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withId(applicationId)
            withCreatedByUser(submittingUser)
            withProbationRegion(submittingUser.probationRegion)
            withName(offenderName)
            withData(
              """
                {}
              """,
            )
          }

          val cas3SubmitApplication = Cas3SubmitApplication(
            translatedDocument = {},
            arrivalDate = LocalDate.now(),
            summaryData = object {
              val num = 42
              val text = "Not out of region test!"
            },
            probationDeliveryUnitId = probationDeliveryUnit.id,
          )

          callApiAndAssertApiResponse(jwt, applicationId, cas3SubmitApplication)
            .expectStatus()
            .isOk

          val persistedApplication = temporaryAccommodationApplicationRepository.findByIdOrNull(applicationId)!!
          val persistedAssessment = persistedApplication.getLatestAssessment() as TemporaryAccommodationAssessmentEntity

          assertThat(persistedAssessment.summaryData).isEqualTo("{\"num\":42,\"text\":\"Not out of region test!\"}")
          assertThat(persistedApplication.name).isEqualTo(offenderName)

          assertThat(persistedApplication.probationRegion.id).isEqualTo(submittingUser.probationRegion.id)
          assertThat(persistedApplication.probationDeliveryUnit!!.id).isEqualTo(probationDeliveryUnit.id)

          assertThat(persistedApplication.previousReferralProbationRegion).isNull()
          assertThat(persistedApplication.previousReferralProbationDeliveryUnit).isNull()
        }
      }
    }

    private fun callApiAndAssertApiResponse(jwt: String, applicationId: UUID, submitApplication: Cas3SubmitApplication) = webTestClient.post()
      .uri("/cas3/applications/$applicationId/submission")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .bodyValue(submitApplication)
      .exchange()
  }

  @Nested
  inner class SoftDelete {
    @Test
    fun `soft delete application without JWT returns 401`() {
      webTestClient.delete()
        .uri("/cas3/applications/${UUID.randomUUID()}")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `soft delete inProgress application successfully returns 200`() {
      givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->

          val application =
            persistApplication(crn = offenderDetails.otherIds.crn, user = userEntity)

          webTestClient.delete()
            .uri("/cas3/applications/${application.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk

          Assertions.assertThat(temporaryAccommodationApplicationRepository.findById(application.id).get().deletedAt)
            .isNotNull()

          val domainEvents =
            domainEventRepository.findByApplicationIdAndType(
              applicationId = application.id,
              type = DomainEventType.CAS3_DRAFT_REFERRAL_DELETED,
            )

          assertThat(domainEvents.size).isEqualTo(1)
        }
      }
    }
  }

  private fun persistApplication(crn: String, user: UserEntity) = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
    withCrn(crn)
    withCreatedByUser(user)
    withProbationRegion(user.probationRegion)
    withArrivalDate(LocalDate.now().plusDays(30))
    withSubmittedAt(null)
  }

  private fun serializableToJsonNode(serializable: Any?): JsonNode {
    if (serializable == null) return NullNode.instance
    if (serializable is String) return objectMapper.readTree(serializable)

    return objectMapper.readTree(objectMapper.writeValueAsString(serializable))
  }
}
