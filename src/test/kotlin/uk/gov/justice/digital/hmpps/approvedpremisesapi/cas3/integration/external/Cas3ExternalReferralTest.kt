package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.Cas3IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ReferralHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3StaffDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASingleAccommodationServiceClientCredentialsApiCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.OffsetDateTime

class Cas3ExternalReferralTest : Cas3IntegrationTestBase() {
  private val crn = "ABC1234"

  @Nested
  inner class GetReferralsByCrn {

    @Test
    fun `Get all referrals without correct JWT returns 403`() {
      givenAUser { user, jwt ->
        webTestClient.get()
          .uri("/cas3/external/referrals/$crn")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get all referrals without correct JWT returns 401`() {
      webTestClient.get()
        .uri("/cas3/external/referrals/$crn")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get all referrals returns ok`() {
      givenAUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          val premises = cas3PremisesEntityFactory.produceAndPersist {
            withAddressLine1("10 Test Street")
            withTown("London")
            withPostcode("SW1A 1AA")
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationDeliveryUnit(
              probationDeliveryUnitFactory.produceAndPersist {
                withProbationRegion(user.probationRegion)
              },
            )
          }

          val assessment1 = createAssessment(user, AssessmentDecision.ACCEPTED, premises = premises)
          val assessment2 = createAssessment(user, AssessmentDecision.REJECTED, premises = premises, referralRejectionReason = "A random rejection reason", referralRejectionReasonDetail = "Some random rejection detail")
          val assessment3 = createAssessment(user, AssessmentDecision.ACCEPTED, OffsetDateTime.now(), premises = premises)
          val assessment4 = createAssessment(user, null, null, user, premises = premises)
          val assessment5 = createAssessment(user, premises = premises)

          val expectedReferrals = listOf(
            Cas3ReferralHistory(
              id = assessment1.id,
              applicationId = assessment1.application.id,
              createdAt = assessment1.createdAt.toInstant(),
              applicationStatus = ApplicationStatus.submitted,
              assessmentStatus = assessment1.deriveAssessmentStatus(),
              type = ServiceType.CAS3,
              referralRejectionReason = null,
              referralRejectionReasonDetail = null,
              localAuthorityArea = (assessment1.application as TemporaryAccommodationApplicationEntity).dutyToReferLocalAuthorityAreaName,
              pdu = (assessment1.application as TemporaryAccommodationApplicationEntity).probationDeliveryUnit?.name,
              referredBy = createStaffDto(user),
              placementAddress = "10 Test Street, London, SW1A 1AA",
              bookingStatus = Cas3BookingStatus.provisional,
              uiUrl = "http://frontend.cas3/referrals/${assessment1.application.id}/full",
            ),
            Cas3ReferralHistory(
              id = assessment2.id,
              applicationId = assessment2.application.id,
              createdAt = assessment2.createdAt.toInstant(),
              applicationStatus = ApplicationStatus.submitted,
              assessmentStatus = assessment2.deriveAssessmentStatus(),
              type = ServiceType.CAS3,
              referralRejectionReason = "A random rejection reason",
              referralRejectionReasonDetail = "Some random rejection detail",
              localAuthorityArea = (assessment2.application as TemporaryAccommodationApplicationEntity).dutyToReferLocalAuthorityAreaName,
              pdu = (assessment2.application as TemporaryAccommodationApplicationEntity).probationDeliveryUnit?.name,
              referredBy = createStaffDto(user),
              placementAddress = "10 Test Street, London, SW1A 1AA",
              bookingStatus = Cas3BookingStatus.provisional,
              uiUrl = "http://frontend.cas3/referrals/${assessment2.application.id}/full",
            ),
            Cas3ReferralHistory(
              id = assessment3.id,
              applicationId = assessment3.application.id,
              createdAt = assessment3.createdAt.toInstant(),
              applicationStatus = ApplicationStatus.submitted,
              assessmentStatus = assessment3.deriveAssessmentStatus(),
              type = ServiceType.CAS3,
              referralRejectionReason = null,
              referralRejectionReasonDetail = null,
              localAuthorityArea = (assessment3.application as TemporaryAccommodationApplicationEntity).dutyToReferLocalAuthorityAreaName,
              pdu = (assessment3.application as TemporaryAccommodationApplicationEntity).probationDeliveryUnit?.name,
              referredBy = createStaffDto(user),
              placementAddress = "10 Test Street, London, SW1A 1AA",
              bookingStatus = Cas3BookingStatus.provisional,
              uiUrl = "http://frontend.cas3/referrals/${assessment3.application.id}/full",
            ),
            Cas3ReferralHistory(
              id = assessment4.id,
              applicationId = assessment4.application.id,
              createdAt = assessment4.createdAt.toInstant(),
              applicationStatus = ApplicationStatus.submitted,
              assessmentStatus = assessment4.deriveAssessmentStatus(),
              type = ServiceType.CAS3,
              referralRejectionReason = null,
              referralRejectionReasonDetail = null,
              localAuthorityArea = (assessment4.application as TemporaryAccommodationApplicationEntity).dutyToReferLocalAuthorityAreaName,
              pdu = (assessment4.application as TemporaryAccommodationApplicationEntity).probationDeliveryUnit?.name,
              referredBy = createStaffDto(user),
              placementAddress = "10 Test Street, London, SW1A 1AA",
              bookingStatus = Cas3BookingStatus.provisional,
              uiUrl = "http://frontend.cas3/referrals/${assessment4.application.id}/full",
            ),
            Cas3ReferralHistory(
              id = assessment5.id,
              applicationId = assessment5.application.id,
              createdAt = assessment5.createdAt.toInstant(),
              applicationStatus = ApplicationStatus.submitted,
              assessmentStatus = assessment5.deriveAssessmentStatus(),
              type = ServiceType.CAS3,
              referralRejectionReason = null,
              referralRejectionReasonDetail = null,
              localAuthorityArea = (assessment5.application as TemporaryAccommodationApplicationEntity).dutyToReferLocalAuthorityAreaName,
              pdu = (assessment5.application as TemporaryAccommodationApplicationEntity).probationDeliveryUnit?.name,
              referredBy = createStaffDto(user),
              placementAddress = "10 Test Street, London, SW1A 1AA",
              bookingStatus = Cas3BookingStatus.provisional,
              uiUrl = "http://frontend.cas3/referrals/${assessment5.application.id}/full",
            ),
          )

          val response = webTestClient.get()
            .uri("/cas3/external/referrals/$crn")
            .header("Authorization", "Bearer $clientCredentialsJwt")
            .exchange()
            .expectStatus()
            .isOk

          val responseBody = response
            .returnResult<String>()
            .responseBody
            .blockFirst()

          assertThat(responseBody).isEqualTo(jsonMapper.writeValueAsString(expectedReferrals))
        }
      }
    }
  }

