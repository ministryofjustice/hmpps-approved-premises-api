package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.integration.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ReferralHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2v2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASingleAccommodationServiceClientCredentialsApiCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.OffsetDateTime

class Cas2v2ExternalReferralHistoryTest : IntegrationTestBase() {
  private val crn = "ABC1234"

  @Nested
  inner class GetReferralsByCrn {

    @Test
    fun `Get all referrals when user JWT returns 403 Forbidden`() {
      givenACas2v2PomUser { _, jwt ->
        webTestClient.get()
          .uri("/cas2/external/referrals/$crn")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get all referrals when no JWT returns 401 Unauthorized`() {
      webTestClient.get()
        .uri("/cas2/external/referrals/$crn")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get all referrals returns ok`() {
      givenACas2v2PomUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          val type = ServiceType.CAS2v2
          val omu = offenderManagementUnitEntityFactory.produceAndPersist {
            withPrisonCode("TST")
            withPrisonName("HMP Test Prison")
          }
          val application1 = createApplication(user, "Referral cancelled", referringPrisonCode = omu.prisonCode)
          val application2 = createApplication(user, "Referral cancelled", referringPrisonCode = omu.prisonCode)
          val application3 = createApplication(user, "Referral cancelled", referringPrisonCode = omu.prisonCode)
          val application4 = createApplication(user, "Referral withdrawn", referringPrisonCode = omu.prisonCode)
          val application5 = createApplication(user, "Referral withdrawn", referringPrisonCode = omu.prisonCode)

          val expectedReferrals = listOf(
            Cas2v2ReferralHistory(
              id = application1.assessment!!.id,
              applicationId = application1.id,
              createdAt = application1.submittedAt!!.toInstant(),
              status = application1.statusUpdates!!.first().label,
              type = type,
              referralRejectionReason = "Referral cancelled",
              localAuthorityArea = omu.prisonName,
              pdu = application1.preferredAreas,
              referredBy = application1.createdByUser.name,
              placementAddress = omu.prisonName,
              placementStatus = application1.statusUpdates!!.first().label,
            ),
            Cas2v2ReferralHistory(
              id = application2.assessment!!.id,
              applicationId = application2.id,
              createdAt = application2.submittedAt!!.toInstant(),
              status = application2.statusUpdates!!.first().label,
              type = type,
              referralRejectionReason = "Referral cancelled",
              localAuthorityArea = omu.prisonName,
              pdu = application2.preferredAreas,
              referredBy = application2.createdByUser.name,
              placementAddress = omu.prisonName,
              placementStatus = application2.statusUpdates!!.first().label,
            ),
            Cas2v2ReferralHistory(
              id = application3.assessment!!.id,
              applicationId = application3.id,
              createdAt = application3.submittedAt!!.toInstant(),
              status = application3.statusUpdates!!.first().label,
              type = type,
              referralRejectionReason = "Referral cancelled",
              localAuthorityArea = omu.prisonName,
              pdu = application3.preferredAreas,
              referredBy = application3.createdByUser.name,
              placementAddress = omu.prisonName,
              placementStatus = application3.statusUpdates!!.first().label,
            ),
            Cas2v2ReferralHistory(
              id = application4.assessment!!.id,
              applicationId = application4.id,
              createdAt = application4.submittedAt!!.toInstant(),
              status = application4.statusUpdates!!.first().label,
              type = type,
              referralRejectionReason = "Referral withdrawn",
              localAuthorityArea = omu.prisonName,
              pdu = application4.preferredAreas,
              referredBy = application4.createdByUser.name,
              placementAddress = omu.prisonName,
              placementStatus = application4.statusUpdates!!.first().label,
            ),
            Cas2v2ReferralHistory(
              id = application5.assessment!!.id,
              applicationId = application5.id,
              createdAt = application5.submittedAt!!.toInstant(),
              status = application5.statusUpdates!!.first().label,
              type = type,
              referralRejectionReason = "Referral withdrawn",
              localAuthorityArea = omu.prisonName,
              pdu = application5.preferredAreas,
              referredBy = application5.createdByUser.name,
              placementAddress = omu.prisonName,
              placementStatus = application5.statusUpdates!!.first().label,
            ),
          )

          val response = webTestClient.get()
            .uri("/cas2/external/referrals/$crn")
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

    @Test
    fun `Get referral returns enriched fields`() {
      givenACas2v2PomUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          val omu = offenderManagementUnitEntityFactory.produceAndPersist {
            withPrisonCode("TST")
            withPrisonName("HMP Test Prison")
          }
          val withdrawnApplication = createApplication(user, "Referral withdrawn", preferredAreas = "South East", referringPrisonCode = omu.prisonCode)

          val response = webTestClient.get()
            .uri("/cas2/external/referrals/$crn")
            .header("Authorization", "Bearer $clientCredentialsJwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList(Cas2v2ReferralHistory::class.java)
            .returnResult()
            .responseBody

          val matched = response!!.first { it.id == withdrawnApplication.assessment!!.id }
          assertThat(matched.referralRejectionReason).isEqualTo("Referral withdrawn")
          assertThat(matched.placementStatus).isEqualTo("Referral withdrawn")
          assertThat(matched.pdu).isEqualTo("South East")
          assertThat(matched.referredBy).isEqualTo(user.name)
          assertThat(matched.localAuthorityArea).isEqualTo(omu.prisonName)
          assertThat(matched.placementAddress).isEqualTo(omu.prisonName)
        }
      }
    }

    @Test
    fun `Get referral returns placement address from OMU prison name`() {
      givenACas2v2PomUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          val omu = offenderManagementUnitEntityFactory.produceAndPersist {
            withPrisonCode("TST")
            withPrisonName("HMP Test Prison")
          }
          val applicationWithPrison = createApplication(user, "Active", referringPrisonCode = omu.prisonCode)

          val response = webTestClient.get()
            .uri("/cas2/external/referrals/$crn")
            .header("Authorization", "Bearer $clientCredentialsJwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList(Cas2v2ReferralHistory::class.java)
            .returnResult()
            .responseBody

          val matched = response!!.first { it.id == applicationWithPrison.assessment!!.id }
          assertThat(matched.placementAddress).isEqualTo(omu.prisonName)
        }
      }
    }
  }

  private fun createApplication(
    user: Cas2UserEntity,
    label: String,
    preferredAreas: String? = null,
    referringPrisonCode: String? = null,
  ): Cas2ApplicationEntity {
    val statusApplication = cas2ApplicationEntityFactory.produceAndPersist {
      withSubmittedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
      withCreatedByUser(user)
    }
    val application = cas2ApplicationEntityFactory.produceAndPersist {
      withId(statusApplication.id)
      withCreatedByUser(user)
      withCrn(crn)
      withSubmittedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
      preferredAreas?.let { withPreferredAreas(it) }
      referringPrisonCode?.let { withReferringPrisonCode(it) }
      withStatusUpdates(
        mutableListOf(
          cas2StatusUpdateEntityFactory.produceAndPersist {
            withLabel(label)
            withApplication(statusApplication)
            withAssessor(cas2UserEntityFactory.produceAndPersist { withUserType(Cas2UserType.EXTERNAL) })
          },
        ),
      )
      withAssessment(
        cas2AssessmentEntityFactory.produceAndPersist {
          withApplication(statusApplication)
        },
      )
    }
    return application
  }
}
