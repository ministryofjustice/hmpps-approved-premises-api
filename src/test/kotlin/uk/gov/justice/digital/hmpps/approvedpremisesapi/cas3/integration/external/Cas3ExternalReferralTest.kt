package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.Cas3IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ReferralHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationAssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASingleAccommodationServiceClientCredentialsApiCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
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
          val assessment1 = createAssessment(user, AssessmentDecision.ACCEPTED)
          val assessment2 = createAssessment(user, AssessmentDecision.REJECTED)
          val assessment3 = createAssessment(user, AssessmentDecision.ACCEPTED, OffsetDateTime.now())
          val assessment4 = createAssessment(user, null, null, user)
          val assessment5 = createAssessment(user)

          val expectedReferrals = listOf(
            Cas3ReferralHistory(
              id = assessment1.id,
              applicationId = assessment1.application.id,
              createdAt = assessment1.createdAt.toInstant(),
              status = TemporaryAccommodationAssessmentStatus.readyToPlace,
              type = ServiceType.CAS3,
            ),
            Cas3ReferralHistory(
              id = assessment2.id,
              applicationId = assessment2.application.id,
              createdAt = assessment2.createdAt.toInstant(),
              status = TemporaryAccommodationAssessmentStatus.rejected,
              type = ServiceType.CAS3,
            ),
            Cas3ReferralHistory(
              id = assessment3.id,
              applicationId = assessment3.application.id,
              createdAt = assessment3.createdAt.toInstant(),
              status = TemporaryAccommodationAssessmentStatus.closed,
              type = ServiceType.CAS3,
            ),
            Cas3ReferralHistory(
              id = assessment4.id,
              applicationId = assessment4.application.id,
              createdAt = assessment4.createdAt.toInstant(),
              status = TemporaryAccommodationAssessmentStatus.inReview,
              type = ServiceType.CAS3,
            ),
            Cas3ReferralHistory(
              id = assessment5.id,
              applicationId = assessment5.application.id,
              createdAt = assessment5.createdAt.toInstant(),
              status = TemporaryAccommodationAssessmentStatus.unallocated,
              type = ServiceType.CAS3,
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

          assertThat(responseBody).isEqualTo(objectMapper.writeValueAsString(expectedReferrals))
        }
      }
    }
  }

  private fun createAssessment(
    user: UserEntity,
    decision: AssessmentDecision? = null,
    completedAt: OffsetDateTime? = null,
    allocated: UserEntity? = null,
  ): TemporaryAccommodationAssessmentEntity {
    val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withCrn(crn)
      withCreatedByUser(user)
      withProbationRegion(user.probationRegion)
      withSubmittedAt(OffsetDateTime.now())
    }
    return temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
      withDecision(decision)
      withCompletedAt(completedAt)
      withAllocatedToUser(allocated)
    }
  }
}
