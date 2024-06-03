package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole

class ReallocationAtomicTest : IntegrationTestBase() {
  @SpykBean
  lateinit var realAssessmentRepository: AssessmentRepository

  @Test
  fun `Database exception after setting reallocated on original Assessment results in that change being rolled back`() {
    givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { requestUser, jwt ->
      givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR)) { otherUser, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR)) { assigneeUser, _ ->
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist()
          val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist()

          val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(otherUser)
            withApplicationSchema(applicationSchema)
          }

          val existingAssessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withAllocatedToUser(otherUser)
            withAssessmentSchema(assessmentSchema)
          }

          every { realAssessmentRepository.save(match { it.id != existingAssessment.id }) } throws RuntimeException("I am a database error")

          webTestClient.post()
            .uri("/tasks/assessment/${existingAssessment.id}/allocations")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .bodyValue(
              NewReallocation(
                userId = assigneeUser.id,
              ),
            )
            .exchange()
            .expectStatus()
            .is5xxServerError

          val assessments = approvedPremisesAssessmentRepository.findAll()

          Assertions.assertThat(assessments.first { it.id == existingAssessment.id }.reallocatedAt).isNull()
        }
      }
    }
  }
}
