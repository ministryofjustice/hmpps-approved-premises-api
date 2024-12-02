package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentRejection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationAssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class AssessmentStateTest : IntegrationTestBase() {
  @Test
  fun `A Temporary Accommodation assessment can transition between all assessment states correctly`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
          withAddedAt(OffsetDateTime.now())
        }

        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
          withApplicationSchema(applicationSchema)
          withProbationRegion(userEntity.probationRegion)
          withData(
            """
              {}
            """,
          )
        }

        val assessment = application.submitAndGetAssessment(jwt)
        assessment.assertApiStatus(TemporaryAccommodationAssessmentStatus.UNALLOCATED, jwt)

        // Happy path: unallocated -> in_review -> ready_to_place -> closed
        assessment.allocateToCurrentUser(jwt)
        assessment.assertApiStatus(TemporaryAccommodationAssessmentStatus.IN_REVIEW, jwt)

        assessment.markReadyToPlace(jwt)
        assessment.assertApiStatus(TemporaryAccommodationAssessmentStatus.READY_TO_PLACE, jwt)

        assessment.markClosed(jwt)
        assessment.assertApiStatus(TemporaryAccommodationAssessmentStatus.CLOSED, jwt)

        // Backwards transitions: closed -> ready_to_place -> in_review -> unallocated
        assessment.markReadyToPlace(jwt)
        assessment.assertApiStatus(TemporaryAccommodationAssessmentStatus.READY_TO_PLACE, jwt)

        assessment.allocateToCurrentUser(jwt)
        assessment.assertApiStatus(TemporaryAccommodationAssessmentStatus.IN_REVIEW, jwt)

        assessment.deallocateUser(jwt)
        assessment.assertApiStatus(TemporaryAccommodationAssessmentStatus.UNALLOCATED, jwt)

        // Unhappy path: unallocated -> in_review -> ready_to_place -> rejected
        assessment.allocateToCurrentUser(jwt)
        assessment.assertApiStatus(TemporaryAccommodationAssessmentStatus.IN_REVIEW, jwt)

        assessment.markReadyToPlace(jwt)
        assessment.assertApiStatus(TemporaryAccommodationAssessmentStatus.READY_TO_PLACE, jwt)

        assessment.reject(jwt)
        assessment.assertApiStatus(TemporaryAccommodationAssessmentStatus.REJECTED, jwt)

        // Backwards transitions: rejected -> ready_to_place -> in_review -> unallocated
        assessment.markReadyToPlace(jwt)
        assessment.assertApiStatus(TemporaryAccommodationAssessmentStatus.READY_TO_PLACE, jwt)

        assessment.allocateToCurrentUser(jwt)
        assessment.assertApiStatus(TemporaryAccommodationAssessmentStatus.IN_REVIEW, jwt)

        assessment.deallocateUser(jwt)
        assessment.assertApiStatus(TemporaryAccommodationAssessmentStatus.UNALLOCATED, jwt)
      }
    }
  }

  private fun TemporaryAccommodationApplicationEntity.submitAndGetAssessment(jwt: String): TemporaryAccommodationAssessmentEntity {
    webTestClient.post()
      .uri("/applications/${this.id}/submission")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .bodyValue(
        SubmitTemporaryAccommodationApplication(
          arrivalDate = LocalDate.now().plusDays(14),
          summaryData = {},
          type = "CAS3",
          translatedDocument = {},
        ),
      )
      .exchange()
      .expectStatus()
      .is2xxSuccessful

    val persistedApplication = temporaryAccommodationApplicationRepository.findByIdOrNull(this.id)!!
    return persistedApplication.assessments.first() as TemporaryAccommodationAssessmentEntity
  }

  private fun TemporaryAccommodationAssessmentEntity.assertApiStatus(status: TemporaryAccommodationAssessmentStatus, jwt: String) {
    webTestClient.get()
      .uri("/assessments/${this.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.status").isEqualTo(status.value)
  }

  private fun TemporaryAccommodationAssessmentEntity.allocateToCurrentUser(jwt: String) {
    webTestClient.post()
      .uri("tasks/assessment/${this.id}/allocations")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .bodyValue({ })
      .exchange()
      .expectStatus()
      .is2xxSuccessful
  }

  private fun TemporaryAccommodationAssessmentEntity.markReadyToPlace(jwt: String) {
    webTestClient.post()
      .uri("/assessments/${this.id}/acceptance")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .bodyValue(AssessmentAcceptance(document = {}))
      .exchange()
      .expectStatus()
      .is2xxSuccessful
  }

  private fun TemporaryAccommodationAssessmentEntity.markClosed(jwt: String) {
    webTestClient.post()
      .uri("/assessments/${this.id}/closure")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .exchange()
      .expectStatus()
      .is2xxSuccessful
  }

  private fun TemporaryAccommodationAssessmentEntity.deallocateUser(jwt: String) {
    webTestClient.delete()
      .uri("tasks/assessment/${this.id}/allocations")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .exchange()
      .expectStatus()
      .is2xxSuccessful
  }

  private fun TemporaryAccommodationAssessmentEntity.reject(jwt: String) {
    val referralRejectionReasonId = UUID.randomUUID()

    referralRejectionReasonEntityFactory
      .produceAndPersist {
        withId(referralRejectionReasonId)
        withIsActive(true)
      }

    webTestClient.post()
      .uri("/assessments/${this.id}/rejection")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .bodyValue(AssessmentRejection(document = {}, rejectionRationale = "Some reason or another", referralRejectionReasonId, null, true))
      .exchange()
      .expectStatus()
      .is2xxSuccessful
  }
}
