package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Assessment`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.TaskTransformer

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
        `Given an Assessment`(
          allocatedToUser = user,
          createdByUser = user,
          crn = offenderDetails.otherIds.crn
        ) { assessment, _ ->
          `Given an Assessment`(
            allocatedToUser = user,
            createdByUser = user,
            crn = offenderDetails.otherIds.crn,
            reallocated = true
          ) { _, _ ->
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
  }

  @Test
  fun `Get a Task for an application without JWT returns 401`() {
    webTestClient.get()
      .uri("/applications/f601ff2d-b1e0-4878-8731-ccfa19a2ce84/tasks/assessment")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get an unknown task type for an application returns 404`() {
    `Given a User`(roles = listOf(UserRole.WORKFLOW_MANAGER)) { user, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        `Given an Assessment`(
          allocatedToUser = user,
          createdByUser = user,
          crn = offenderDetails.otherIds.crn
        ) { _, application ->
          webTestClient.get()
            .uri("/applications/${application.id}/tasks/unknown-task")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isNotFound
        }
      }
    }
  }

  @Test
  fun `Get an assessment task for an application returns 200 with correct body`() {
    `Given a User`(roles = listOf(UserRole.WORKFLOW_MANAGER)) { _, jwt ->
      `Given a User` { user, _ ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          `Given an Assessment`(
            allocatedToUser = user,
            createdByUser = user,
            crn = offenderDetails.otherIds.crn
          ) { assessment, application ->
            webTestClient.get()
              .uri("/applications/${application.id}/tasks/assessment")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  taskTransformer.transformAssessmentToTask(assessment, offenderDetails, inmateDetails)
                )
              )
          }
        }
      }
    }
  }

  @Test
  fun `Get an non-implemented task type for an application returns 404`() {
    `Given a User`(roles = listOf(UserRole.WORKFLOW_MANAGER)) { user, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        `Given an Assessment`(
          allocatedToUser = user,
          createdByUser = user,
          crn = offenderDetails.otherIds.crn
        ) { _, application ->
          webTestClient.get()
            .uri("/applications/${application.id}/tasks/placement-request")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
        }
      }
    }
  }
}
