package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskWrapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Placement Application`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Placement Request`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Application`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Assessment for Approved Premises`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Assessment for Temporary Accommodation`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.TaskTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import java.time.OffsetDateTime
import java.util.UUID

class TasksTest : IntegrationTestBase() {
  @Autowired
  lateinit var taskTransformer: TaskTransformer

  @Autowired
  lateinit var userTransformer: UserTransformer

  @BeforeEach
  fun stubBankHolidaysApi() {
    GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()
  }

  @Nested
  inner class GetAllReallocatableTest {
    @Test
    fun `Get all reallocatable tasks without JWT returns 401`() {
      webTestClient.get()
        .uri("/tasks/reallocatable")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get all reallocatable tasks without workflow manager permissions returns 403`() {
      `Given a User` { _, jwt ->
        webTestClient.get()
          .uri("/tasks/reallocatable")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get all reallocatable tasks returns 200 with correct body, only returns Assessments from CAS1`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, inmateDetails ->
            `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              reallocated = true,
            )

            `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              reallocated = true,
              isWithdrawn = true,
            )

            `Given an Assessment for Temporary Accommodation`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              reallocated = true,
            )

            `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              decision = PlacementApplicationDecision.ACCEPTED,
              crn = offenderDetails.otherIds.crn,
            )

            `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              reallocated = true,
              crn = offenderDetails.otherIds.crn,
            )

            `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
            )

            `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              reallocated = true,
              crn = offenderDetails.otherIds.crn,
            )

            val (allocatableAssessment) = `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            val (allocatablePlacementRequest) = `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            val (placementRequestMarkedAsUnableToMatch) = `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            bookingNotMadeFactory.produceAndPersist {
              withPlacementRequest(placementRequestMarkedAsUnableToMatch)
            }

            val allocatablePlacementApplication = `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
            )

            webTestClient.get()
              .uri("/tasks/reallocatable")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    taskTransformer.transformAssessmentToTask(
                      allocatableAssessment,
                      "${offenderDetails.firstName} ${offenderDetails.surname}",
                    ),
                    taskTransformer.transformPlacementRequestToTask(
                      allocatablePlacementRequest,
                      "${offenderDetails.firstName} ${offenderDetails.surname}",
                    ),
                    taskTransformer.transformPlacementApplicationToTask(
                      allocatablePlacementApplication,
                      "${offenderDetails.firstName} ${offenderDetails.surname}",
                    ),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `Get all reallocatable tasks with taskType that doesn't exist returns 404`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        webTestClient.get()
          .uri("/tasks/reallocatable?type=RANDOMWORD")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Get all reallocatable tasks with taskType of bookingAppeal returns 400`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        webTestClient.get()
          .uri("/tasks/reallocatable?type=bookingAppeal")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isBadRequest
      }
    }

    @Test
    fun `Get all reallocatable tasks returns 200 with correct body when type assessment is passed as parameter`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->
            val (allocatableAssessment) = `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            webTestClient.get()
              .uri("/tasks/reallocatable?type=assessment")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    taskTransformer.transformAssessmentToTask(
                      allocatableAssessment,
                      "${offenderDetails.firstName} ${offenderDetails.surname}",
                    ),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `Get all reallocatable tasks returns 200 with correct body when type placement request is passed as parameter`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val (allocatablePlacementRequest) = `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            webTestClient.get()
              .uri("/tasks/reallocatable?type=PlacementRequest")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    taskTransformer.transformPlacementRequestToTask(
                      allocatablePlacementRequest,
                      "${offenderDetails.firstName} ${offenderDetails.surname}",
                    ),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `Get all reallocatable tasks returns 200 with correct body when type placement application is passed as parameter`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val allocatablePlacementApplication = `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
            )

            webTestClient.get()
              .uri("/tasks/reallocatable?type=PlacementApplication")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    taskTransformer.transformPlacementApplicationToTask(
                      allocatablePlacementApplication,
                      "${offenderDetails.firstName} ${offenderDetails.surname}",
                    ),
                  ),
                ),
              )
          }
        }
      }
    }
  }

  @Nested
  inner class GetAllForUserTest {
    @Test
    fun `Get all tasks for a user without JWT returns 401`() {
      webTestClient.get()
        .uri("/tasks")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get all tasks for a user returns the relevant tasks for a user`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MATCHER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, inmateDetails ->
            val (placementRequestAllocatedToMe) = `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            val placementApplicationAllocatedToMe = `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
            )

            `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              reallocated = true,
            )

            `Given a Placement Application`(
              createdByUser = otherUser,
              allocatedToUser = otherUser,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
            )

            `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              reallocated = true,
            )

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
                    taskTransformer.transformPlacementRequestToTask(
                      placementRequestAllocatedToMe,
                      "${offenderDetails.firstName} ${offenderDetails.surname}",
                    ),
                    taskTransformer.transformPlacementApplicationToTask(
                      placementApplicationAllocatedToMe,
                      "${offenderDetails.firstName} ${offenderDetails.surname}",
                    ),
                  ),
                ),
              )
          }
        }
      }
    }
  }

  @Nested
  inner class GetTaskTest {
    @Test
    fun `Get a Task for an application without JWT returns 401`() {
      webTestClient.get()
        .uri("/tasks/assessment/f601ff2d-b1e0-4878-8731-ccfa19a2ce84")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get an unknown task type for an application returns 404`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          `Given an Assessment for Approved Premises`(
            allocatedToUser = user,
            createdByUser = user,
            crn = offenderDetails.otherIds.crn,
          ) { _, application ->
            webTestClient.get()
              .uri("/tasks/unknown-task/${application.id}")
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
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
        `Given a User` { user, _ ->
          `Given a User`(
            roles = listOf(UserRole.CAS1_ASSESSOR),
          ) { allocatableUser, _ ->
            `Given a User`(
              roles = listOf(UserRole.CAS1_ASSESSOR),
              isActive = false,
            ) { _, _ ->
              `Given an Offender` { offenderDetails, inmateDetails ->
                `Given an Assessment for Approved Premises`(
                  allocatedToUser = user,
                  createdByUser = user,
                  crn = offenderDetails.otherIds.crn,
                ) { assessment, _ ->
                  webTestClient.get()
                    .uri("/tasks/assessment/${assessment.id}")
                    .header("Authorization", "Bearer $jwt")
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody()
                    .json(
                      objectMapper.writeValueAsString(
                        TaskWrapper(
                          task = taskTransformer.transformAssessmentToTask(assessment, "${offenderDetails.firstName} ${offenderDetails.surname}"),
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
    }

    @Test
    fun `Get a Placement Request Task for an application returns 200`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
        `Given a User` { user, _ ->
          `Given a User`(
            roles = listOf(UserRole.CAS1_MATCHER),
          ) { allocatableUser, _ ->
            `Given an Offender` { offenderDetails, inmateDetails ->
              `Given a Placement Request`(
                placementRequestAllocatedTo = user,
                assessmentAllocatedTo = user,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              ) { placementRequest, _ ->
                webTestClient.get()
                  .uri("/tasks/placement-request/${placementRequest.id}")
                  .header("Authorization", "Bearer $jwt")
                  .exchange()
                  .expectStatus()
                  .isOk
                  .expectBody()
                  .json(
                    objectMapper.writeValueAsString(
                      TaskWrapper(
                        task = taskTransformer.transformPlacementRequestToTask(placementRequest, "${offenderDetails.firstName} ${offenderDetails.surname}"),
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
    fun `Get a Placement Application Task for an application returns 200`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
        `Given a User` { user, _ ->
          `Given a User`(
            roles = listOf(UserRole.CAS1_ASSESSOR),
          ) { allocatableUser, _ ->
            `Given an Offender` { offenderDetails, inmateDetails ->
              `Given a Placement Application`(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
              ) { placementApplication ->
                webTestClient.get()
                  .uri("/tasks/placement-application/${placementApplication.id}")
                  .header("Authorization", "Bearer $jwt")
                  .exchange()
                  .expectStatus()
                  .isOk
                  .expectBody()
                  .json(
                    objectMapper.writeValueAsString(
                      TaskWrapper(
                        task = taskTransformer.transformPlacementApplicationToTask(
                          placementApplication,
                          "${offenderDetails.firstName} ${offenderDetails.surname}",
                        ),
                        users = listOf(
                          userTransformer.transformJpaToApi(
                            allocatableUser,
                            ServiceName.approvedPremises,
                          ),
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

    @Test
    fun `Get an non-implemented task type for an application returns 405`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          `Given an Assessment for Approved Premises`(
            allocatedToUser = user,
            createdByUser = user,
            crn = offenderDetails.otherIds.crn,
          ) { _, application ->
            webTestClient.get()
              .uri("/tasks/booking-appeal/${application.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
          }
        }
      }
    }
  }

  @Nested
  inner class GetTaskTaskIdTest {
    @Test
    fun `Get placement application tasks without JWT returns 401`() {
      webTestClient.get()
        .uri("/tasks/PlacementApplication")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get an unknown task type returns 404`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          `Given an Assessment for Approved Premises`(
            allocatedToUser = user,
            createdByUser = user,
            crn = offenderDetails.otherIds.crn,
          ) { _, application ->
            webTestClient.get()
              .uri("/tasks/unknown-type")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isNotFound
          }
        }
      }
    }

    @Test
    fun `Get tasks by taskType for a user returns the relevant placement requests tasks for a user`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MATCHER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, inmateDetails ->
            val (placementRequestAllocatedToMe) = `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              reallocated = true,
            )

            `Given a Placement Application`(
              createdByUser = otherUser,
              allocatedToUser = otherUser,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
            )

            `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              reallocated = true,
            )

            webTestClient.get()
              .uri("/tasks/placement-request")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    taskTransformer.transformPlacementRequestToTask(
                      placementRequestAllocatedToMe,
                      "${offenderDetails.firstName} ${offenderDetails.surname}",
                    ),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `Get tasks by taskType for a user returns the relevant placement applications tasks for a user`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MATCHER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, inmateDetails ->

            val placementApplicationAllocatedToMe = `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
            )

            `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              reallocated = true,
            )

            `Given a Placement Application`(
              createdByUser = otherUser,
              allocatedToUser = otherUser,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
            )

            `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              reallocated = true,
            )

            webTestClient.get()
              .uri("/tasks/placement-application")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    taskTransformer.transformPlacementApplicationToTask(
                      placementApplicationAllocatedToMe,
                      "${offenderDetails.firstName} ${offenderDetails.surname}",
                    ),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `Get an non-implemented task type for an application returns 405`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MATCHER)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          `Given an Assessment for Approved Premises`(
            allocatedToUser = user,
            createdByUser = user,
            crn = offenderDetails.otherIds.crn,
          ) { _, application ->
            webTestClient.get()
              .uri("/tasks/booking-appeal")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isEqualTo(HttpStatus.BAD_REQUEST)
          }
        }
      }
    }
  }

  @Nested
  inner class ReallocateTaskTest {
    @Test
    fun `Reallocate application to different assessor without JWT returns 401`() {
      webTestClient.post()
        .uri("/tasks/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
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
          .uri("/tasks/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
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
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
        `Given a User`(roles = listOf(UserRole.CAS1_ASSESSOR)) { user, _ ->
          `Given a User`(
            roles = listOf(UserRole.CAS1_ASSESSOR),
          ) { assigneeUser, _ ->
            `Given an Offender` { offenderDetails, _ ->
              `Given an Assessment for Approved Premises`(
                allocatedToUser = user,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              ) { existingAssessment, application ->

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

                val assessments = approvedPremisesAssessmentRepository.findAll()

                Assertions.assertThat(assessments.first { it.id == existingAssessment.id }.reallocatedAt).isNotNull
                Assertions.assertThat(assessments)
                  .anyMatch { it.application.id == application.id && it.allocatedToUser!!.id == assigneeUser.id }
              }
            }
          }
        }
      }
    }

    @Test
    fun `Reallocating a placement request to different assessor returns 201, creates new placement request, deallocates old one`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User`(
          roles = listOf(UserRole.CAS1_MATCHER),
        ) { assigneeUser, _ ->
          `Given an Offender` { offenderDetails, _ ->
            `Given a Placement Request`(
              createdByUser = user,
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = user,
              crn = offenderDetails.otherIds.crn,
            ) { existingPlacementRequest, _ ->
              webTestClient.post()
                .uri("/tasks/placement-request/${existingPlacementRequest.id}/allocations")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
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
              val allocatedPlacementRequest = placementRequests.find { it.allocatedToUser!!.id == assigneeUser.id }

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
    fun `Reallocating a placement application to different assessor returns 201, creates new placement application, deallocates old one`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
        `Given a User` { user, _ ->
          `Given a User`(
            roles = listOf(UserRole.CAS1_MATCHER),
          ) { assigneeUser, _ ->
            `Given an Offender` { offenderDetails, _ ->
              `Given a Placement Application`(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
              ) { placementApplication ->
                val placementDate = placementDateFactory.produceAndPersist {
                  withPlacementApplication(placementApplication)
                }

                webTestClient.post()
                  .uri("/tasks/placement-application/${placementApplication.id}/allocations")
                  .header("Authorization", "Bearer $jwt")
                  .header("X-Service-Name", ServiceName.approvedPremises.value)
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
                        taskType = TaskType.placementApplication,
                      ),
                    ),
                  )

                val placementApplications = placementApplicationRepository.findAll()
                val allocatedPlacementApplication = placementApplications.find { it.allocatedToUser!!.id == assigneeUser.id }

                Assertions.assertThat(placementApplications.first { it.id == placementApplication.id }.reallocatedAt).isNotNull
                Assertions.assertThat(allocatedPlacementApplication).isNotNull

                val placementDates = allocatedPlacementApplication!!.placementDates

                Assertions.assertThat(placementDates.size).isEqualTo(1)
                Assertions.assertThat(placementDates[0].expectedArrival).isEqualTo(placementDate.expectedArrival)
                Assertions.assertThat(placementDates[0].duration).isEqualTo(placementDate.duration)
              }
            }
          }
        }
      }
    }

    @Test
    fun `Reallocating a booking appeal returns a NotAllowedProblem`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { userToReallocate, _ ->
          `Given an Application`(createdByUser = user) { application ->
            webTestClient.post()
              .uri("/tasks/booking-appeal/${application.id}/allocations")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
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
    fun `Reallocating a Temporary Accommodation assessment does not require a request body`() {
      `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { originalUser, _ ->
        `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { expectedUser, jwt ->
          `Given an Offender` { offenderDetails, _ ->
            `Given an Assessment for Temporary Accommodation`(
              allocatedToUser = originalUser,
              createdByUser = originalUser,
              crn = offenderDetails.otherIds.crn,
            ) { assessment, _ ->
              webTestClient.post()
                .uri("/tasks/assessment/${assessment.id}/allocations")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
                .bodyValue(Unit)
                .exchange()
                .expectStatus()
                .isCreated

              val result = temporaryAccommodationAssessmentRepository.findAll().first { it.id == assessment.id }
              Assertions.assertThat(result.allocatedToUser).isNotNull()
              Assertions.assertThat(result.allocatedToUser!!.id).isEqualTo(expectedUser.id)
            }
          }
        }
      }
    }
  }

  @Nested
  inner class DeallocateTaskTest {
    @Test
    fun `Deallocate assessment without JWT returns 401 Unauthorized`() {
      webTestClient.delete()
        .uri("/tasks/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Deallocate Temporary Accommodation assessment without CAS3_ASSESSOR role returns 403 Forbidden`() {
      `Given a User` { _, jwt ->
        webTestClient.delete()
          .uri("/tasks/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Deallocate Approved Premises assessment returns 403 Forbidden`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          `Given a User` { _, _ ->
            `Given an Assessment for Approved Premises`(
              allocatedToUser = user,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            ) { assessment, _ ->
              webTestClient.delete()
                .uri("/tasks/assessment/${assessment.id}/allocations")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
                .exchange()
                .expectStatus()
                .isForbidden
            }
          }
        }
      }
    }

    @Test
    fun `Deallocate Temporary Accommodation assessment returns 200 and unassigns the allocated user`() {
      `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          `Given an Assessment for Temporary Accommodation`(
            allocatedToUser = user,
            createdByUser = user,
            crn = offenderDetails.otherIds.crn,
          ) { existingAssessment, _ ->

            webTestClient.delete()
              .uri("/tasks/assessment/${existingAssessment.id}/allocations")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
              .exchange()
              .expectStatus()
              .isNoContent

            val assessment = temporaryAccommodationAssessmentRepository.findAll().first { it.id == existingAssessment.id }

            Assertions.assertThat(assessment.allocatedToUser).isNull()
            Assertions.assertThat(assessment.allocatedAt).isNull()
          }
        }
      }
    }
  }
}
