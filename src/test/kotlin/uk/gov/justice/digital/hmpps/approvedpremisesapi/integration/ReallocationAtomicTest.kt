package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole

class ReallocationAtomicTest : IntegrationTestBase() {
  @SpykBean
  lateinit var realAssessmentRepository: AssessmentRepository

  @Test
  fun `Database exception after setting reallocated on original Assessment results in that change being rolled back`() {
    `Given a User`(roles = listOf(UserRole.WORKFLOW_MANAGER)) { requestUser, jwt ->
      `Given a User`(roles = listOf(UserRole.ASSESSOR)) { otherUser, _ ->
        `Given a User`(roles = listOf(UserRole.ASSESSOR)) { assigneeUser, _ ->
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist()
          val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist()

          val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(otherUser)
            withApplicationSchema(applicationSchema)
          }

          val existingAssessment = assessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withAllocatedToUser(otherUser)
            withAssessmentSchema(assessmentSchema)
          }

          every { realAssessmentRepository.save(match { it.id != existingAssessment.id }) } throws RuntimeException("I am a database error")

          webTestClient.post()
            .uri("/applications/${application.id}/allocations")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Reallocation(
                userId = assigneeUser.id
              )
            )
            .exchange()
            .expectStatus()
            .is5xxServerError

          val assessments = assessmentRepository.findAll()

          Assertions.assertThat(assessments.first { it.id == existingAssessment.id }.reallocatedAt).isNull()
        }
      }
    }
  }
}
