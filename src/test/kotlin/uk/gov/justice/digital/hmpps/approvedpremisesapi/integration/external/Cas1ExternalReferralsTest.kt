package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.external

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ReferralHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1StaffDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASingleAccommodationServiceClientCredentialsApiCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.LocalDate
import java.time.OffsetDateTime

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
      givenAUser { user, jwt ->
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

          val assessment1 = createCas1Assessment(crn, user, AssessmentDecision.ACCEPTED, rejectionRationale = "Not suitable", apArea = apArea, cruManagementArea = cruManagementArea, premises = premises)
          val assessment2 = createCas1Assessment(crn, user, AssessmentDecision.REJECTED, rejectionRationale = "Not suitable", apArea = apArea, cruManagementArea = cruManagementArea, premises = premises)
          val assessment3 = createCas1Assessment(crn, user, AssessmentDecision.ACCEPTED, rejectionRationale = "Not suitable", apArea = apArea, cruManagementArea = cruManagementArea, premises = premises)
          val assessment4 = createCas1Assessment(crn, user, null, user, rejectionRationale = "Not suitable", apArea = apArea, cruManagementArea = cruManagementArea, premises = premises)
          val assessment5 = createCas1Assessment(crn, user, rejectionRationale = "Not suitable", apArea = apArea, cruManagementArea = cruManagementArea, premises = premises)

          val expectedReferrals = listOf(
            Cas1ReferralHistory(
              id = assessment1.id,
              applicationId = assessment1.application.id,
              createdAt = assessment1.createdAt.toInstant(),
              status = Cas1AssessmentStatus.completed,
              type = ServiceType.CAS1,
              referralRejectionReason = "Not suitable",
              localAuthorityArea = apArea.name,
              pdu = cruManagementArea.name,
              referredBy = createStaffDto(assessment1.application.createdByUser),
              placementAddress = "10 Test Street, London, SW1A 1AA",
              placementStatus = Cas1SpaceBookingStatus.ARRIVED.value,
              referralUrl = "http://frontend/applications/${assessment1.application.id}",
            ),
            Cas1ReferralHistory(
              id = assessment2.id,
              applicationId = assessment2.application.id,
              createdAt = assessment2.createdAt.toInstant(),
              status = Cas1AssessmentStatus.completed,
              type = ServiceType.CAS1,
              referralRejectionReason = "Not suitable",
              localAuthorityArea = apArea.name,
              pdu = cruManagementArea.name,
              referredBy = createStaffDto(assessment2.application.createdByUser),
              placementAddress = "10 Test Street, London, SW1A 1AA",
              placementStatus = Cas1SpaceBookingStatus.ARRIVED.value,
              referralUrl = "http://frontend/applications/${assessment2.application.id}",
            ),
            Cas1ReferralHistory(
              id = assessment3.id,
              applicationId = assessment3.application.id,
              createdAt = assessment3.createdAt.toInstant(),
              status = Cas1AssessmentStatus.completed,
              type = ServiceType.CAS1,
              referralRejectionReason = "Not suitable",
              localAuthorityArea = apArea.name,
              pdu = cruManagementArea.name,
              referredBy = createStaffDto(assessment3.application.createdByUser),
              placementAddress = "10 Test Street, London, SW1A 1AA",
              placementStatus = Cas1SpaceBookingStatus.ARRIVED.value,
              referralUrl = "http://frontend/applications/${assessment3.application.id}",
            ),
            Cas1ReferralHistory(
              id = assessment4.id,
              applicationId = assessment4.application.id,
              createdAt = assessment4.createdAt.toInstant(),
              status = Cas1AssessmentStatus.inProgress,
              type = ServiceType.CAS1,
              referralRejectionReason = "Not suitable",
              localAuthorityArea = apArea.name,
              pdu = cruManagementArea.name,
              referredBy = createStaffDto(assessment4.application.createdByUser),
              placementAddress = "10 Test Street, London, SW1A 1AA",
              placementStatus = Cas1SpaceBookingStatus.ARRIVED.value,
              referralUrl = "http://frontend/applications/${assessment4.application.id}",
            ),
            Cas1ReferralHistory(
              id = assessment5.id,
              applicationId = assessment5.application.id,
              createdAt = assessment5.createdAt.toInstant(),
              status = Cas1AssessmentStatus.inProgress,
              type = ServiceType.CAS1,
              referralRejectionReason = "Not suitable",
              localAuthorityArea = apArea.name,
              pdu = cruManagementArea.name,
              referredBy = createStaffDto(assessment5.application.createdByUser),
              placementAddress = "10 Test Street, London, SW1A 1AA",
              placementStatus = Cas1SpaceBookingStatus.ARRIVED.value,
              referralUrl = "http://frontend/applications/${assessment5.application.id}",
            ),
          )

          val response = webTestClient.get()
            .uri("/cas1/external/referrals/$crn")
            .header("Authorization", "Bearer $clientCredentialsJwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList(Cas1ReferralHistory::class.java)
            .returnResult()
            .responseBody

          Assertions.assertThat(response).containsExactlyInAnyOrderElementsOf(expectedReferrals)
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
    rejectionRationale: String? = null,
    apArea: ApAreaEntity? = null,
    cruManagementArea: Cas1CruManagementAreaEntity? = null,
    premises: ApprovedPremisesEntity? = null,
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
      withRejectionRationale(rejectionRationale)
      withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
    }
    if (premises != null) {
      cas1SpaceBookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withApplication(application)
        withPlacementRequest(null)
        withCreatedBy(user)
        withCrn(crn)
        withActualArrivalDate(LocalDate.now())
      }
    }
    return assessment
  }
}
