package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an AP Area`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Application`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Assessment for Approved Premises`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Assessment for Temporary Accommodation`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.UserWorkload
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

  @SuppressWarnings("LargeClass")
  @Nested
  inner class GetAllReallocatableTest {
    private val pageSize = 10

    @Test
    fun `Get all reallocatable tasks without JWT returns 401`() {
      webTestClient.get()
        .uri("/tasks/reallocatable")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get all reallocatable tasks without workflow manager or matcher permissions returns 403`() {
      `Given a User` { _, jwt ->
        webTestClient.get()
          .uri("/tasks/reallocatable")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_MATCHER", "CAS1_WORKFLOW_MANAGER"])
    fun `Get all reallocatable tasks returns 200 when have CAS1_WORKFLOW_MANAGER OR CAS1_MATCHER roles`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { callingUser, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val task = `Given a Placement Application`(
              createdByUser = otherUser,
              allocatedToUser = otherUser,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
            )

            val expectedTasks = listOf(
              taskTransformer.transformPlacementApplicationToTask(
                task,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
            )

            webTestClient.get()
              .uri("/tasks/reallocatable?page=1&sortBy=createdAt&sortDirection=asc&allocatedToUserId=${otherUser.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  expectedTasks,
                ),
              )
          }
        }
      }
    }

    @Test
    fun `Get all reallocatable tasks returns 200 when no type with only CAS1 assessments`() {
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
              isWithdrawn = true,
            )

            `Given an Assessment for Temporary Accommodation`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
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
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
        webTestClient.get()
          .uri("/tasks/reallocatable?type=RANDOMWORD")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Get all reallocatable tasks returns 200 when type assessment`() {
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
    fun `Get all reallocatable tasks returns 200 when type assessment and ap area defined`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val apArea1 = `Given an AP Area`()
            val apArea2 = `Given an AP Area`()

            val (allocatableAssessment) = `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea1,
            )

            `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea2,
            )

            webTestClient.get()
              .uri("/tasks/reallocatable?type=assessment&apAreaId=${apArea1.id}")
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
    fun `Get all reallocatable tasks returns 200 when type assessment and allocated to user defined`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val apArea1 = `Given an AP Area`()
            val apArea2 = `Given an AP Area`()

            val (allocatableAssessment) = `Given an Assessment for Approved Premises`(
              allocatedToUser = user,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea1,
            )

            `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea2,
            )

            webTestClient.get()
              .uri("/tasks/reallocatable?type=assessment&allocatedToUserId=${user.id}")
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
    fun `Get all reallocatable tasks returns 200 when type placement request`() {
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
    fun `Get all reallocatable tasks returns 200 when type placement application`() {
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

    @Test
    fun `Get all reallocatable tasks returns 200 when type placement application and ap area defined`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val apArea1 = `Given an AP Area`()
            val apArea2 = `Given an AP Area`()

            val allocatablePlacementApplication = `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
              apArea = apArea1,
            )

            `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
              apArea = apArea2,
            )

            webTestClient.get()
              .uri("/tasks/reallocatable?type=PlacementApplication&apAreaId=${apArea1.id}")
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

    @Test
    fun `Get all reallocatable tasks returns 200 when type placement application and allocated to user defined`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val apArea1 = `Given an AP Area`()
            val apArea2 = `Given an AP Area`()

            val allocatablePlacementApplication = `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
              apArea = apArea1,
            )

            `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = otherUser,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
              apArea = apArea2,
            )

            webTestClient.get()
              .uri("/tasks/reallocatable?type=PlacementApplication&allocatedToUserId=${user.id}")
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

    @Test
    fun `Get all reallocatable tasks returns 200 when type assessment and page is two and no allocated filter`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->
            val numAllocatableAssessment = 7
            val numUnallocatableAssessment = 5
            val totalTasks = numAllocatableAssessment + numUnallocatableAssessment

            repeat(numAllocatableAssessment) {
              `Given an Assessment for Approved Premises`(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numUnallocatableAssessment) {
              `Given an Assessment for Approved Premises`(
                null,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            webTestClient.get()
              .uri("/tasks/reallocatable?type=assessment&page=2")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", totalTasks.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all reallocatable tasks returns 200 when type assessment and page is two and allocated filter allocated`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->
            val numAllocatableAssessment = 12
            val numUnallocatableAssessment = 5

            repeat(numAllocatableAssessment) {
              `Given an Assessment for Approved Premises`(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numUnallocatableAssessment) {
              `Given an Assessment for Approved Premises`(
                null,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            webTestClient.get()
              .uri("/tasks/reallocatable?type=assessment&page=2&allocatedFilter=allocated")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", numAllocatableAssessment.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all reallocatable tasks returns 200 when type assessment and page is two and allocated filter unallocated`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->
            val numAllocatableAssessment = 2
            val numUnallocatableAssessment = 15

            repeat(numAllocatableAssessment) {
              `Given an Assessment for Approved Premises`(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numUnallocatableAssessment) {
              `Given an Assessment for Approved Premises`(
                null,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            webTestClient.get()
              .uri("/tasks/reallocatable?type=assessment&page=2&allocatedFilter=unallocated")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", numUnallocatableAssessment.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all reallocatable tasks returns 200 when type placement requests and page two no allocated filter`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val numAllocatedRequests = 8
            val numUnallocatedPlacementRequests = 6
            val totalTasks = numAllocatedRequests + numUnallocatedPlacementRequests

            repeat(numAllocatedRequests) {
              `Given a Placement Request`(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numUnallocatedPlacementRequests) {
              `Given a Placement Request`(
                null,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            webTestClient.get()
              .uri("/tasks/reallocatable?type=PlacementRequest&page=2")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", totalTasks.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all reallocatable tasks returns 200 when type placement requests and ap area defined`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val apArea1 = `Given an AP Area`()
            val apArea2 = `Given an AP Area`()

            `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea1,
            )

            val (allocatablePlacementRequest) = `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea2,
            )

            webTestClient.get()
              .uri("/tasks/reallocatable?type=PlacementRequest&apAreaId=${apArea2.id}")
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
    fun `Get all reallocatable tasks returns 200 when type placement requests and allocated to user defined`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val apArea1 = `Given an AP Area`()
            val apArea2 = `Given an AP Area`()

            `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea1,
            )

            val (allocatablePlacementRequest) = `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea2,
            )

            webTestClient.get()
              .uri("/tasks/reallocatable?type=PlacementRequest&allocatedToUserId=${user.id}")
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
    fun `Get all reallocatable tasks returns 200 when type placement requests and page two and allocated filter allocated`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val numAllocatedRequests = 12
            val numUnallocatedPlacementRequests = 5

            repeat(numAllocatedRequests) {
              `Given a Placement Request`(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numUnallocatedPlacementRequests) {
              `Given a Placement Request`(
                null,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            webTestClient.get()
              .uri("/tasks/reallocatable?type=PlacementRequest&page=2&allocatedFilter=allocated")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", numAllocatedRequests.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all reallocatable tasks returns 200 when type placement requests and page two and allocated filter unallocated`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val numAllocatedRequests = 2
            val numUnallocatedPlacementRequests = 15

            repeat(numAllocatedRequests) {
              `Given a Placement Request`(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numUnallocatedPlacementRequests) {
              `Given a Placement Request`(
                null,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            webTestClient.get()
              .uri("/tasks/reallocatable?type=PlacementRequest&page=2&allocatedFilter=unallocated")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", numUnallocatedPlacementRequests.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all reallocatable tasks returns 200 when no type retains original sort order`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->
            val (task1, _) = `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            val (task2, _) = `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            val (task3, _) = `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            val task4 = `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
            )

            val (task5) = `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            val expectedTasks = listOf(
              taskTransformer.transformPlacementRequestToTask(
                task1,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
              taskTransformer.transformAssessmentToTask(
                task2,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
              taskTransformer.transformPlacementRequestToTask(
                task3,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
              taskTransformer.transformPlacementApplicationToTask(
                task4,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
              taskTransformer.transformAssessmentToTask(
                task5,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
            )

            webTestClient.get()
              .uri("/tasks/reallocatable?page=1&sortBy=createdAt&sortDirection=asc")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  expectedTasks,
                ),
              )

            webTestClient.get()
              .uri("/tasks/reallocatable?page=1&sortBy=createdAt&sortDirection=desc")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  expectedTasks.reversed(),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `Get all reallocatable tasks returns 200 when no type and allocated to user defined`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { callingUser, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val (task1, _) = `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = callingUser,
              createdByUser = callingUser,
              crn = offenderDetails.otherIds.crn,
            )

            `Given a Placement Request`(
              placementRequestAllocatedTo = callingUser,
              assessmentAllocatedTo = callingUser,
              createdByUser = callingUser,
              crn = offenderDetails.otherIds.crn,
            )

            val (task2, _) = `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = callingUser,
              crn = offenderDetails.otherIds.crn,
            )

            `Given an Assessment for Approved Premises`(
              allocatedToUser = callingUser,
              createdByUser = callingUser,
              crn = offenderDetails.otherIds.crn,
            )

            val task3 = `Given a Placement Application`(
              createdByUser = otherUser,
              allocatedToUser = otherUser,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
            )

            `Given a Placement Application`(
              createdByUser = callingUser,
              allocatedToUser = callingUser,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
            )

            val expectedTasks = listOf(
              taskTransformer.transformPlacementRequestToTask(
                task1,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
              taskTransformer.transformAssessmentToTask(
                task2,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
              taskTransformer.transformPlacementApplicationToTask(
                task3,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
            )

            webTestClient.get()
              .uri("/tasks/reallocatable?page=1&sortBy=createdAt&sortDirection=asc&allocatedToUserId=${otherUser.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  expectedTasks,
                ),
              )
          }
        }
      }
    }

    @Test
    fun `Get all reallocatable tasks returns 200 when no type and ap area defined`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val apArea1 = `Given an AP Area`()
            val apArea2 = `Given an AP Area`()

            val (task1, _) = `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea1,
            )

            `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea2,
            )

            val (task2, _) = `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea1,
            )

            `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea2,
            )

            val task3 = `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
              apArea = apArea1,
            )

            `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
              apArea = apArea2,
            )

            val expectedTasks = listOf(
              taskTransformer.transformPlacementRequestToTask(
                task1,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
              taskTransformer.transformAssessmentToTask(
                task2,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
              taskTransformer.transformPlacementApplicationToTask(
                task3,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
            )

            webTestClient.get()
              .uri("/tasks/reallocatable?page=1&sortBy=createdAt&sortDirection=asc&apAreaId=${apArea1.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  expectedTasks,
                ),
              )
          }
        }
      }
    }

    @Test
    fun `Get all reallocatable tasks returns 200 when type placement application and page two`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val numAllocatablePlacementApplications = 12

            repeat(numAllocatablePlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            webTestClient.get()
              .uri("/tasks/reallocatable?type=PlacementApplication&page=2")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", numAllocatablePlacementApplications.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all reallocatable tasks returns 200 when type placement application and page two and no allocated filter`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val numAllocatedPlacementApplications = 6
            val numUnallocatedPlacementApplications = 5
            val totalTasks = numAllocatedPlacementApplications + numUnallocatedPlacementApplications

            repeat(numAllocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            repeat(numUnallocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            webTestClient.get()
              .uri("/tasks/reallocatable?type=PlacementApplication&page=2")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", totalTasks.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all reallocatable tasks returns 200 when type placement application and page two and unallocated filter`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val numAllocatedPlacementApplications = 12
            val numUnallocatedPlacementApplications = 15

            repeat(numAllocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            repeat(numUnallocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            webTestClient.get()
              .uri("/tasks/reallocatable?type=PlacementApplication&page=2&allocatedFilter=unallocated")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", numUnallocatedPlacementApplications.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all reallocatable tasks returns 200 when type placement application and page two and allocated filter`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val numAllocatedPlacementApplications = 12
            val numUnallocatedPlacementApplications = 15

            repeat(numAllocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            repeat(numUnallocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            webTestClient.get()
              .uri("/tasks/reallocatable?type=PlacementApplication&page=2&allocatedFilter=allocated")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", numAllocatedPlacementApplications.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all reallocatable tasks returns 200 when no type and page two and no allocated filter`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val numAllocatedPlacementApplications = 2
            val numUnallocatedPlacementApplications = 3
            val numAllocatableAssessment = 4
            val numUnallocatableAssessment = 5
            val numAllocatedPlacementRequests = 6
            val numUnallocatedPlacementRequests = 7

            val totalTasks = numAllocatedPlacementApplications +
              numUnallocatedPlacementApplications +
              numAllocatableAssessment +
              numUnallocatableAssessment +
              numAllocatedPlacementRequests +
              numUnallocatedPlacementRequests

            repeat(numAllocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            repeat(numUnallocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            repeat(numAllocatableAssessment) {
              `Given an Assessment for Approved Premises`(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numUnallocatableAssessment) {
              `Given an Assessment for Approved Premises`(
                null,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numAllocatedPlacementRequests) {
              `Given a Placement Request`(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numUnallocatedPlacementRequests) {
              `Given a Placement Request`(
                null,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            webTestClient.get()
              .uri("/tasks/reallocatable?page=2")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 3)
              .expectHeader().valueEquals("X-Pagination-TotalResults", totalTasks.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all reallocatable tasks returns 200 when no type and page two and allocated filter`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val numAllocatedPlacementApplications = 12
            val numUnallocatedPlacementApplications = 1
            val numAllocatableAssessment = 4
            val numUnallocatableAssessment = 1
            val numAllocatedPlacementRequests = 6
            val numUnallocatedPlacementRequests = 1

            val totalTasks = numAllocatedPlacementApplications +
              numAllocatableAssessment +
              numAllocatedPlacementRequests

            repeat(numAllocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            repeat(numUnallocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            repeat(numAllocatableAssessment) {
              `Given an Assessment for Approved Premises`(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numUnallocatableAssessment) {
              `Given an Assessment for Approved Premises`(
                null,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numAllocatedPlacementRequests) {
              `Given a Placement Request`(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numUnallocatedPlacementRequests) {
              `Given a Placement Request`(
                null,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            webTestClient.get()
              .uri("/tasks/reallocatable?page=2&allocatedFilter=allocated")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 3)
              .expectHeader().valueEquals("X-Pagination-TotalResults", totalTasks.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all reallocatable tasks returns 200 when no type and page two and unallocated filter`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val numAllocatedPlacementApplications = 1
            val numAllocatedAssessments = 1
            val numAllocatedPlacementRequests = 1

            repeat(numAllocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            repeat(numAllocatedAssessments) {
              `Given an Assessment for Approved Premises`(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numAllocatedPlacementRequests) {
              `Given a Placement Request`(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            val numUnallocatedPlacementApplications = 11
            val numUnallocatedAssessments = 5
            val numUnallocatedPlacementRequests = 7

            repeat(numUnallocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
                reallocated = false,
                decision = null,
              )
            }

            repeat(numUnallocatedAssessments) {
              `Given an Assessment for Approved Premises`(
                null,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numUnallocatedPlacementRequests) {
              `Given a Placement Request`(
                null,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            val totalUnallocatedTasks = numUnallocatedPlacementApplications +
              numUnallocatedAssessments +
              numUnallocatedPlacementRequests

            webTestClient.get()
              .uri("/tasks/reallocatable?page=2&allocatedFilter=unallocated")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 3)
              .expectHeader().valueEquals("X-Pagination-TotalResults", totalUnallocatedTasks.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }
  }

  @SuppressWarnings("LargeClass")
  @Nested
  inner class GetTasksTest {
    private val pageSize = 10

    @Test
    fun `Get all tasks without JWT returns 401`() {
      webTestClient.get()
        .uri("/task")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get all tasks without workflow manager or matcher permissions returns 403`() {
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
    fun `Get all tasks returns 200 when types specified (all types)`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->
            val (task1, _) = `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            val (task2, _) = `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            val (task3, _) = `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            val task4 = `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
            )

            val (task5) = `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            val expectedTasks = listOf(
              taskTransformer.transformPlacementRequestToTask(
                task1,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
              taskTransformer.transformAssessmentToTask(
                task2,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
              taskTransformer.transformPlacementRequestToTask(
                task3,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
              taskTransformer.transformPlacementApplicationToTask(
                task4,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
              taskTransformer.transformAssessmentToTask(
                task5,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
            )

            webTestClient.get()
              .uri("/tasks?page=1&sortBy=createdAt&sortDirection=asc&types=PlacementApplication&types=Assessment&types=PlacementRequest")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  expectedTasks,
                ),
              )
          }
        }
      }
    }

    @Test
    fun `Get all tasks returns 200 when types specified (2 types)`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->
            `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            val (assessment1, _) = `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            val placementApp1 = `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
            )

            val (assessment2, _) = `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            val expectedTasks = listOf(
              taskTransformer.transformAssessmentToTask(
                assessment1,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
              taskTransformer.transformPlacementApplicationToTask(
                placementApp1,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
              taskTransformer.transformAssessmentToTask(
                assessment2,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
            )

            webTestClient.get()
              .uri("/tasks?page=1&sortBy=createdAt&sortDirection=asc&types=PlacementApplication&types=Assessment")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  expectedTasks,
                ),
              )
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_MATCHER", "CAS1_WORKFLOW_MANAGER"])
    fun `Get all tasks returns 200 when have CAS1_WORKFLOW_MANAGER OR CAS1_MATCHER roles`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val task = `Given a Placement Application`(
              createdByUser = otherUser,
              allocatedToUser = otherUser,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
            )

            val expectedTasks = listOf(
              taskTransformer.transformPlacementApplicationToTask(
                task,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
            )

            webTestClient.get()
              .uri("/tasks?page=1&sortBy=createdAt&sortDirection=asc")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  expectedTasks,
                ),
              )
          }
        }
      }
    }

    @Test
    fun `Get all tasks returns 200 when no type with only CAS1 assessments`() {
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
              isWithdrawn = true,
            )

            `Given an Assessment for Temporary Accommodation`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
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
              .uri("/tasks")
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
    fun `Get all tasks with taskType BookingAppeal returns 400`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
        webTestClient.get()
          .uri("/tasks?type=BookingAppeal")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isBadRequest
      }
    }

    @Test
    fun `Get all tasks returns 200 when type assessment`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->
            val (allocatableAssessment) = `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            webTestClient.get()
              .uri("/tasks?type=Assessment")
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
    fun `Get all tasks returns 200 when types is assessment`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->
            val (allocatableAssessment) = `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            webTestClient.get()
              .uri("/tasks?types=Assessment")
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
    fun `Get all tasks returns 200 when type assessment and ap area defined`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val apArea1 = `Given an AP Area`()
            val apArea2 = `Given an AP Area`()

            val (allocatableAssessment) = `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea1,
            )

            `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea2,
            )

            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea1,
            )

            webTestClient.get()
              .uri("/tasks?type=Assessment&apAreaId=${apArea1.id}")
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
    fun `Get all tasks returns 200 when type assessment and allocated to user defined`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val apArea1 = `Given an AP Area`()
            val apArea2 = `Given an AP Area`()

            val (allocatableAssessment) = `Given an Assessment for Approved Premises`(
              allocatedToUser = user,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea1,
            )

            `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea2,
            )

            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            webTestClient.get()
              .uri("/tasks?type=Assessment&allocatedToUserId=${user.id}")
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
    fun `Get all tasks returns 200 when type placement request`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val (allocatablePlacementRequest) = `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            webTestClient.get()
              .uri("/tasks?type=PlacementRequest")
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
    fun `Get all tasks returns 200 when types is placement request`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val (allocatablePlacementRequest) = `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            webTestClient.get()
              .uri("/tasks?types=PlacementRequest")
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
    fun `Get all tasks returns 200 when type placement application`() {
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

            `Given a Placement Request`(
              placementRequestAllocatedTo = null,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            webTestClient.get()
              .uri("/tasks?type=PlacementApplication")
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

    @Test
    fun `Get all tasks returns 200 when types is placement application`() {
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

            `Given a Placement Request`(
              placementRequestAllocatedTo = null,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            webTestClient.get()
              .uri("/tasks?types=PlacementApplication")
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

    @Test
    fun `Get all tasks returns 200 when type placement application and ap area defined`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val apArea1 = `Given an AP Area`()
            val apArea2 = `Given an AP Area`()

            val allocatablePlacementApplication = `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
              apArea = apArea1,
            )

            `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
              apArea = apArea2,
            )

            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea1,
            )

            webTestClient.get()
              .uri("/tasks?type=PlacementApplication&apAreaId=${apArea1.id}")
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

    @Test
    fun `Get all tasks returns 200 when type placement application and allocated to user defined`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val apArea1 = `Given an AP Area`()
            val apArea2 = `Given an AP Area`()

            val allocatablePlacementApplication = `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
              apArea = apArea1,
            )

            `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = otherUser,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
              apArea = apArea2,
            )

            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            webTestClient.get()
              .uri("/tasks?type=PlacementApplication&allocatedToUserId=${user.id}")
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

    @Test
    fun `Get all tasks returns 200 when type assessment and page is two and no allocated filter`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->
            val numAllocatableAssessment = 7
            val numUnallocatableAssessment = 5
            val totalTasks = numAllocatableAssessment + numUnallocatableAssessment

            repeat(numAllocatableAssessment) {
              `Given an Assessment for Approved Premises`(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numUnallocatableAssessment) {
              `Given an Assessment for Approved Premises`(
                null,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            `Given a Placement Request`(
              placementRequestAllocatedTo = null,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            webTestClient.get()
              .uri("/tasks?type=Assessment&page=2")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", totalTasks.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all tasks returns 200 when type assessment and page is two and allocated filter allocated`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->
            val numAllocatableAssessment = 12
            val numUnallocatableAssessment = 5

            repeat(numAllocatableAssessment) {
              `Given an Assessment for Approved Premises`(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numUnallocatableAssessment) {
              `Given an Assessment for Approved Premises`(
                null,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            webTestClient.get()
              .uri("/tasks?type=Assessment&page=2&allocatedFilter=allocated")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", numAllocatableAssessment.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all tasks returns 200 when type assessment and page is two and allocated filter unallocated`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->
            val numAllocatableAssessment = 2
            val numUnallocatableAssessment = 15

            repeat(numAllocatableAssessment) {
              `Given an Assessment for Approved Premises`(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numUnallocatableAssessment) {
              `Given an Assessment for Approved Premises`(
                null,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            `Given a Placement Request`(
              placementRequestAllocatedTo = null,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            webTestClient.get()
              .uri("/tasks?type=Assessment&page=2&allocatedFilter=unallocated")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", numUnallocatableAssessment.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all tasks returns 200 when type placement requests and page two no allocated filter`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val numAllocatedRequests = 8
            val numUnallocatedPlacementRequests = 6
            val totalTasks = numAllocatedRequests + numUnallocatedPlacementRequests

            repeat(numAllocatedRequests) {
              `Given a Placement Request`(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numUnallocatedPlacementRequests) {
              `Given a Placement Request`(
                null,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            webTestClient.get()
              .uri("/tasks?type=PlacementRequest&page=2")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", totalTasks.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all tasks returns 200 when type placement requests and ap area defined`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val apArea1 = `Given an AP Area`()
            val apArea2 = `Given an AP Area`()

            `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea1,
            )

            val (allocatablePlacementRequest) = `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea2,
            )

            `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea1,
            )

            webTestClient.get()
              .uri("/tasks?type=PlacementRequest&apAreaId=${apArea2.id}")
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
    fun `Get all tasks returns 200 when type placement requests and allocated to user defined`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val apArea1 = `Given an AP Area`()
            val apArea2 = `Given an AP Area`()

            `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea1,
            )

            val (allocatablePlacementRequest) = `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea2,
            )

            `Given an Assessment for Approved Premises`(
              allocatedToUser = user,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            webTestClient.get()
              .uri("/tasks?type=PlacementRequest&allocatedToUserId=${user.id}")
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
    fun `Get all tasks returns 200 when type placement requests and page two and allocated filter allocated`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val numAllocatedRequests = 12
            val numUnallocatedPlacementRequests = 5

            repeat(numAllocatedRequests) {
              `Given a Placement Request`(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numUnallocatedPlacementRequests) {
              `Given a Placement Request`(
                null,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            webTestClient.get()
              .uri("/tasks?type=PlacementRequest&page=2&allocatedFilter=allocated")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", numAllocatedRequests.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all tasks returns 200 when type placement requests and page two and allocated filter unallocated`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val numAllocatedRequests = 2
            val numUnallocatedPlacementRequests = 15

            repeat(numAllocatedRequests) {
              `Given a Placement Request`(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numUnallocatedPlacementRequests) {
              `Given a Placement Request`(
                null,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            `Given an Assessment for Approved Premises`(
              allocatedToUser = null,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            webTestClient.get()
              .uri("/tasks?type=PlacementRequest&page=2&allocatedFilter=unallocated")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", numUnallocatedPlacementRequests.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all tasks returns 200 when no type retains original sort order`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->
            val (task1, _) = `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            val (task2, _) = `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            val (task3, _) = `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            val task4 = `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
            )

            val (task5) = `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            val expectedTasks = listOf(
              taskTransformer.transformPlacementRequestToTask(
                task1,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
              taskTransformer.transformAssessmentToTask(
                task2,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
              taskTransformer.transformPlacementRequestToTask(
                task3,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
              taskTransformer.transformPlacementApplicationToTask(
                task4,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
              taskTransformer.transformAssessmentToTask(
                task5,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
            )

            webTestClient.get()
              .uri("/tasks?page=1&sortBy=createdAt&sortDirection=asc")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  expectedTasks,
                ),
              )

            webTestClient.get()
              .uri("/tasks?page=1&sortBy=createdAt&sortDirection=desc")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  expectedTasks.reversed(),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `Get all tasks returns 200 when no type and allocated to user defined`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { callingUser, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val (task1, _) = `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = callingUser,
              createdByUser = callingUser,
              crn = offenderDetails.otherIds.crn,
            )

            `Given a Placement Request`(
              placementRequestAllocatedTo = callingUser,
              assessmentAllocatedTo = callingUser,
              createdByUser = callingUser,
              crn = offenderDetails.otherIds.crn,
            )

            val (task2, _) = `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = callingUser,
              crn = offenderDetails.otherIds.crn,
            )

            `Given an Assessment for Approved Premises`(
              allocatedToUser = callingUser,
              createdByUser = callingUser,
              crn = offenderDetails.otherIds.crn,
            )

            val task3 = `Given a Placement Application`(
              createdByUser = otherUser,
              allocatedToUser = otherUser,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
            )

            `Given a Placement Application`(
              createdByUser = callingUser,
              allocatedToUser = callingUser,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
            )

            val expectedTasks = listOf(
              taskTransformer.transformPlacementRequestToTask(
                task1,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
              taskTransformer.transformAssessmentToTask(
                task2,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
              taskTransformer.transformPlacementApplicationToTask(
                task3,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
            )

            webTestClient.get()
              .uri("/tasks?page=1&sortBy=createdAt&sortDirection=asc&allocatedToUserId=${otherUser.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  expectedTasks,
                ),
              )
          }
        }
      }
    }

    @Test
    fun `Get all tasks returns 200 when no type and ap area defined`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val apArea1 = `Given an AP Area`()
            val apArea2 = `Given an AP Area`()

            val (task1, _) = `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea1,
            )

            `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea2,
            )

            val (task2, _) = `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea1,
            )

            `Given an Assessment for Approved Premises`(
              allocatedToUser = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea2,
            )

            val task3 = `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
              apArea = apArea1,
            )

            `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
              apArea = apArea2,
            )

            val expectedTasks = listOf(
              taskTransformer.transformPlacementRequestToTask(
                task1,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
              taskTransformer.transformAssessmentToTask(
                task2,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
              taskTransformer.transformPlacementApplicationToTask(
                task3,
                "${offenderDetails.firstName} ${offenderDetails.surname}",
              ),
            )

            webTestClient.get()
              .uri("/tasks?page=1&sortBy=createdAt&sortDirection=asc&apAreaId=${apArea1.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  expectedTasks,
                ),
              )
          }
        }
      }
    }

    @Test
    fun `Get all tasks returns 200 when type placement application and page two`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val numAllocatablePlacementApplications = 12

            repeat(numAllocatablePlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            `Given a Placement Request`(
              placementRequestAllocatedTo = null,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            webTestClient.get()
              .uri("/tasks?type=PlacementApplication&page=2")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", numAllocatablePlacementApplications.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all tasks returns 200 when type placement application and page two and no allocated filter`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val numAllocatedPlacementApplications = 6
            val numUnallocatedPlacementApplications = 5
            val totalTasks = numAllocatedPlacementApplications + numUnallocatedPlacementApplications

            repeat(numAllocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            repeat(numUnallocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            `Given a Placement Request`(
              placementRequestAllocatedTo = null,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            webTestClient.get()
              .uri("/tasks?type=PlacementApplication&page=2")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", totalTasks.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all tasks returns 200 when type placement application and page two and unallocated filter`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val numAllocatedPlacementApplications = 12
            val numUnallocatedPlacementApplications = 15

            repeat(numAllocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            repeat(numUnallocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            `Given a Placement Request`(
              placementRequestAllocatedTo = null,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            webTestClient.get()
              .uri("/tasks?type=PlacementApplication&page=2&allocatedFilter=unallocated")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", numUnallocatedPlacementApplications.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all tasks returns 200 when type placement application and page two and allocated filter`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val numAllocatedPlacementApplications = 12
            val numUnallocatedPlacementApplications = 15

            repeat(numAllocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            repeat(numUnallocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            )

            webTestClient.get()
              .uri("/tasks?type=PlacementApplication&page=2&allocatedFilter=allocated")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", numAllocatedPlacementApplications.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all tasks returns 200 when no type and page two and no allocated filter`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val numAllocatedPlacementApplications = 2
            val numUnallocatedPlacementApplications = 3
            val numAllocatableAssessment = 4
            val numUnallocatableAssessment = 5
            val numAllocatedPlacementRequests = 6
            val numUnallocatedPlacementRequests = 7

            val totalTasks = numAllocatedPlacementApplications +
              numUnallocatedPlacementApplications +
              numAllocatableAssessment +
              numUnallocatableAssessment +
              numAllocatedPlacementRequests +
              numUnallocatedPlacementRequests

            repeat(numAllocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            repeat(numUnallocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            repeat(numAllocatableAssessment) {
              `Given an Assessment for Approved Premises`(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numUnallocatableAssessment) {
              `Given an Assessment for Approved Premises`(
                null,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numAllocatedPlacementRequests) {
              `Given a Placement Request`(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numUnallocatedPlacementRequests) {
              `Given a Placement Request`(
                null,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            webTestClient.get()
              .uri("/tasks?page=2")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 3)
              .expectHeader().valueEquals("X-Pagination-TotalResults", totalTasks.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all tasks returns 200 when no type and page two and allocated filter`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val numAllocatedPlacementApplications = 12
            val numUnallocatedPlacementApplications = 1
            val numAllocatableAssessment = 4
            val numUnallocatableAssessment = 1
            val numAllocatedPlacementRequests = 6
            val numUnallocatedPlacementRequests = 1

            val totalTasks = numAllocatedPlacementApplications +
              numAllocatableAssessment +
              numAllocatedPlacementRequests

            repeat(numAllocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            repeat(numUnallocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            repeat(numAllocatableAssessment) {
              `Given an Assessment for Approved Premises`(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numUnallocatableAssessment) {
              `Given an Assessment for Approved Premises`(
                null,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numAllocatedPlacementRequests) {
              `Given a Placement Request`(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numUnallocatedPlacementRequests) {
              `Given a Placement Request`(
                null,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            webTestClient.get()
              .uri("/tasks?page=2&allocatedFilter=allocated")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 3)
              .expectHeader().valueEquals("X-Pagination-TotalResults", totalTasks.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get all tasks returns 200 when no type and page two and unallocated filter`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val numAllocatedPlacementApplications = 1
            val numAllocatedAssessments = 1
            val numAllocatedPlacementRequests = 1

            repeat(numAllocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            repeat(numAllocatedAssessments) {
              `Given an Assessment for Approved Premises`(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numAllocatedPlacementRequests) {
              `Given a Placement Request`(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            val numUnallocatedPlacementApplications = 11
            val numUnallocatedAssessments = 5
            val numUnallocatedPlacementRequests = 7

            repeat(numUnallocatedPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
                reallocated = false,
                decision = null,
              )
            }

            repeat(numUnallocatedAssessments) {
              `Given an Assessment for Approved Premises`(
                null,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            repeat(numUnallocatedPlacementRequests) {
              `Given a Placement Request`(
                null,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )
            }

            val totalUnallocatedTasks = numUnallocatedPlacementApplications +
              numUnallocatedAssessments +
              numUnallocatedPlacementRequests

            webTestClient.get()
              .uri("/tasks?page=2&allocatedFilter=unallocated")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 3)
              .expectHeader().valueEquals("X-Pagination-TotalResults", totalUnallocatedTasks.toLong())
              .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
              .expectBody()
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
              roles = listOf(UserRole.CAS1_MATCHER),
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
                          task = taskTransformer.transformAssessmentToTask(
                            assessment,
                            "${offenderDetails.firstName} ${offenderDetails.surname}",
                          ),
                          users = listOf(
                            userTransformer.transformJpaToAPIUserWithWorkload(
                              allocatableUser,
                              UserWorkload(
                                0,
                                0,
                                0,
                              ),
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
    }

    @Test
    fun `Get a Placement Request Task for an application returns 200`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
        `Given a User`(
          roles = listOf(UserRole.CAS1_ASSESSOR),
        ) { user, _ ->
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
                        task = taskTransformer.transformPlacementRequestToTask(
                          placementRequest,
                          "${offenderDetails.firstName} ${offenderDetails.surname}",
                        ),
                        users = listOf(
                          userTransformer.transformJpaToAPIUserWithWorkload(
                            allocatableUser,
                            UserWorkload(0, 0, 0),
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
    fun `Get a Placement Application Task for an application returns 200`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
        `Given a User`(
          roles = listOf(UserRole.CAS1_ADMIN),
        ) { user, _ ->
          `Given a User`(
            roles = listOf(UserRole.CAS1_MATCHER),
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
                          userTransformer.transformJpaToAPIUserWithWorkload(
                            allocatableUser,
                            UserWorkload(0, 0, 0),
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
    fun `Get a Placement Application Task for an application returns 2 users with correct roles`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
        `Given a User`(
          roles = listOf(UserRole.CAS1_MATCHER),
        ) { user, _ ->
          `Given a User`(
            roles = listOf(UserRole.CAS1_MATCHER),
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
                          userTransformer.transformJpaToAPIUserWithWorkload(
                            user,
                            UserWorkload(1, 0, 0),
                          ),
                          userTransformer.transformJpaToAPIUserWithWorkload(
                            allocatableUser,
                            UserWorkload(0, 0, 0),
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
    fun `Get a PlacementApplication Task for an application gets UserWithWorkload, ignores inactive users and returns 200`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
        `Given a User` { user, _ ->
          `Given a User`(
            roles = listOf(UserRole.CAS1_MATCHER),
          ) { allocatableUser, _ ->
            `Given a User`(
              roles = listOf(UserRole.CAS1_MATCHER),
              isActive = false,
            ) { _, _ ->
              `Given an Offender` { offenderDetails, inmateDetails ->
                `Given a Placement Application`(
                  createdByUser = user,
                  allocatedToUser = user,
                  schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                    withPermissiveSchema()
                  },
                  crn = offenderDetails.otherIds.crn,

                ) { placementApplication ->
                  val numAssessmentsPending = 3
                  repeat(numAssessmentsPending) {
                    createTask(TaskType.assessment, null, allocatableUser, user, offenderDetails.otherIds.crn)
                  }

                  val numPlacementApplicationsPending = 4
                  repeat(numPlacementApplicationsPending) {
                    createTask(TaskType.placementApplication, null, allocatableUser, user, offenderDetails.otherIds.crn)
                  }

                  val numPlacementRequestsPending = 2
                  repeat(numPlacementRequestsPending) {
                    createTask(TaskType.placementRequest, null, allocatableUser, user, offenderDetails.otherIds.crn)
                  }

                  val numAssessmentsCompletedBetween1And7DaysAgo = 4
                  repeat(numAssessmentsCompletedBetween1And7DaysAgo) {
                    val days = kotlin.random.Random.nextInt(1, 7).toLong()
                    createTask(TaskType.assessment, OffsetDateTime.now().minusDays(days), allocatableUser, user, offenderDetails.otherIds.crn)
                  }

                  val numPlacementApplicationsCompletedBetween1And7DaysAgo = 2
                  repeat(numPlacementApplicationsCompletedBetween1And7DaysAgo) {
                    val days = kotlin.random.Random.nextInt(1, 7).toLong()
                    createTask(TaskType.placementApplication, OffsetDateTime.now().minusDays(days), allocatableUser, user, offenderDetails.otherIds.crn)
                  }

                  val numPlacementRequestsCompletedBetween1And7DaysAgo = 1
                  repeat(numPlacementRequestsCompletedBetween1And7DaysAgo) {
                    val days = kotlin.random.Random.nextInt(1, 7).toLong()
                    createTask(TaskType.placementRequest, OffsetDateTime.now().minusDays(days), allocatableUser, user, offenderDetails.otherIds.crn)
                  }

                  val numAssessmentsCompletedBetween8And30DaysAgo = 4
                  repeat(numAssessmentsCompletedBetween8And30DaysAgo) {
                    val days = kotlin.random.Random.nextInt(8, 30).toLong()
                    createTask(TaskType.assessment, OffsetDateTime.now().minusDays(days), allocatableUser, user, offenderDetails.otherIds.crn)
                  }

                  val numPlacementApplicationsCompletedBetween8And30DaysAgo = 3
                  repeat(numPlacementApplicationsCompletedBetween8And30DaysAgo) {
                    val days = kotlin.random.Random.nextInt(8, 30).toLong()
                    createTask(TaskType.placementApplication, OffsetDateTime.now().minusDays(days), allocatableUser, user, offenderDetails.otherIds.crn)
                  }

                  val numPlacementRequestsCompletedBetween8And30DaysAgo = 2
                  repeat(numPlacementRequestsCompletedBetween8And30DaysAgo) {
                    val days = kotlin.random.Random.nextInt(8, 30).toLong()
                    createTask(TaskType.placementRequest, OffsetDateTime.now().minusDays(days), allocatableUser, user, offenderDetails.otherIds.crn)
                  }

                  val numPendingTasks = listOf(
                    numAssessmentsPending,
                    numPlacementRequestsPending,
                    numPlacementApplicationsPending,
                  ).sum()
                  val numTasksCompletedInTheLast7Days = listOf(
                    numAssessmentsCompletedBetween1And7DaysAgo,
                    numPlacementApplicationsCompletedBetween1And7DaysAgo,
                    numPlacementRequestsCompletedBetween1And7DaysAgo,
                  ).sum()
                  val numTasksCompletedInTheLast30Days = listOf(
                    numTasksCompletedInTheLast7Days,
                    numAssessmentsCompletedBetween8And30DaysAgo,
                    numPlacementApplicationsCompletedBetween8And30DaysAgo,
                    numPlacementRequestsCompletedBetween8And30DaysAgo,
                  ).sum()

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
                            userTransformer.transformJpaToAPIUserWithWorkload(
                              allocatableUser,
                              UserWorkload(
                                numPendingTasks,
                                numTasksCompletedInTheLast7Days,
                                numTasksCompletedInTheLast30Days,
                              ),
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

    private fun createTask(taskType: TaskType, completedAt: OffsetDateTime?, allocatedUser: UserEntity, createdByUser: UserEntity, crn: String) {
      (
        when (taskType) {
          TaskType.assessment -> {
            if (completedAt != null) {
              `Given an Assessment for Approved Premises`(
                allocatedToUser = allocatedUser,
                createdByUser = createdByUser,
                crn = crn,
                decision = null,
                reallocated = false,
                submittedAt = completedAt,
              )
            } else {
              `Given an Assessment for Approved Premises`(
                allocatedToUser = allocatedUser,
                createdByUser = createdByUser,
                crn = crn,
                decision = null,
                reallocated = false,
                submittedAt = null,
              )
            }
          }
          TaskType.placementRequest -> {
            val booking = if (completedAt != null) {
              val premises = approvedPremisesEntityFactory.produceAndPersist {
                withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
                withProbationRegion(createdByUser.probationRegion)
              }
              bookingEntityFactory.produceAndPersist {
                withPremises(premises)
                withCreatedAt(completedAt)
              }
            } else {
              null
            }

            `Given a Placement Request`(
              placementRequestAllocatedTo = allocatedUser,
              assessmentAllocatedTo = createdByUser,
              createdByUser = createdByUser,
              crn = crn,
              booking = booking,
            )
          }
          TaskType.placementApplication -> {
            `Given a Placement Application`(
              createdByUser = createdByUser,
              allocatedToUser = allocatedUser,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              submittedAt = completedAt,
              crn = crn,
            )
          }
          else -> {
            null
          }
        }
        )
    }
  }

  @Nested
  inner class GetTaskTaskTypeTest {
    @Test
    fun `Get placement application tasks without JWT returns 401`() {
      webTestClient.get()
        .uri("/tasks/placement-application")
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
    fun `Get an non-implemented task type for an application returns 405`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MATCHER)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          `Given an Assessment for Approved Premises`(
            allocatedToUser = user,
            createdByUser = user,
            crn = offenderDetails.otherIds.crn,
          ) { _, _ ->
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

    @Test
    fun `Get tasks by taskType for a user returns the relevant placement requests tasks for a user`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MATCHER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->
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
              submittedAt = OffsetDateTime.now(),
            )

            `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              reallocated = true,
              submittedAt = OffsetDateTime.now(),
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
    fun `Get tasks by taskType for a user returns the relevant placement requests tasks for a user page 2`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MATCHER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->
            val numberOfPlacementRequests = 12
            repeat(numberOfPlacementRequests) {
              `Given a Placement Request`(
                placementRequestAllocatedTo = user,
                assessmentAllocatedTo = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )
            }

            `Given a Placement Application`(
              createdByUser = otherUser,
              allocatedToUser = otherUser,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
            )

            `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              reallocated = true,
              submittedAt = OffsetDateTime.now(),
            )

            webTestClient.get()
              .uri("/tasks/placement-request?page=2")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", 12)
              .expectHeader().valueEquals("X-Pagination-PageSize", 10)
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get tasks by taskType for a user returns the relevant placement requests tasks for a user and ap area`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MATCHER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            val apArea1 = `Given an AP Area`()
            val apArea2 = `Given an AP Area`()

            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea1,
            )

            val (apArea2AllocatedToMe, _) = `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea2,
            )

            `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
              apArea = apArea1,
            )

            webTestClient.get()
              .uri("/tasks/placement-request?apAreaId=${apArea2.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(
                    taskTransformer.transformPlacementRequestToTask(
                      apArea2AllocatedToMe,
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
          `Given an Offender` { offenderDetails, _ ->

            val placementApplicationAllocatedToMe = `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              decision = null,
              submittedAt = OffsetDateTime.now(),
            )

            `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              decision = PlacementApplicationDecision.ACCEPTED,
              submittedAt = OffsetDateTime.now(),
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
              submittedAt = OffsetDateTime.now(),
            )

            `Given a Placement Application`(
              createdByUser = user,
              allocatedToUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              crn = offenderDetails.otherIds.crn,
              reallocated = true,
              submittedAt = OffsetDateTime.now(),
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
    fun `Get tasks by taskType for a user returns the relevant placement application tasks for a user page 2`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MATCHER)) { user, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->

            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn,
            )

            val approvedPremisesPlacementApplicationJsonSchema =
              approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              }
            val numberOfPlacementApplications = 12
            repeat(numberOfPlacementApplications) {
              `Given a Placement Application`(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchema,
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )
            }

            webTestClient.get()
              .uri("/tasks/placement-application?page=2")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", 12)
              .expectHeader().valueEquals("X-Pagination-PageSize", 10)
              .expectBody()
          }
        }
      }
    }

    @Test
    fun `Get tasks by taskType for a user returns the relevant placement applications tasks for a user and ap area`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MATCHER)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->

          val apArea1 = `Given an AP Area`()
          val apArea2 = `Given an AP Area`()

          val placementApplicationAllocatedToMeInApArea1 = `Given a Placement Application`(
            createdByUser = user,
            allocatedToUser = user,
            schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
            crn = offenderDetails.otherIds.crn,
            decision = null,
            apArea = apArea1,
            submittedAt = OffsetDateTime.now(),
          )

          `Given a Placement Application`(
            createdByUser = user,
            allocatedToUser = user,
            schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
            crn = offenderDetails.otherIds.crn,
            decision = null,
            apArea = apArea2,
            submittedAt = OffsetDateTime.now(),
          )

          `Given a Placement Application`(
            createdByUser = user,
            allocatedToUser = user,
            schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
            crn = offenderDetails.otherIds.crn,
            reallocated = true,
            apArea = apArea1,
            submittedAt = OffsetDateTime.now(),
          )

          webTestClient.get()
            .uri("/tasks/placement-application?apAreaId=${apArea1.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                listOf(
                  taskTransformer.transformPlacementApplicationToTask(
                    placementApplicationAllocatedToMeInApArea1,
                    "${offenderDetails.firstName} ${offenderDetails.surname}",
                  ),
                ),
              ),
            )
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
                        user = userTransformer.transformJpaToApi(
                          assigneeUser,
                          ServiceName.approvedPremises,
                        ) as ApprovedPremisesUser,
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
                      user = userTransformer.transformJpaToApi(
                        assigneeUser,
                        ServiceName.approvedPremises,
                      ) as ApprovedPremisesUser,
                      taskType = TaskType.placementRequest,
                    ),
                  ),
                )

              val placementRequests = placementRequestRepository.findAll()
              val allocatedPlacementRequest = placementRequests.find { it.allocatedToUser!!.id == assigneeUser.id }

              Assertions.assertThat(placementRequests.first { it.id == existingPlacementRequest.id }.reallocatedAt).isNotNull
              Assertions.assertThat(allocatedPlacementRequest).isNotNull

              val desirableCriteria =
                allocatedPlacementRequest!!.placementRequirements.desirableCriteria.map { it.propertyName }
              val essentialCriteria =
                allocatedPlacementRequest!!.placementRequirements.essentialCriteria.map { it.propertyName }

              Assertions.assertThat(desirableCriteria)
                .isEqualTo(existingPlacementRequest.placementRequirements.desirableCriteria.map { it.propertyName })
              Assertions.assertThat(essentialCriteria)
                .isEqualTo(existingPlacementRequest.placementRequirements.essentialCriteria.map { it.propertyName })
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
                        user = userTransformer.transformJpaToApi(
                          assigneeUser,
                          ServiceName.approvedPremises,
                        ) as ApprovedPremisesUser,
                        taskType = TaskType.placementApplication,
                      ),
                    ),
                  )

                val placementApplications = placementApplicationRepository.findAll()
                val allocatedPlacementApplication =
                  placementApplications.find { it.allocatedToUser!!.id == assigneeUser.id }

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

            val assessment =
              temporaryAccommodationAssessmentRepository.findAll().first { it.id == existingAssessment.id }

            Assertions.assertThat(assessment.allocatedToUser).isNull()
            Assertions.assertThat(assessment.allocatedAt).isNull()
          }
        }
      }
    }
  }
}
