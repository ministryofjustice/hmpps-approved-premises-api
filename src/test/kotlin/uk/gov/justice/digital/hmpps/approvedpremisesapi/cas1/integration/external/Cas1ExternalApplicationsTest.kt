package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalApplicationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalAssessmentDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPlacementDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPremisesDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalRequestForPlacementDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1StaffDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.PlacementApplicationDecisionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.MatchRequestWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlacementApplicationWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.RequestForPlacementAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASingleAccommodationServiceClientCredentialsApiCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision as AssessmentDecisionApi
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision as AssessmentDecisionJpa

class Cas1ExternalApplicationsTest : IntegrationTestBase() {
  private val crn = "ABC1234"
  private val applicationSubmittedAt = OffsetDateTime.parse("2023-01-01T12:00:00.00Z")
    .truncatedTo(ChronoUnit.MICROS)
  private val assessmentSubmittedAt = OffsetDateTime.parse("2023-07-01T12:00:00.00Z")
    .truncatedTo(ChronoUnit.MICROS)
  fun transformToStaffDto(user: UserEntity) = Cas1StaffDto(user.name, user.deliusUsername, user.deliusStaffCode)

  @Nested
  inner class GetSuitableApplicationsByCrn {
    @Test
    fun `Get suitable application without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/external/cases/$crn/applications/suitable")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get suitable application without correct JWT authority returns 403`() {
      givenAUser { _, jwt ->
        webTestClient.get()
          .uri("/cas1/external/cases/$crn/applications/suitable")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get suitable application returns ok`() {
      givenAUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          givenAnAssessmentForApprovedPremises(
            allocatedToUser = null,
            createdByUser = user,
            submittedAt = assessmentSubmittedAt,
            decision = AssessmentDecisionJpa.ACCEPTED,
          ) { assessment, application ->

            application.status = ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED
            application.submittedAt = applicationSubmittedAt

            approvedPremisesApplicationRepository.save(application)

            val placementRequirements = placementRequirementsFactory.produceAndPersist {
              withApplication(application)
              withAssessment(assessment)
              withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
              withEssentialCriteria(listOf())
              withDesirableCriteria(listOf())
            }

            val placementApplication = placementApplicationFactory.produceAndPersist {
              withApplication(application)
              withDecision(PlacementApplicationDecision.ACCEPTED)
              withCreatedAt(OffsetDateTime.parse("2007-08-03T10:15:30+01"))
              withCreatedByUser(user)
              withSubmittedAt(OffsetDateTime.now())
              withReallocatedAt(null)
              withAuthorisedDuration(20)
              withExpectedArrival(LocalDate.now().plusDays(10))
              withRequestedDuration(10)
            }

            val placementRequest = placementRequestFactory.produceAndPersist {
              withCreatedAt(OffsetDateTime.parse("2007-08-03T10:15:30+01"))
              withApplication(application)
              withAssessment(assessment)
              withPlacementRequirements(placementRequirements)
              withPlacementApplication(placementApplication)
            }

            val region = givenAProbationRegion()

            val premises = givenAnApprovedPremises(
              region = region,
              supportsSpaceBookings = true,
            )

            val (offender) = givenAnOffender()

            val booking = cas1SpaceBookingEntityFactory.produceAndPersist {
              withCrn(offender.otherIds.crn)
              withPremises(premises)
              withPlacementRequest(placementRequest)
              withApplication(placementRequest.application)
              withCreatedBy(user)
              withExpectedArrivalDate(LocalDate.now())
              withExpectedDepartureDate(LocalDate.now().plusDays(10))
              withActualDepartureDate(null)
              withActualArrivalDate(null)
            }

            val suitableApplication = Cas1SuitableApplication(
              id = application.id,
              applicationStatus = ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED,
              requestForPlacementStatus = RequestForPlacementStatus.placementBooked,
              placementStatus = Cas1SpaceBookingStatus.UPCOMING,
              premises = Cas1ExternalPremisesDto(
                startDate = booking.expectedArrivalDate,
                endDate = booking.expectedDepartureDate,
                addressLine1 = premises.addressLine1,
                addressLine2 = premises.addressLine2,
                town = premises.town,
                postcode = premises.postcode,
              ),
              uiUrl = "http://frontend/applications/${application.id}",
              application = Cas1ExternalApplicationDto(
                createdAt = application.createdAt,
                createdBy = transformToStaffDto(user),
                submittedAt = applicationSubmittedAt,
                expiresAt = assessment.submittedAt?.toLocalDate()?.plusDays(365),
                status = ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED,
                id = application.id,
              ),
              assessment = Cas1ExternalAssessmentDto(
                decision = AssessmentDecisionApi.forValue(assessment.decision.toString()),
                rejectionRationale = null,
              ),
              requestForPlacement = Cas1ExternalRequestForPlacementDto(
                decision = placementApplication.decision?.apiValue,
                rejectionReason = null,
                submittedBy = transformToStaffDto(placementApplication.createdByUser),
                submittedAt = placementApplication.submittedAt?.toLocalDate(),
                withdrawalReason = null,
                withdrawalDate = null,
                expectedArrivalDate = booking.expectedArrivalDate,
                durationDays = 20,
                status = RequestForPlacementStatus.placementBooked,
              ),
              placement = Cas1ExternalPlacementDto(
                actualArrivalDate = booking.actualArrivalDate,
                actualDepartureDate = booking.actualDepartureDate,
                cancellationReason = booking.cancellationReason?.name,
                premises = Cas1ExternalPremisesDto(
                  startDate = booking.expectedArrivalDate,
                  endDate = booking.expectedDepartureDate,
                  addressLine1 = premises.addressLine1,
                  addressLine2 = premises.addressLine2,
                  town = premises.town,
                  postcode = premises.postcode,
                ),
                status = Cas1SpaceBookingStatus.UPCOMING,
              ),
              placementHistory = emptyList(),
            )

            val response = webTestClient.get()
              .uri("/cas1/external/cases/${application.crn}/applications/suitable")
              .header("Authorization", "Bearer $clientCredentialsJwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody(Cas1SuitableApplication::class.java)
              .returnResult()
              .responseBody

            assertThat(response).isEqualTo(suitableApplication)
          }
        }
      }
    }

    @Test
    fun `Get suitable rejected application returns ok`() {
      givenAUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          givenAnAssessmentForApprovedPremises(
            allocatedToUser = null,
            createdByUser = user,
            submittedAt = assessmentSubmittedAt,
            decision = AssessmentDecisionJpa.REJECTED,
          ) { assessment, application ->

            assessment.rejectionRationale = "NO SPACE"
            application.status = ApprovedPremisesApplicationStatus.REJECTED
            application.submittedAt = applicationSubmittedAt

            approvedPremisesAssessmentRepository.save(assessment)
            approvedPremisesApplicationRepository.save(application)

            val suitableApplication = Cas1SuitableApplication(
              id = application.id,
              applicationStatus = ApprovedPremisesApplicationStatus.REJECTED,
              requestForPlacementStatus = null,
              placementStatus = null,
              premises = null,
              uiUrl = "http://frontend/applications/${application.id}",
              application = Cas1ExternalApplicationDto(
                createdAt = application.createdAt,
                createdBy = transformToStaffDto(user),
                submittedAt = applicationSubmittedAt,
                expiresAt = null,
                status = ApprovedPremisesApplicationStatus.REJECTED,
                id = application.id,
              ),
              assessment = Cas1ExternalAssessmentDto(
                decision = AssessmentDecisionApi.forValue(assessment.decision.toString()),
                rejectionRationale = assessment.rejectionRationale,
              ),
              requestForPlacement = null,
              placement = null,
              placementHistory = emptyList(),
            )

            val response = webTestClient.get()
              .uri("/cas1/external/cases/${application.crn}/applications/suitable")
              .header("Authorization", "Bearer $clientCredentialsJwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody(Cas1SuitableApplication::class.java)
              .returnResult()
              .responseBody

            assertThat(response).isEqualTo(suitableApplication)
          }
        }
      }
    }

    @Test
    fun `Get suitable application returns ok and placement is rejected`() {
      givenAUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          givenAnAssessmentForApprovedPremises(
            allocatedToUser = null,
            createdByUser = user,
            submittedAt = assessmentSubmittedAt,
            decision = AssessmentDecisionJpa.ACCEPTED,
          ) { assessment, application ->

            application.status = ApprovedPremisesApplicationStatus.REJECTED
            application.submittedAt = applicationSubmittedAt

            approvedPremisesApplicationRepository.save(application)

            val placementApplication = placementApplicationFactory.produceAndPersist {
              withApplication(application)
              withDecision(PlacementApplicationDecision.REJECTED)
              withCreatedAt(OffsetDateTime.parse("2007-08-03T10:15:30+01"))
              withCreatedByUser(user)
              withSubmittedAt(OffsetDateTime.now())
              withReallocatedAt(null)
              withAuthorisedDuration(20)
              withExpectedArrival(LocalDate.now().plusDays(10))
              withRequestedDuration(10)
              withDecisionMadeAt(OffsetDateTime.now())
            }

            val envelopedData = Cas1DomainEventEnvelope(
              id = UUID.randomUUID(),
              timestamp = Instant.now(),
              eventType = EventType.requestForPlacementAssessed,
              eventDetails = RequestForPlacementAssessedFactory()
                .withPlacementApplicationId(placementApplication.id)
                .withDecisionSummary("NO SPACE")
                .withApplicationId(application.id)
                .produce(),
            )

            domainEventFactory.produceAndPersist {
              withData(jsonMapper.writeValueAsString(envelopedData))
              withType(DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED)
              withOccurredAt(OffsetDateTime.now().minusDays(6))
            }

            val suitableApplication = Cas1SuitableApplication(
              id = application.id,
              applicationStatus = ApprovedPremisesApplicationStatus.REJECTED,
              requestForPlacementStatus = RequestForPlacementStatus.requestRejected,
              placementStatus = null,
              premises = null,
              uiUrl = "http://frontend/applications/${application.id}",
              application = Cas1ExternalApplicationDto(
                createdAt = application.createdAt,
                createdBy = transformToStaffDto(user),
                submittedAt = applicationSubmittedAt,
                expiresAt = assessment.submittedAt?.toLocalDate()?.plusDays(365),
                status = ApprovedPremisesApplicationStatus.REJECTED,
                id = application.id,
              ),
              assessment = Cas1ExternalAssessmentDto(
                decision = AssessmentDecisionApi.forValue(assessment.decision.toString()),
                rejectionRationale = null,
              ),
              requestForPlacement = Cas1ExternalRequestForPlacementDto(
                decision = placementApplication.decision?.apiValue,
                rejectionReason = envelopedData.eventDetails.decisionSummary,
                submittedBy = transformToStaffDto(placementApplication.createdByUser),
                submittedAt = placementApplication.submittedAt?.toLocalDate(),
                withdrawalReason = null,
                withdrawalDate = null,
                expectedArrivalDate = placementApplication.expectedArrival,
                durationDays = 20,
                status = RequestForPlacementStatus.requestRejected,
              ),
              placement = null,
              placementHistory = emptyList(),
            )

            val response = webTestClient.get()
              .uri("/cas1/external/cases/${application.crn}/applications/suitable")
              .header("Authorization", "Bearer $clientCredentialsJwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody(Cas1SuitableApplication::class.java)
              .returnResult()
              .responseBody

            assertThat(response).isEqualTo(suitableApplication)
          }
        }
      }
    }

    @Test
    fun `Get suitable application with placement application withdrawn returns ok`() {
      givenAUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          givenAnAssessmentForApprovedPremises(
            allocatedToUser = null,
            createdByUser = user,
            submittedAt = assessmentSubmittedAt,
            decision = AssessmentDecisionJpa.ACCEPTED,
          ) { assessment, application ->

            application.status = ApprovedPremisesApplicationStatus.WITHDRAWN
            application.submittedAt = applicationSubmittedAt

            approvedPremisesApplicationRepository.save(application)

            val placementRequirements = placementRequirementsFactory.produceAndPersist {
              withApplication(application)
              withAssessment(assessment)
              withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
              withEssentialCriteria(listOf())
              withDesirableCriteria(listOf())
            }

            val placementApplication = placementApplicationFactory.produceAndPersist {
              withIsWithdrawn(true)
              withWithdrawalReason(PlacementApplicationWithdrawalReason.ERROR_IN_PLACEMENT_REQUEST)
              withApplication(application)
              withDecision(PlacementApplicationDecision.ACCEPTED)
              withCreatedAt(OffsetDateTime.parse("2007-08-03T10:15:30+01"))
              withCreatedByUser(user)
              withSubmittedAt(OffsetDateTime.now())
              withReallocatedAt(null)
              withAuthorisedDuration(20)
              withExpectedArrival(LocalDate.now().plusDays(10))
              withRequestedDuration(10)
            }

            val placementRequest = placementRequestFactory.produceAndPersist {
              withCreatedAt(OffsetDateTime.parse("2007-08-03T10:15:30+01"))
              withApplication(application)
              withAssessment(assessment)
              withPlacementRequirements(placementRequirements)
              withPlacementApplication(placementApplication)
            }

            val region = givenAProbationRegion()

            val premises = givenAnApprovedPremises(
              region = region,
              supportsSpaceBookings = true,
            )

            val (offender) = givenAnOffender()

            val cancellationReason = cancellationReasonEntityFactory.produceAndPersist {
              withName("A problem")
            }

            val booking = cas1SpaceBookingEntityFactory.produceAndPersist {
              withCrn(offender.otherIds.crn)
              withPremises(premises)
              withPlacementRequest(placementRequest)
              withApplication(placementRequest.application)
              withCreatedBy(user)
              withExpectedArrivalDate(LocalDate.now())
              withExpectedDepartureDate(LocalDate.now().plusDays(10))
              withActualDepartureDate(null)
              withActualArrivalDate(null)
              withCancellationReason(cancellationReason)
              withCancellationOccurredAt(LocalDate.now().minusDays(10))
              withCancellationRecordedAt(Instant.now())
              withCancellationReasonNotes("Notes")
            }

            val envelopedData = Cas1DomainEventEnvelope(
              id = UUID.randomUUID(),
              timestamp = Instant.now(),
              eventType = EventType.placementApplicationWithdrawn,
              eventDetails = PlacementApplicationWithdrawnFactory()
                .withPlacementApplicationId(placementApplication.id)
                .produce(),
            )

            val withDrawnDomainEntity = domainEventFactory.produceAndPersist {
              withData(jsonMapper.writeValueAsString(envelopedData))
              withType(DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN)
              withOccurredAt(OffsetDateTime.now().minusDays(6))
            }

            val suitableApplication = Cas1SuitableApplication(
              id = application.id,
              applicationStatus = ApprovedPremisesApplicationStatus.WITHDRAWN,
              requestForPlacementStatus = RequestForPlacementStatus.requestWithdrawn,
              placementStatus = Cas1SpaceBookingStatus.CANCELLED,
              premises = Cas1ExternalPremisesDto(
                startDate = booking.expectedArrivalDate,
                endDate = booking.expectedDepartureDate,
                addressLine1 = premises.addressLine1,
                addressLine2 = premises.addressLine2,
                town = premises.town,
                postcode = premises.postcode,
              ),
              uiUrl = "http://frontend/applications/${application.id}",
              application = Cas1ExternalApplicationDto(
                createdAt = application.createdAt,
                createdBy = transformToStaffDto(user),
                submittedAt = applicationSubmittedAt,
                expiresAt = assessment.submittedAt?.toLocalDate()?.plusDays(365),
                status = ApprovedPremisesApplicationStatus.WITHDRAWN,
                id = application.id,
              ),
              assessment = Cas1ExternalAssessmentDto(
                decision = AssessmentDecisionApi.forValue(assessment.decision.toString()),
                rejectionRationale = null,
              ),
              requestForPlacement = Cas1ExternalRequestForPlacementDto(
                decision = placementApplication.decision?.apiValue,
                rejectionReason = null,
                submittedBy = transformToStaffDto(placementApplication.createdByUser),
                submittedAt = placementApplication.submittedAt?.toLocalDate(),
                withdrawalReason = WithdrawPlacementRequestReason.errorInPlacementRequest,
                withdrawalDate = withDrawnDomainEntity.occurredAt.toLocalDate(),
                expectedArrivalDate = booking.expectedArrivalDate,
                durationDays = 20,
                status = RequestForPlacementStatus.requestWithdrawn,
              ),
              placement = Cas1ExternalPlacementDto(
                actualArrivalDate = booking.actualArrivalDate,
                actualDepartureDate = booking.actualDepartureDate,
                cancellationReason = booking.cancellationReason?.name,
                status = Cas1SpaceBookingStatus.CANCELLED,
                premises = Cas1ExternalPremisesDto(
                  startDate = booking.expectedArrivalDate,
                  endDate = booking.expectedDepartureDate,
                  addressLine1 = premises.addressLine1,
                  addressLine2 = premises.addressLine2,
                  town = premises.town,
                  postcode = premises.postcode,
                ),
              ),
              placementHistory = emptyList(),
            )

            val response = webTestClient.get()
              .uri("/cas1/external/cases/${application.crn}/applications/suitable")
              .header("Authorization", "Bearer $clientCredentialsJwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody(Cas1SuitableApplication::class.java)
              .returnResult()
              .responseBody

            assertThat(response).isEqualTo(suitableApplication)
          }
        }
      }
    }

    @Test
    fun `Get suitable application with placement request withdrawn returns ok`() {
      givenAUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          givenAnAssessmentForApprovedPremises(
            allocatedToUser = null,
            createdByUser = user,
            submittedAt = assessmentSubmittedAt,
            decision = AssessmentDecisionJpa.ACCEPTED,
          ) { assessment, application ->

            application.status = ApprovedPremisesApplicationStatus.WITHDRAWN
            application.submittedAt = applicationSubmittedAt

            approvedPremisesApplicationRepository.save(application)

            val placementRequirements = placementRequirementsFactory.produceAndPersist {
              withApplication(application)
              withAssessment(assessment)
              withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
              withEssentialCriteria(listOf())
              withDesirableCriteria(listOf())
            }

            val placementRequest = placementRequestFactory.produceAndPersist {
              withCreatedAt(OffsetDateTime.parse("2007-08-03T10:15:30+01"))
              withApplication(application)
              withAssessment(assessment)
              withPlacementRequirements(placementRequirements)
              withPlacementApplication(null)
              withIsWithdrawn(true)
              withWithdrawalReason(PlacementRequestWithdrawalReason.ERROR_IN_PLACEMENT_REQUEST)
              withDuration(30)
            }

            val region = givenAProbationRegion()

            val premises = givenAnApprovedPremises(
              region = region,
              supportsSpaceBookings = true,
            )

            val (offender) = givenAnOffender()

            val booking = cas1SpaceBookingEntityFactory.produceAndPersist {
              withCrn(offender.otherIds.crn)
              withPremises(premises)
              withPlacementRequest(placementRequest)
              withApplication(placementRequest.application)
              withCreatedBy(user)
              withExpectedArrivalDate(LocalDate.now())
              withExpectedDepartureDate(LocalDate.now().plusDays(10))
              withActualDepartureDate(null)
              withActualArrivalDate(null)
            }

            val envelopedData = Cas1DomainEventEnvelope(
              id = UUID.randomUUID(),
              timestamp = Instant.now(),
              eventType = EventType.matchRequestWithdrawn,
              eventDetails = MatchRequestWithdrawnFactory()
                .withMatchRequestId(placementRequest.id)
                .produce(),
            )

            val withDrawnDomainEntity = domainEventFactory.produceAndPersist {
              withData(jsonMapper.writeValueAsString(envelopedData))
              withType(DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN)
              withOccurredAt(OffsetDateTime.now().minusDays(6))
            }

            val suitableApplication = Cas1SuitableApplication(
              id = application.id,
              applicationStatus = ApprovedPremisesApplicationStatus.WITHDRAWN,
              requestForPlacementStatus = RequestForPlacementStatus.requestWithdrawn,
              placementStatus = Cas1SpaceBookingStatus.UPCOMING,
              premises = Cas1ExternalPremisesDto(
                startDate = booking.expectedArrivalDate,
                endDate = booking.expectedDepartureDate,
                addressLine1 = premises.addressLine1,
                addressLine2 = premises.addressLine2,
                town = premises.town,
                postcode = premises.postcode,
              ),
              uiUrl = "http://frontend/applications/${application.id}",
              application = Cas1ExternalApplicationDto(
                createdAt = application.createdAt,
                createdBy = transformToStaffDto(user),
                submittedAt = application.submittedAt,
                status = ApprovedPremisesApplicationStatus.WITHDRAWN,
                expiresAt = assessment.submittedAt?.toLocalDate()?.plusDays(365),
                id = application.id,
              ),
              assessment = Cas1ExternalAssessmentDto(
                decision = AssessmentDecisionApi.forValue(assessment.decision.toString()),
                rejectionRationale = null,
              ),
              requestForPlacement = Cas1ExternalRequestForPlacementDto(
                decision = PlacementApplicationDecisionDto.accepted,
                rejectionReason = null,
                submittedBy = transformToStaffDto(application.createdByUser),
                submittedAt = placementRequest.createdAt.toLocalDate(),
                withdrawalReason = WithdrawPlacementRequestReason.errorInPlacementRequest,
                withdrawalDate = withDrawnDomainEntity.occurredAt.toLocalDate(),
                expectedArrivalDate = booking.expectedArrivalDate,
                durationDays = placementRequest.duration,
                status = RequestForPlacementStatus.requestWithdrawn,
              ),
              placement = Cas1ExternalPlacementDto(
                actualArrivalDate = booking.actualArrivalDate,
                actualDepartureDate = booking.actualDepartureDate,
                cancellationReason = booking.cancellationReason?.name,
                premises = Cas1ExternalPremisesDto(
                  startDate = booking.expectedArrivalDate,
                  endDate = booking.expectedDepartureDate,
                  addressLine1 = premises.addressLine1,
                  addressLine2 = premises.addressLine2,
                  town = premises.town,
                  postcode = premises.postcode,
                ),
                status = Cas1SpaceBookingStatus.UPCOMING,
              ),
              placementHistory = emptyList(),
            )

            val response = webTestClient.get()
              .uri("/cas1/external/cases/${application.crn}/applications/suitable")
              .header("Authorization", "Bearer $clientCredentialsJwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody(Cas1SuitableApplication::class.java)
              .returnResult()
              .responseBody

            assertThat(response).isEqualTo(suitableApplication)
          }
        }
      }
    }

    @Test
    fun `Get suitable application returns not found`() {
      givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
        webTestClient.get()
          .uri("/cas1/external/cases/$crn/applications/suitable")
          .header("Authorization", "Bearer $clientCredentialsJwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }

  @Nested
  inner class GetArrivedApplicationsByCrn {
    @Test
    fun `Get arrived application without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/external/cases/$crn/premises/current")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get arrived application without correct JWT authority returns 403`() {
      givenAUser { _, jwt ->
        webTestClient.get()
          .uri("/cas1/external/cases/$crn/premises/current")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get arrived application returns ok`() {
      givenAUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          givenAnAssessmentForApprovedPremises(
            allocatedToUser = null,
            createdByUser = user,
          ) { assessment, application ->

            application.status = ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED

            approvedPremisesApplicationRepository.save(application)

            val placementRequirements = placementRequirementsFactory.produceAndPersist {
              withApplication(application)
              withAssessment(assessment)
              withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
              withEssentialCriteria(listOf())
              withDesirableCriteria(listOf())
            }

            val placementRequest = placementRequestFactory.produceAndPersist {
              withCreatedAt(OffsetDateTime.parse("2007-08-03T10:15:30+01"))
              withApplication(application)
              withAssessment(assessment)
              withPlacementRequirements(placementRequirements)
            }

            val region = givenAProbationRegion()

            val premises = givenAnApprovedPremises(
              region = region,
              supportsSpaceBookings = true,
            )

            val (offender) = givenAnOffender()

            val booking = cas1SpaceBookingEntityFactory.produceAndPersist {
              withCrn(offender.otherIds.crn)
              withPremises(premises)
              withPlacementRequest(placementRequest)
              withApplication(placementRequest.application)
              withCreatedBy(user)
              withExpectedArrivalDate(LocalDate.now().minusDays(1))
              withExpectedDepartureDate(LocalDate.now().plusDays(10))
              withActualArrivalDate(LocalDate.now())
              withActualDepartureDate(null)
            }

            val currentPremises = Cas1ExternalPremisesDto(
              startDate = booking.actualArrivalDate,
              endDate = booking.expectedDepartureDate,
              addressLine1 = premises.addressLine1,
              addressLine2 = premises.addressLine2,
              town = premises.town,
              postcode = premises.postcode,
            )

            val response = webTestClient.get()
              .uri("/cas1/external/cases/${application.crn}/premises/current")
              .header("Authorization", "Bearer $clientCredentialsJwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody(Cas1ExternalPremisesDto::class.java)
              .returnResult()
              .responseBody

            assertThat(response).isEqualTo(currentPremises)
          }
        }
      }
    }

    @Test
    fun `Get arrived application returns not found when placement is not ARRIVED`() {
      givenAUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          givenAnAssessmentForApprovedPremises(
            allocatedToUser = null,
            createdByUser = user,
          ) { assessment, application ->

            application.status = ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED

            approvedPremisesApplicationRepository.save(application)

            val placementRequirements = placementRequirementsFactory.produceAndPersist {
              withApplication(application)
              withAssessment(assessment)
              withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
              withEssentialCriteria(listOf())
              withDesirableCriteria(listOf())
            }

            val placementRequest = placementRequestFactory.produceAndPersist {
              withCreatedAt(OffsetDateTime.parse("2007-08-03T10:15:30+01"))
              withApplication(application)
              withAssessment(assessment)
              withPlacementRequirements(placementRequirements)
            }

            val region = givenAProbationRegion()

            val premises = givenAnApprovedPremises(
              region = region,
              supportsSpaceBookings = true,
            )

            val (offender) = givenAnOffender()

            cas1SpaceBookingEntityFactory.produceAndPersist {
              withCrn(offender.otherIds.crn)
              withPremises(premises)
              withPlacementRequest(placementRequest)
              withApplication(placementRequest.application)
              withCreatedBy(user)
              withExpectedArrivalDate(LocalDate.now())
              withExpectedDepartureDate(LocalDate.now().plusDays(10))
            }

            webTestClient.get()
              .uri("/cas1/external/cases/${application.crn}/premises/current")
              .header("Authorization", "Bearer $clientCredentialsJwt")
              .exchange()
              .expectStatus()
              .isNotFound()
          }
        }
      }
    }

    @Test
    fun `Get arrived application returns not found`() {
      givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
        webTestClient.get()
          .uri("/cas1/external/cases/$crn/premises/current")
          .header("Authorization", "Bearer $clientCredentialsJwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }
}