  private fun createStaffDto(user: UserEntity) = Cas3StaffDto(user.name, user.deliusUsername, user.deliusStaffCode)

  private fun createAssessment(
    user: UserEntity,
    decision: AssessmentDecision? = null,
    completedAt: OffsetDateTime? = null,
    allocated: UserEntity? = null,
    premises: Cas3PremisesEntity? = null,
    referralRejectionReason: String? = null,
    referralRejectionReasonDetail: String? = null,
  ): TemporaryAccommodationAssessmentEntity {
    val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withCrn(crn)
      withCreatedByUser(user)
      withProbationRegion(user.probationRegion)
      withSubmittedAt(OffsetDateTime.now())
    }
    val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
      withDecision(decision)
      withCompletedAt(completedAt)
      withAllocatedToUser(allocated)
      if (referralRejectionReason != null) withReferralRejectionReason(referralRejectionReasonEntityFactory.produceAndPersist { withName(referralRejectionReason) })
      if (referralRejectionReasonDetail != null) withReferralRejectionReasonDetail(referralRejectionReasonDetail)
    }
    if (premises != null) {
      val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
        withPremises(premises)
      }
      cas3BookingEntityFactory.produceAndPersist {
        withCrn(crn)
        withApplication(application)
        withPremises(premises)
        withBedspace(bedspace)
        withStatus(Cas3BookingStatus.provisional)
        withServiceName(ServiceName.temporaryAccommodation)
      }
    }
    return assessment
  }
}
