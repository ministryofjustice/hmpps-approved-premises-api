package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ReferralHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASingleAccommodationServiceClientCredentialsApiCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.OffsetDateTime

class Cas2ExternalReferralHistoryTest : IntegrationTestBase() {
  private val crn = "ABC1234"

  @Nested
  inner class GetReferralsByCrn {

    @Test
    fun `Get all referrals when user JWT returns 403 Forbidden`() {
      givenACas2PomUser { _, jwt ->
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
      givenACas2PomUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          val type = "CAS2"
          val application1 = createApplication(user, "Status 1")
          val application2 = createApplication(user, "Status 2")
          val application3 = createApplication(user, "Status 3")
          val application4 = createApplication(user, "Status 4")
          val application5 = createApplication(user, "Status 5")

          val expectedReferrals = listOf(
            Cas2ReferralHistory(
              id = application1.assessment!!.id,
              applicationId = application1.id,
              createdAt = application1.submittedAt!!,
              status = application1.statusUpdates!!.first().label,
              type = type,
            ),
            Cas2ReferralHistory(
              id = application2.assessment!!.id,
              applicationId = application2.id,
              createdAt = application2.submittedAt!!,
              status = application2.statusUpdates!!.first().label,
              type = type,
            ),
            Cas2ReferralHistory(
              id = application3.assessment!!.id,
              applicationId = application3.id,
              createdAt = application3.submittedAt!!,
              status = application3.statusUpdates!!.first().label,
              type = type,
            ),
            Cas2ReferralHistory(
              id = application4.assessment!!.id,
              applicationId = application4.id,
              createdAt = application4.submittedAt!!,
              status = application4.statusUpdates!!.first().label,
              type = type,
            ),
            Cas2ReferralHistory(
              id = application5.assessment!!.id,
              applicationId = application5.id,
              createdAt = application5.submittedAt!!,
              status = application5.statusUpdates!!.first().label,
              type = type,
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

          assertThat(responseBody).isEqualTo(objectMapper.writeValueAsString(expectedReferrals))
        }
      }
    }
  }

  private fun createApplication(
    user: NomisUserEntity,
    label: String,
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
      withStatusUpdates(
        mutableListOf(
          cas2StatusUpdateEntityFactory.produceAndPersist {
            withLabel(label)
            withApplication(statusApplication)
            withAssessor(externalUserEntityFactory.produceAndPersist())
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
