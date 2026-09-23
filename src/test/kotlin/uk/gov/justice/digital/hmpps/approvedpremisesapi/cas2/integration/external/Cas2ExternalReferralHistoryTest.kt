package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ApplicationStatusSeeding
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ReferralHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2Cohort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2v2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASingleAccommodationServiceClientCredentialsApiCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.OffsetDateTime

class Cas2ExternalReferralHistoryTest : IntegrationTestBase() {
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
          val now = OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres()
          val application1 = createApplication(user, "Referral cancelled", referringPrisonCode = omu.prisonCode, createdAt = now.minusDays(1))
          val application2 = createApplication(user, "Referral cancelled", referringPrisonCode = omu.prisonCode, createdAt = now.minusDays(2))
          val application3 = createApplication(user, "Referral cancelled", referringPrisonCode = omu.prisonCode, createdAt = now.minusDays(3))
          val application4 = createApplication(user, "Referral withdrawn", referringPrisonCode = omu.prisonCode, createdAt = now.minusDays(4))
          val application5 = createApplication(user, "Referral withdrawn", referringPrisonCode = omu.prisonCode, createdAt = now.minusDays(5))

          val expectedReferrals = listOf(
            Cas2ReferralHistory(
              type = type,
              id = application1.assessment!!.id,
              applicationId = application1.id,
              applicationStatus = Cas2AssessmentStatus.CANCELLED,
              applicationSubmittedDate = application1.submittedAt!!.toLocalDate(),
              applicationLastUpdatedDate = application1.statusUpdates!!.first().createdAt.toLocalDate(),
              referralRejectionReason = "cancelled",
              localAuthorityArea = omu.prisonName,
              pdu = application1.preferredAreas,
              referredBy = application1.createdByUser.name,
              placementAddress = omu.prisonName,
              uiUrl = "http://localhost:3000/assess/applications/${application1.id}/overview",
            ),
            Cas2ReferralHistory(
              type = type,
              id = application2.assessment!!.id,
              applicationId = application2.id,
              applicationStatus = Cas2AssessmentStatus.CANCELLED,
              applicationSubmittedDate = application2.submittedAt!!.toLocalDate(),
              applicationLastUpdatedDate = application2.statusUpdates!!.first().createdAt.toLocalDate(),
              referralRejectionReason = "cancelled",
              localAuthorityArea = omu.prisonName,
              pdu = application2.preferredAreas,
              referredBy = application2.createdByUser.name,
              placementAddress = omu.prisonName,
              uiUrl = "http://localhost:3000/assess/applications/${application2.id}/overview",
            ),
            Cas2ReferralHistory(
              type = type,
              id = application3.assessment!!.id,
              applicationId = application3.id,
              applicationStatus = Cas2AssessmentStatus.CANCELLED,
              applicationSubmittedDate = application3.submittedAt!!.toLocalDate(),
              applicationLastUpdatedDate = application3.statusUpdates!!.first().createdAt.toLocalDate(),
              referralRejectionReason = "cancelled",
              localAuthorityArea = omu.prisonName,
              pdu = application3.preferredAreas,
              referredBy = application3.createdByUser.name,
              placementAddress = omu.prisonName,
              uiUrl = "http://localhost:3000/assess/applications/${application3.id}/overview",
            ),
            Cas2ReferralHistory(
              type = type,
              id = application4.assessment!!.id,
              applicationId = application4.id,
              applicationStatus = Cas2AssessmentStatus.WITHDRAWN,
              applicationSubmittedDate = application4.submittedAt!!.toLocalDate(),
              applicationLastUpdatedDate = application4.statusUpdates!!.first().createdAt.toLocalDate(),
              referralRejectionReason = "withdrawn",
              localAuthorityArea = omu.prisonName,
              pdu = application4.preferredAreas,
              referredBy = application4.createdByUser.name,
              placementAddress = omu.prisonName,
              uiUrl = "http://localhost:3000/assess/applications/${application4.id}/overview",
            ),
            Cas2ReferralHistory(
              type = type,
              id = application5.assessment!!.id,
              applicationId = application5.id,
              applicationStatus = Cas2AssessmentStatus.WITHDRAWN,
              applicationSubmittedDate = application5.submittedAt!!.toLocalDate(),
              applicationLastUpdatedDate = application5.statusUpdates!!.first().createdAt.toLocalDate(),
              referralRejectionReason = "withdrawn",
              localAuthorityArea = omu.prisonName,
              pdu = application4.preferredAreas,
              referredBy = application5.createdByUser.name,
              placementAddress = omu.prisonName,
              uiUrl = "http://localhost:3000/assess/applications/${application5.id}/overview",
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
            .expectBodyList(Cas2ReferralHistory::class.java)
            .returnResult()
            .responseBody

          val matched = response!!.first { it.id == withdrawnApplication.assessment!!.id }
          assertThat(matched.referralRejectionReason).isEqualTo("withdrawn")
          assertThat(matched.applicationStatus).isEqualTo(Cas2AssessmentStatus.WITHDRAWN)
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
          val applicationWithPrison = createApplication(user, "Awaiting decision", referringPrisonCode = omu.prisonCode)

          val response = webTestClient.get()
            .uri("/cas2/external/referrals/$crn")
            .header("Authorization", "Bearer $clientCredentialsJwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList(Cas2ReferralHistory::class.java)
            .returnResult()
            .responseBody

          val matched = response!!.first { it.id == applicationWithPrison.assessment!!.id }
          assertThat(matched.placementAddress).isEqualTo(omu.prisonName)
        }
      }
    }

