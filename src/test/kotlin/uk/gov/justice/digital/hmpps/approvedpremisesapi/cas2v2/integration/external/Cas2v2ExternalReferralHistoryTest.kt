package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.integration.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ReferralHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.integration.Cas2v2IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2v2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASingleAccommodationServiceClientCredentialsApiCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.OffsetDateTime

class Cas2v2ExternalReferralHistoryTest : Cas2v2IntegrationTestBase() {
  private val crn = "ABC1234"

  @Nested
  inner class GetReferralsByCrn {

    @Test
    fun `Get all referrals when user JWT returns 403 Forbidden`() {
      givenACas2v2PomUser { _, jwt ->
        webTestClient.get()
          .uri("/cas2v2/external/referrals/$crn")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get all referrals when no JWT returns 401 Unauthorized`() {
      webTestClient.get()
        .uri("/cas2v2/external/referrals/$crn")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get all referrals returns ok`() {
      givenACas2v2PomUser { user, _ ->
        givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
          val type = ServiceType.CAS2v2
          val application1 = createApplication(user, "Status 1")
          val application2 = createApplication(user, "Status 2")
          val application3 = createApplication(user, "Status 3")
          val application4 = createApplication(user, "Status 4")
          val application5 = createApplication(user, "Status 5")

          val expectedReferrals = listOf(
            Cas2v2ReferralHistory(
              id = application1.assessment!!.id,
              applicationId = application1.id,
              createdAt = application1.submittedAt!!.toInstant(),
              status = application1.statusUpdates!!.first().label,
              type = type,
            ),
            Cas2v2ReferralHistory(
              id = application2.assessment!!.id,
              applicationId = application2.id,
              createdAt = application2.submittedAt!!.toInstant(),
              status = application2.statusUpdates!!.first().label,
              type = type,
            ),
            Cas2v2ReferralHistory(
              id = application3.assessment!!.id,
              applicationId = application3.id,
              createdAt = application3.submittedAt!!.toInstant(),
              status = application3.statusUpdates!!.first().label,
              type = type,
            ),
            Cas2v2ReferralHistory(
              id = application4.assessment!!.id,
              applicationId = application4.id,
              createdAt = application4.submittedAt!!.toInstant(),
              status = application4.statusUpdates!!.first().label,
              type = type,
            ),
            Cas2v2ReferralHistory(
              id = application5.assessment!!.id,
              applicationId = application5.id,
              createdAt = application5.submittedAt!!.toInstant(),
              status = application5.statusUpdates!!.first().label,
              type = type,
            ),
          )

          val response = webTestClient.get()
            .uri("/cas2v2/external/referrals/$crn")
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
    user: Cas2v2UserEntity,
    label: String,
  ): Cas2v2ApplicationEntity {
    val statusApplication = cas2v2ApplicationEntityFactory.produceAndPersist {
      withSubmittedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
      withCreatedByUser(user)
    }
    val application = cas2v2ApplicationEntityFactory.produceAndPersist {
      withId(statusApplication.id)
      withCreatedByUser(user)
      withCrn(crn)
      withSubmittedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
      withStatusUpdates(
        mutableListOf(
          cas2v2StatusUpdateEntityFactory.produceAndPersist {
            withLabel(label)
            withApplication(statusApplication)
            withAssessor(cas2v2UserEntityFactory.produceAndPersist { withUserType(Cas2v2UserType.EXTERNAL) })
          },
        ),
      )
      withAssessment(
        cas2v2AssessmentEntityFactory.produceAndPersist {
          withApplication(statusApplication)
        },
      )
    }
    return application
  }
}
