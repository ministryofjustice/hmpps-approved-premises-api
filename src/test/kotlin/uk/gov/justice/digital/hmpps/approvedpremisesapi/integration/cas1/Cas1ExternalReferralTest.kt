package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ReferralHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASingleAccommodationServiceClientCredentialsApiCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.OffsetDateTime

class Cas1ExternalReferralTest : IntegrationTestBase() {
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
          givenAnOffender { _, _ ->
            val assessment1 = createCas1Assessment(crn, user, AssessmentDecision.ACCEPTED)
            val assessment2 = createCas1Assessment(crn, user, AssessmentDecision.REJECTED)
            val assessment3 = createCas1Assessment(crn, user, AssessmentDecision.ACCEPTED)
            val assessment4 = createCas1Assessment(crn, user, null, user)
            val assessment5 = createCas1Assessment(crn, user)

            val expectedReferrals = listOf(
              Cas1ReferralHistory(
                id = assessment1.id,
                applicationId = assessment1.application.id,
                createdAt = assessment1.createdAt.toInstant(),
                status = Cas1AssessmentStatus.completed,
                type = ServiceType.CAS1,
              ),
              Cas1ReferralHistory(
                id = assessment2.id,
                applicationId = assessment2.application.id,
                createdAt = assessment2.createdAt.toInstant(),
                status = Cas1AssessmentStatus.completed,
                type = ServiceType.CAS1,
              ),
              Cas1ReferralHistory(
                id = assessment3.id,
                applicationId = assessment3.application.id,
                createdAt = assessment3.createdAt.toInstant(),
                status = Cas1AssessmentStatus.completed,
                type = ServiceType.CAS1,
              ),
              Cas1ReferralHistory(
                id = assessment4.id,
                applicationId = assessment4.application.id,
                createdAt = assessment4.createdAt.toInstant(),
                status = Cas1AssessmentStatus.inProgress,
                type = ServiceType.CAS1,
              ),
              Cas1ReferralHistory(
                id = assessment5.id,
                applicationId = assessment5.application.id,
                createdAt = assessment5.createdAt.toInstant(),
                status = Cas1AssessmentStatus.inProgress,
                type = ServiceType.CAS1,
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

            assertThat(response).containsExactlyInAnyOrderElementsOf(expectedReferrals)
          }
        }
      }
    }
  }

  private fun createCas1Assessment(
    crn: String,
    user: UserEntity,
    decision: AssessmentDecision? = null,
    allocated: UserEntity? = null,
  ): ApprovedPremisesAssessmentEntity {
    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCrn(crn)
      withCreatedByUser(user)
    }
    return approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withAllocatedToUser(allocated ?: user)
      withDecision(decision)
      withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
    }
  }
}
