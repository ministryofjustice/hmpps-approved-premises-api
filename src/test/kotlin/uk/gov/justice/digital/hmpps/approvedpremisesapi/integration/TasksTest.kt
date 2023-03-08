package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.TaskTransformer
import java.time.OffsetDateTime

class TasksTest : IntegrationTestBase() {
  @Autowired
  lateinit var taskTransformer: TaskTransformer

  @Test
  fun `Get all tasks without JWT returns 401`() {
    webTestClient.get()
      .uri("/tasks")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get all tasks returns 200 with correct body`() {
    `Given a User` { user, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
          withAddedAt(OffsetDateTime.now())
        }

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(user)
          withApplicationSchema(applicationSchema)
        }

        val assessment = assessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(user)
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
        }

        assessment.schemaUpToDate = true

        val reallocatedAssessment = assessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(user)
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
          withReallocatedAt(OffsetDateTime.now())
        }

        reallocatedAssessment.schemaUpToDate = true

        webTestClient.get()
          .uri("/tasks")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              listOf(
                taskTransformer.transformAssessmentToTask(assessment, offenderDetails, inmateDetails)
              )
            )
          )
      }
    }
  }
}