    @Test
    fun `Get referrals returns only applications in ISR cohorts`() {
      givenACas2v2PomUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          val omu = offenderManagementUnitEntityFactory.produceAndPersist {
            withPrisonCode("TST")
            withPrisonName("HMP Test Prison")
          }
          val isrApplication = createApplication(user, "Awaiting decision", referringPrisonCode = omu.prisonCode, cohort = Cas2Cohort.ATCR)
          val nonIsrApplication = createApplication(user, "Awaiting decision", referringPrisonCode = omu.prisonCode, cohort = Cas2Cohort.COURT_BAIL)

          val response = webTestClient.get()
            .uri("/cas2/external/referrals/$crn")
            .header("Authorization", "Bearer $clientCredentialsJwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList(Cas2ReferralHistory::class.java)
            .returnResult()
            .responseBody

          assertThat(response!!.map { it.id }).contains(isrApplication.assessment!!.id)
          assertThat(response.map { it.id }).doesNotContain(nonIsrApplication.assessment!!.id)
        }
      }
    }
  }

  private fun createApplication(
    user: Cas2UserEntity,
    label: String,
    preferredAreas: String? = null,
    referringPrisonCode: String? = null,
    cohort: Cas2Cohort? = Cas2Cohort.ATCR,
    createdAt: OffsetDateTime = OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
  ): Cas2ApplicationEntity {
    val actualStatusId = Cas2ApplicationStatusSeeding.statusList(ServiceName.cas2v2).first { it.label == label }.id
    val statusApplication = cas2ApplicationEntityFactory.produceAndPersist {
      withCreatedAt(createdAt)
      withSubmittedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
      withCreatedByUser(user)
    }
    val application = cas2ApplicationEntityFactory.produceAndPersist {
      withId(statusApplication.id)
      withCreatedByUser(user)
      withCrn(crn)
      withCreatedAt(createdAt)
      withSubmittedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
      withCohort(cohort)
      preferredAreas?.let { withPreferredAreas(it) }
      referringPrisonCode?.let { withReferringPrisonCode(it) }
      withStatusUpdates(
        mutableListOf(
          cas2StatusUpdateEntityFactory.produceAndPersist {
            withStatusId(actualStatusId)
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
