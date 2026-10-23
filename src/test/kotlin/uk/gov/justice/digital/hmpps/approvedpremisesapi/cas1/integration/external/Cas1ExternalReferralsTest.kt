package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.external

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason.noCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ReferralHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1StaffDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.RequestForPlacementAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASingleAccommodationServiceClientCredentialsApiCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1ExternalReferralsTest : IntegrationTestBase() {
  private val crn = "ABC1234"

  @Nested
  inner class GetReferralsByCrn {
    @Test
    fun `Get all referrals without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/external/referrals/$crn")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get all referrals without correct JWT authority returns 401`() {
      givenAUser { _, jwt ->
        webTestClient.get()
          .uri("/cas1/external/referrals/$crn")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get all referrals returns ok`() {
      givenAUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          val apArea = givenAnApArea(name = "London AP Area")
          val cruManagementArea = givenACas1CruManagementArea(name = "London CRU")

          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withCruManagementArea(cruManagementArea)
            withAddressLine1("10 Test Street")
            withTown("London")
            withPostcode("SW1A 1AA")
            withProbationRegion(givenAProbationRegion())
            withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
          }

          val assessment1 = createCas1Assessment(crn, user, AssessmentDecision.ACCEPTED, apArea = apArea, cruManagementArea = cruManagementArea, premises = premises)
          val assessment2 = createCas1Assessment(crn, user, AssessmentDecision.REJECTED, apArea = apArea, cruManagementArea = cruManagementArea, premises = premises)
          val assessment3 = createCas1Assessment(crn, user, AssessmentDecision.ACCEPTED, apArea = apArea, cruManagementArea = cruManagementArea, premises = premises)
          val assessment4 = createCas1Assessment(crn, user, null, user, apArea = apArea, cruManagementArea = cruManagementArea, premises = premises)
          val assessment5 = createCas1Assessment(crn, user, apArea = apArea, cruManagementArea = cruManagementArea, premises = premises, withdrawalReason = noCapacity)

          val expectedReferrals = listOf(
            Cas1ReferralHistory(
              id = assessment1.id,
              applicationId = assessment1.application.id,
              date = assessment1.createdAt.toLocalDate(),
              applicationStatus = (assessment1.application as ApprovedPremisesApplicationEntity).status,
              type = ServiceType.CAS1,
              referralRejectionReason = null,
              localAuthorityArea = apArea.name,
              pdu = cruManagementArea.name,
              referredBy = createStaffDto(assessment1.application.createdByUser),
              placementAddress = "10 Test Street, London, SW1A 1AA",
              placementStatus = Cas1SpaceBookingStatus.ARRIVED,
              requestForPlacementStatus = RequestForPlacementStatus.placementBooked,
              uiUrl = "http://frontend/applications/${assessment1.application.id}",
              withdrawalReason = null,
            ),
            Cas1ReferralHistory(
              id = assessment2.id,
              applicationId = assessment2.application.id,
              date = assessment2.createdAt.toLocalDate(),
              applicationStatus = (assessment2.application as ApprovedPremisesApplicationEntity).status,
              type = ServiceType.CAS1,
              referralRejectionReason = null,
              localAuthorityArea = apArea.name,
              pdu = cruManagementArea.name,
              referredBy = createStaffDto(assessment2.application.createdByUser),
              placementAddress = "10 Test Street, London, SW1A 1AA",
              placementStatus = Cas1SpaceBookingStatus.ARRIVED,
              requestForPlacementStatus = RequestForPlacementStatus.placementBooked,
              uiUrl = "http://frontend/applications/${assessment2.application.id}",
              withdrawalReason = null,
            ),
            Cas1ReferralHistory(
              id = assessment3.id,
              applicationId = assessment3.application.id,
              date = assessment3.createdAt.toLocalDate(),
              applicationStatus = (assessment3.application as ApprovedPremisesApplicationEntity).status,
              type = ServiceType.CAS1,
              referralRejectionReason = null,
              localAuthorityArea = apArea.name,
              pdu = cruManagementArea.name,
              referredBy = createStaffDto(assessment3.application.createdByUser),
              placementAddress = "10 Test Street, London, SW1A 1AA",
              placementStatus = Cas1SpaceBookingStatus.ARRIVED,
              requestForPlacementStatus = RequestForPlacementStatus.placementBooked,
              uiUrl = "http://frontend/applications/${assessment3.application.id}",
              withdrawalReason = null,
            ),
            Cas1ReferralHistory(
              id = assessment4.id,
              applicationId = assessment4.application.id,
              date = assessment4.createdAt.toLocalDate(),
              applicationStatus = (assessment4.application as ApprovedPremisesApplicationEntity).status,
              type = ServiceType.CAS1,
              referralRejectionReason = null,
              localAuthorityArea = apArea.name,
              pdu = cruManagementArea.name,
              referredBy = createStaffDto(assessment4.application.createdByUser),
              placementAddress = "10 Test Street, London, SW1A 1AA",
              placementStatus = Cas1SpaceBookingStatus.ARRIVED,
              requestForPlacementStatus = RequestForPlacementStatus.placementBooked,
              uiUrl = "http://frontend/applications/${assessment4.application.id}",
              withdrawalReason = null,
            ),
            Cas1ReferralHistory(
              id = assessment5.id,
              applicationId = assessment5.application.id,
              date = assessment5.createdAt.toLocalDate(),
              applicationStatus = (assessment5.application as ApprovedPremisesApplicationEntity).status,
              type = ServiceType.CAS1,
              referralRejectionReason = null,
              localAuthorityArea = apArea.name,
              pdu = cruManagementArea.name,
              referredBy = createStaffDto(assessment5.application.createdByUser),
              placementAddress = null,
              placementStatus = null,
              requestForPlacementStatus = RequestForPlacementStatus.requestWithdrawn,
              uiUrl = "http://frontend/applications/${assessment5.application.id}",
              withdrawalReason = noCapacity,
            ),
          )

          val response = webTestClient.get()
            .uri("/cas1/external/referrals/$crn")
            .header("Authorization", "Bearer $clientCredentialsJwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList<Cas1ReferralHistory>()
            .returnResult()
            .responseBody

          Assertions.assertThat(response).containsExactlyInAnyOrderElementsOf(expectedReferrals)
        }
      }
    }

    @Test
    fun `Get referral with placement request rejected returns ok`() {
      givenAUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          val apArea = givenAnApArea(name = "London AP Area")
          val cruManagementArea = givenACas1CruManagementArea(name = "London CRU")

          val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(crn)
            withCreatedByUser(user)
            withApArea(apArea)
            withCruManagementArea(cruManagementArea)
          }

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

          val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withAllocatedToUser(user)
            withDecision(AssessmentDecision.ACCEPTED)
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          }

          val expectedReferrals = listOf(
            Cas1ReferralHistory(
              id = assessment.id,
              applicationId = application.id,
              date = assessment.createdAt.toLocalDate(),
              applicationStatus = application.status,
              type = ServiceType.CAS1,
              referralRejectionReason = "NO SPACE",
              localAuthorityArea = apArea.name,
              pdu = cruManagementArea.name,
              referredBy = createStaffDto(application.createdByUser),
              placementAddress = null,
              placementStatus = null,
              requestForPlacementStatus = RequestForPlacementStatus.requestRejected,
              uiUrl = "http://frontend/applications/${application.id}",
              withdrawalReason = null,
            ),
          )

          val response = webTestClient.get()
            .uri("/cas1/external/referrals/$crn")
            .header("Authorization", "Bearer $clientCredentialsJwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList<Cas1ReferralHistory>()
            .returnResult()
            .responseBody

          Assertions.assertThat(response).containsExactlyInAnyOrderElementsOf(expectedReferrals)
        }
      }
    }

    @Test
    fun `Get referrals filters out reallocated assessments`() {
      givenAUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          val apArea = givenAnApArea(name = "London AP Area")
          val cruManagementArea = givenACas1CruManagementArea(name = "London CRU")

          val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(crn)
            withCreatedByUser(user)
            withApArea(apArea)
            withCruManagementArea(cruManagementArea)
          }

          approvedPremisesAssessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withAllocatedToUser(user)
            withReallocatedAt(OffsetDateTime.now())
            withCreatedAt(OffsetDateTime.now().minusDays(2).roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          }

          val currentAssessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withAllocatedToUser(user)
            withCreatedAt(OffsetDateTime.now().minusDays(1).roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          }

          val response = webTestClient.get()
            .uri("/cas1/external/referrals/$crn")
            .header("Authorization", "Bearer $clientCredentialsJwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList<Cas1ReferralHistory>()
            .returnResult()
            .responseBody

          Assertions.assertThat(response).hasSize(1)
          Assertions.assertThat(response!![0].id).isEqualTo(currentAssessment.id)
        }
      }
    }

    @Test
    fun `Get referrals for application with appeal returns only the latest assessment`() {
      givenAUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          val apArea = givenAnApArea(name = "London AP Area")
          val cruManagementArea = givenACas1CruManagementArea(name = "London CRU")

          val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(crn)
            withCreatedByUser(user)
            withApArea(apArea)
            withCruManagementArea(cruManagementArea)
          }

          approvedPremisesAssessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withAllocatedToUser(user)
            withDecision(AssessmentDecision.REJECTED)
            withCreatedAt(OffsetDateTime.now().minusDays(5).roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          }

          val appealedAssessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withAllocatedToUser(user)
            withDecision(AssessmentDecision.ACCEPTED)
            withCreatedFromAppeal(true)
            withCreatedAt(OffsetDateTime.now().minusDays(1).roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          }

          val response = webTestClient.get()
            .uri("/cas1/external/referrals/$crn")
            .header("Authorization", "Bearer $clientCredentialsJwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList<Cas1ReferralHistory>()
            .returnResult()
            .responseBody

          Assertions.assertThat(response).hasSize(1)
          Assertions.assertThat(response!![0].id).isEqualTo(appealedAssessment.id)
          Assertions.assertThat(response[0].applicationId).isEqualTo(application.id)
        }
      }
    }

    @Test
    fun `Get referrals for unassessed started application returns referral with application id`() {
      givenAUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          val apArea = givenAnApArea(name = "London AP Area")
          val cruManagementArea = givenACas1CruManagementArea(name = "London CRU")

          val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(crn)
            withCreatedByUser(user)
            withApArea(apArea)
            withCruManagementArea(cruManagementArea)
            withStatus(ApprovedPremisesApplicationStatus.STARTED)
            withSubmittedAt(null)
          }

          val response = webTestClient.get()
            .uri("/cas1/external/referrals/$crn")
            .header("Authorization", "Bearer $clientCredentialsJwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList<Cas1ReferralHistory>()
            .returnResult()
            .responseBody

          val expectedReferral = Cas1ReferralHistory(
            id = application.id,
            applicationId = application.id,
            date = application.createdAt.toLocalDate(),
            applicationStatus = ApprovedPremisesApplicationStatus.STARTED,
            type = ServiceType.CAS1,
            referralRejectionReason = null,
            localAuthorityArea = apArea.name,
            pdu = cruManagementArea.name,
            referredBy = createStaffDto(application.createdByUser),
            placementAddress = null,
            placementStatus = null,
            requestForPlacementStatus = null,
            uiUrl = "http://frontend/applications/${application.id}",
            withdrawalReason = null,
          )

          Assertions.assertThat(response).containsExactly(expectedReferral)
        }
      }
    }
  }

  private fun createStaffDto(user: UserEntity) = Cas1StaffDto(user.name, user.deliusUsername, user.deliusStaffCode)

  @Suppress("LongParameterList")
  private fun createCas1Assessment(
    crn: String,
    user: UserEntity,
    decision: AssessmentDecision? = null,
    allocated: UserEntity? = null,
    apArea: ApAreaEntity? = null,
    cruManagementArea: Cas1CruManagementAreaEntity? = null,
    premises: ApprovedPremisesEntity? = null,
    withdrawalReason: WithdrawPlacementRequestReason? = null,
  ): ApprovedPremisesAssessmentEntity {
    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCrn(crn)
      withCreatedByUser(user)
      if (apArea != null) withApArea(apArea)
      if (cruManagementArea != null) withCruManagementArea(cruManagementArea)
    }

    val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withAllocatedToUser(allocated ?: user)
      withDecision(decision)
      withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
    }
    if (premises != null) {
      val placementRequirements = placementRequirementsFactory.produceAndPersist {
        withApplication(application)
        withAssessment(assessment)
        withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
        withEssentialCriteria(listOf())
        withDesirableCriteria(listOf())
      }
      val placementRequest = placementRequestFactory.produceAndPersist {
        withApplication(application)
        withAssessment(assessment)
        withPlacementRequirements(placementRequirements)
        withCreatedAt(OffsetDateTime.now())
        withExpectedArrival(LocalDate.now())
        withDuration(7)
        if (withdrawalReason != null) {
          withIsWithdrawn(true)
          withWithdrawalReason(PlacementRequestWithdrawalReason.valueOf(withdrawalReason))
        }
      }
      if (withdrawalReason == null) {
        cas1SpaceBookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withApplication(application)
          withPlacementRequest(placementRequest)
          withCreatedBy(user)
          withCrn(crn)
          withExpectedArrivalDate(LocalDate.now())
          withExpectedDepartureDate(LocalDate.now().plusDays(7))
          withActualArrivalDate(LocalDate.now())
        }
      }
    }

    return assessment
  }
}
