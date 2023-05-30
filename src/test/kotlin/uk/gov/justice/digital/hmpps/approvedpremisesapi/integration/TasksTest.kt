package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskWrapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Placement Request`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Application`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Assessment for Approved Premises`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.TaskTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import java.util.UUID

class TasksTest : IntegrationTestBase() {
  @Autowired
  lateinit var taskTransformer: TaskTransformer

  @Autowired
  lateinit var userTransformer: UserTransformer

  @Test
  fun `Get all tasks without JWT returns 401`() {
    webTestClient.get()
      .uri("/tasks")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get all tasks without workflow manager permissions returns 403`() {
    `Given a User` { _, jwt ->
      webTestClient.get()
        .uri("/tasks")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get all tasks returns 200 with correct body`() {
    `Given a User`(roles = listOf(UserRole.WORKFLOW_MANAGER)) { user, jwt ->
      `Given a User` { otherUser, _ ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          `Given an Assessment for Approved Premises`(
            allocatedToUser = otherUser,
            createdByUser = otherUser,
            crn = offenderDetails.otherIds.crn,
          ) { assessment, _ ->
            `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              reallocated = true,
            ) { _, _ ->
              `Given a Placement Request`(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              ) { placementRequest, _ ->
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
                        taskTransformer.transformAssessmentToTask(assessment, offenderDetails, inmateDetails),
                        taskTransformer.transformPlacementRequestToTask(
                          placementRequest,
                          offenderDetails,
                          inmateDetails,
                        ),
                      ),
                    ),
                  )
              }
            }
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
        `Given an Assessment for Approved Premises`(
          allocatedToUser = user,
          createdByUser = user,
          crn = offenderDetails.otherIds.crn,
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
        `Given a User`(
          roles = listOf(UserRole.ASSESSOR),
        ) { allocatableUser, _ ->
          `Given an Offender` { offenderDetails, inmateDetails ->
            `Given an Assessment for Approved Premises`(
              allocatedToUser = user,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
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
                    TaskWrapper(
                      task = taskTransformer.transformAssessmentToTask(assessment, offenderDetails, inmateDetails),
                      users = listOf(userTransformer.transformJpaToApi(allocatableUser, ServiceName.approvedPremises)),
                    ),
                  ),
                )
            }
          }
        }
      }
    }
  }

  @Test
  fun `Get a Placement Request Task for an application returns 200`() {
    `Given a User`(roles = listOf(UserRole.WORKFLOW_MANAGER)) { _, jwt ->
      `Given a User` { user, _ ->
        `Given a User`(
          roles = listOf(UserRole.MATCHER),
        ) { allocatableUser, _ ->
          `Given an Offender` { offenderDetails, inmateDetails ->
            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = user,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            ) { placementRequest, application ->
              webTestClient.get()
                .uri("/applications/${application.id}/tasks/placement-request")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    TaskWrapper(
                      task = taskTransformer.transformPlacementRequestToTask(placementRequest, offenderDetails, inmateDetails),
                      users = listOf(userTransformer.transformJpaToApi(allocatableUser, ServiceName.approvedPremises)),
                    ),
                  ),
                )
            }
          }
        }
      }
    }
  }

  @Test
  fun `Get an non-implemented task type for an application returns 405`() {
    `Given a User`(roles = listOf(UserRole.WORKFLOW_MANAGER)) { user, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        `Given an Assessment for Approved Premises`(
          allocatedToUser = user,
          createdByUser = user,
          crn = offenderDetails.otherIds.crn,
        ) { _, application ->
          webTestClient.get()
            .uri("/applications/${application.id}/tasks/booking-appeal")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
        }
      }
    }
  }

  @Test
  fun `Reallocate application to different assessor without JWT returns 401`() {
    webTestClient.post()
      .uri("/applications/9c7abdf6-fd39-4670-9704-98a5bbfec95e/tasks/assessment/allocations")
      .bodyValue(
        NewReallocation(
          userId = UUID.randomUUID(),
        ),
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Reallocate application to different assessor without WORKFLOW_MANAGER role returns 403`() {
    `Given a User` { _, jwt ->
      webTestClient.post()
        .uri("/applications/9c7abdf6-fd39-4670-9704-98a5bbfec95e/tasks/assessment/allocations")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewReallocation(
            userId = UUID.randomUUID(),
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Reallocate assessment to different assessor returns 201, creates new assessment, deallocates old one`() {
    `Given a User`(roles = listOf(UserRole.WORKFLOW_MANAGER)) { _, jwt ->
      `Given a User`(roles = listOf(UserRole.ASSESSOR)) { user, _ ->
        `Given a User`(
          roles = listOf(UserRole.ASSESSOR),
        ) { assigneeUser, _ ->
          `Given an Offender` { offenderDetails, _ ->
            `Given an Assessment for Approved Premises`(
              allocatedToUser = user,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            ) { existingAssessment, application ->

              webTestClient.post()
                .uri("/applications/${application.id}/tasks/assessment/allocations")
                .header("Authorization", "Bearer $jwt")
                .bodyValue(
                  NewReallocation(
                    userId = assigneeUser.id,
                  ),
                )
                .exchange()
                .expectStatus()
                .isCreated
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    Reallocation(
                      user = userTransformer.transformJpaToApi(assigneeUser, ServiceName.approvedPremises) as ApprovedPremisesUser,
                      taskType = TaskType.assessment,
                    ),
                  ),
                )

              val assessments = assessmentRepository.findAll()

              Assertions.assertThat(assessments.first { it.id == existingAssessment.id }.reallocatedAt).isNotNull
              Assertions.assertThat(assessments)
                .anyMatch { it.application.id == application.id && it.allocatedToUser.id == assigneeUser.id }
            }
          }
        }
      }
    }
  }

  @Test
  fun `Reallocating a placement request to different assessor returns 201, creates new placement request, deallocates old one`() {
    `Given a User`(roles = listOf(UserRole.WORKFLOW_MANAGER)) { user, jwt ->
      `Given a User`(
        roles = listOf(UserRole.MATCHER),
      ) { assigneeUser, _ ->
        `Given an Offender` { offenderDetails, _ ->
          `Given a Placement Request`(
            createdByUser = user,
            placementRequestAllocatedTo = user,
            assessmentAllocatedTo = user,
            crn = offenderDetails.otherIds.crn,
          ) { existingPlacementRequest, application ->
            webTestClient.post()
              .uri("/applications/${application.id}/tasks/placement-request/allocations")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                NewReallocation(
                  userId = assigneeUser.id,
                ),
              )
              .exchange()
              .expectStatus()
              .isCreated
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  Reallocation(
                    user = userTransformer.transformJpaToApi(assigneeUser, ServiceName.approvedPremises) as ApprovedPremisesUser,
                    taskType = TaskType.placementRequest,
                  ),
                ),
              )

            val placementRequests = placementRequestRepository.findAll()
            val allocatedPlacementRequest = placementRequests.find { it.allocatedToUser.id == assigneeUser.id }

            Assertions.assertThat(placementRequests.first { it.id == existingPlacementRequest.id }.reallocatedAt).isNotNull
            Assertions.assertThat(allocatedPlacementRequest).isNotNull

            val desirableCriteria = allocatedPlacementRequest!!.placementRequirements.desirableCriteria.map { it.propertyName }
            val essentialCriteria = allocatedPlacementRequest!!.placementRequirements.essentialCriteria.map { it.propertyName }

            Assertions.assertThat(desirableCriteria).isEqualTo(existingPlacementRequest.placementRequirements.desirableCriteria.map { it.propertyName })
            Assertions.assertThat(essentialCriteria).isEqualTo(existingPlacementRequest.placementRequirements.essentialCriteria.map { it.propertyName })
          }
        }
      }
    }
  }

  @Test
  fun `Reallocating a placement request review returns a NotAllowedProblem`() {
    `Given a User`(roles = listOf(UserRole.WORKFLOW_MANAGER)) { user, jwt ->
      `Given a User` { userToReallocate, _ ->
        `Given an Application`(createdByUser = user) { application ->
          webTestClient.post()
            .uri("/applications/${application.id}/tasks/placement-request-review/allocations")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewReallocation(
                userId = userToReallocate.id,
              ),
            )
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
        }
      }
    }
  }

  @Test
  fun `Reallocating a booking appeal returns a NotAllowedProblem`() {
    `Given a User`(roles = listOf(UserRole.WORKFLOW_MANAGER)) { user, jwt ->
      `Given a User` { userToReallocate, _ ->
        `Given an Application`(createdByUser = user) { application ->
          webTestClient.post()
            .uri("/applications/${application.id}/tasks/booking-appeal/allocations")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewReallocation(
                userId = userToReallocate.id,
              ),
            )
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
        }
      }
    }
  }
}
