package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.UserWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.TaskTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.math.ceil

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
  inner class GetTasksTest {
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

    @Nested
    inner class FilterByType {
      private lateinit var tasks: Map<TaskType, List<Task>>
      lateinit var jwt: String

      @BeforeEach
      fun setup() {
        `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
          `Given a User` { otherUser, _ ->
            `Given an Offender` { offenderDetails, _ ->
              this.jwt = jwt

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

              val placementRequests = listOf(
                taskTransformer.transformPlacementRequestToTask(
                  task1,
                  "${offenderDetails.firstName} ${offenderDetails.surname}",
                ),
                taskTransformer.transformPlacementRequestToTask(
                  task3,
                  "${offenderDetails.firstName} ${offenderDetails.surname}",
                ),
              )

              val placementApplications = listOf(
                taskTransformer.transformPlacementApplicationToTask(
                  task4,
                  "${offenderDetails.firstName} ${offenderDetails.surname}",
                ),
              )

              val assessments = listOf(
                taskTransformer.transformAssessmentToTask(
                  task2,
                  "${offenderDetails.firstName} ${offenderDetails.surname}",
                ),
                taskTransformer.transformAssessmentToTask(
                  task5,
                  "${offenderDetails.firstName} ${offenderDetails.surname}",
                ),
              )

              tasks = mapOf(
                TaskType.assessment to assessments,
                TaskType.placementApplication to placementApplications,
                TaskType.placementRequest to placementRequests,
              )
            }
          }
        }
      }

      @ParameterizedTest
      @EnumSource(value = TaskType::class, names = ["assessment", "placementRequest", "placementApplication"])
      fun `Get all tasks filters by a single type`(taskType: TaskType) {
        val url = "/tasks?page=1&sortBy=createdAt&sortDirection=asc&types=${taskType.value}"
        val expectedTasks = tasks[taskType]!!.sortedBy { it.dueDate }

        webTestClient.get()
          .uri(url)
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

      @ParameterizedTest
      @CsvSource("assessment,placementRequest", "assessment,placementApplication", "placementRequest,placementApplication", "placementApplication,placementRequest")
      fun `Get all tasks filters by multiple types`(taskType1: TaskType, taskType2: TaskType) {
        val url = "/tasks?page=1&sortBy=createdAt&sortDirection=asc&types=${taskType1.value}&types=${taskType2.value}"
        val expectedTasks = listOf(
          tasks[taskType1]!!,
          tasks[taskType2]!!,
        ).flatten().sortedBy { it.dueDate }

        webTestClient.get()
          .uri(url)
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

      @Test
      fun `Get all tasks returns all task types`() {
        val url = "/tasks?page=1&sortBy=createdAt&sortDirection=asc&types=Assessment&types=PlacementRequest&types=PlacementApplication"
        val expectedTasks = listOf(
          tasks[TaskType.assessment]!!,
          tasks[TaskType.placementRequest]!!,
          tasks[TaskType.placementApplication]!!,
        ).flatten().sortedBy { it.dueDate }

        webTestClient.get()
          .uri(url)
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

      @Test
      fun `Get all tasks returns all task types by default`() {
        val url = "/tasks?page=1&sortBy=createdAt&sortDirection=asc"
        val expectedTasks = listOf(
          tasks[TaskType.assessment]!!,
          tasks[TaskType.placementRequest]!!,
          tasks[TaskType.placementApplication]!!,
        ).flatten().sortedBy { it.dueDate }

        webTestClient.get()
          .uri(url)
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

    @Nested
    inner class FilterByApArea {
      private lateinit var tasks: Map<TaskType, List<Task>>

      lateinit var jwt: String
      lateinit var apArea: ApAreaEntity

      @BeforeEach
      fun setup() {
        `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
          `Given a User` { otherUser, _ ->
            `Given an Offender` { offenderDetails, _ ->
              this.jwt = jwt

              apArea = `Given an AP Area`()
              val apArea2 = `Given an AP Area`()

              val (assessment) = `Given an Assessment for Approved Premises`(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
                apArea = apArea,
              )

              `Given an Assessment for Approved Premises`(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
                apArea = apArea2,
              )

              val placementApplication = `Given a Placement Application`(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
                apArea = apArea,
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

              val (placementRequest) = `Given a Placement Request`(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
                apArea = apArea,
              )

              `Given a Placement Request`(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
                apArea = apArea2,
              )

              val assessments = listOf(
                taskTransformer.transformAssessmentToTask(
                  assessment,
                  "${offenderDetails.firstName} ${offenderDetails.surname}",
                ),
              )

              val placementApplications = listOf(
                taskTransformer.transformPlacementApplicationToTask(
                  placementApplication,
                  "${offenderDetails.firstName} ${offenderDetails.surname}",
                ),
              )

              val placementRequests = listOf(
                taskTransformer.transformPlacementRequestToTask(
                  placementRequest,
                  "${offenderDetails.firstName} ${offenderDetails.surname}",
                ),
              )

              tasks = mapOf(
                TaskType.assessment to assessments,
                TaskType.placementApplication to placementApplications,
                TaskType.placementRequest to placementRequests,
              )
            }
          }
        }
      }

      @ParameterizedTest
      @EnumSource(value = TaskType::class, names = ["assessment", "placementRequest", "placementApplication"])
      fun `it filters by Ap Area and task type`(taskType: TaskType) {
        val expectedTasks = tasks[taskType]
        val url = "/tasks?type=${taskType.value}&apAreaId=${apArea.id}"

        webTestClient.get()
          .uri(url)
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

      @Test
      fun `it filters by all areas with no task type`() {
        val expectedTasks = listOf(
          tasks[TaskType.assessment]!!,
          tasks[TaskType.placementRequest]!!,
          tasks[TaskType.placementApplication]!!,
        ).flatten().sortedBy { it.dueDate }

        webTestClient.get()
          .uri("/tasks?apAreaId=${apArea.id}")
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

    @Nested
    inner class FilterByUser {
      private lateinit var tasks: Map<TaskType, List<Task>>

      lateinit var jwt: String
      lateinit var user: UserEntity

      @BeforeEach
      fun setup() {
        `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
          `Given a User` { otherUser, _ ->
            `Given an Offender` { offenderDetails, _ ->
              this.jwt = jwt
              this.user = user

              val (allocatableAssessment) = `Given an Assessment for Approved Premises`(
                allocatedToUser = user,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )

              `Given an Assessment for Approved Premises`(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )

              val allocatablePlacementApplication = `Given a Placement Application`(
                createdByUser = user,
                allocatedToUser = user,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )

              `Given a Placement Application`(
                createdByUser = user,
                allocatedToUser = otherUser,
                schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                  withPermissiveSchema()
                },
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )

              `Given a Placement Request`(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )

              val (allocatablePlacementRequest) = `Given a Placement Request`(
                placementRequestAllocatedTo = user,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
              )

              val assessments = listOf(
                taskTransformer.transformAssessmentToTask(
                  allocatableAssessment,
                  "${offenderDetails.firstName} ${offenderDetails.surname}",
                ),
              )

              val placementApplications = listOf(
                taskTransformer.transformPlacementApplicationToTask(
                  allocatablePlacementApplication,
                  "${offenderDetails.firstName} ${offenderDetails.surname}",
                ),
              )

              val placementRequests = listOf(
                taskTransformer.transformPlacementRequestToTask(
                  allocatablePlacementRequest,
                  "${offenderDetails.firstName} ${offenderDetails.surname}",
                ),
              )

              tasks = mapOf(
                TaskType.assessment to assessments,
                TaskType.placementApplication to placementApplications,
                TaskType.placementRequest to placementRequests,
              )
            }
          }
        }
      }

      @ParameterizedTest
      @EnumSource(value = TaskType::class, names = ["assessment", "placementRequest", "placementApplication"])
      fun `it filters by user and task type`(taskType: TaskType) {
        val expectedTasks = tasks[taskType]
        val url = "/tasks?type=${taskType.value}&allocatedToUserId=${user.id}"

        webTestClient.get()
          .uri(url)
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

      @Test
      fun `it filters by user with all tasks`() {
        val expectedTasks = listOf(
          tasks[TaskType.assessment]!!,
          tasks[TaskType.placementRequest]!!,
          tasks[TaskType.placementApplication]!!,
        ).flatten().sortedBy { it.dueDate }

        val url = "/tasks?allocatedToUserId=${user.id}"

        webTestClient.get()
          .uri(url)
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

    @Nested
    inner class Pagination {
      private val pageSize = 1
      private lateinit var counts: Map<TaskType, Map<String, Int>>

      lateinit var jwt: String

      @BeforeEach
      fun setup() {
        `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
          `Given a User` { otherUser, _ ->
            `Given an Offender` { offenderDetails, _ ->
              this.jwt = jwt

              counts = mapOf(
                TaskType.assessment to mapOf(
                  "allocated" to 2,
                  "unallocated" to 3,
                ),
                TaskType.placementRequest to mapOf(
                  "allocated" to 2,
                  "unallocated" to 4,
                ),
                TaskType.placementApplication to mapOf(
                  "allocated" to 3,
                  "unallocated" to 2,
                ),
              )

              repeat(counts[TaskType.assessment]!!["allocated"]!!) {
                `Given an Assessment for Approved Premises`(
                  allocatedToUser = otherUser,
                  createdByUser = otherUser,
                  crn = offenderDetails.otherIds.crn,
                )
              }

              repeat(counts[TaskType.assessment]!!["unallocated"]!!) {
                `Given an Assessment for Approved Premises`(
                  null,
                  createdByUser = otherUser,
                  crn = offenderDetails.otherIds.crn,
                )
              }

              repeat(counts[TaskType.placementRequest]!!["allocated"]!!) {
                `Given a Placement Request`(
                  placementRequestAllocatedTo = otherUser,
                  assessmentAllocatedTo = otherUser,
                  createdByUser = user,
                  crn = offenderDetails.otherIds.crn,
                )
              }

              repeat(counts[TaskType.placementRequest]!!["unallocated"]!!) {
                `Given a Placement Request`(
                  null,
                  assessmentAllocatedTo = otherUser,
                  createdByUser = user,
                  crn = offenderDetails.otherIds.crn,
                )
              }

              repeat(counts[TaskType.placementApplication]!!["allocated"]!!) {
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

              repeat(counts[TaskType.placementApplication]!!["unallocated"]!!) {
                `Given a Placement Application`(
                  createdByUser = user,
                  schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                    withPermissiveSchema()
                  },
                  crn = offenderDetails.otherIds.crn,
                  submittedAt = OffsetDateTime.now(),
                )
              }
            }
          }
        }
      }

      @ParameterizedTest
      @CsvSource(
        "assessment,allocated,1", "assessment,allocated,2", "assessment,unallocated,1", "assessment,unallocated,1",
        "placementRequest,allocated,1", "placementRequest,allocated,2", "placementRequest,unallocated,1", "placementRequest,unallocated,2",
        "placementApplication,allocated,1", "placementApplication,allocated,2", "placementApplication,unallocated,1", "placementApplication,unallocated,2",
      )
      fun `get all tasks returns page counts when taskType and allocated filter are set`(taskType: TaskType, allocatedFilter: String, pageNumber: String) {
        val itemCount = counts[taskType]!![allocatedFilter]!!
        val url = "/tasks?type=${taskType.value}&perPage=$pageSize&page=$pageNumber&allocatedFilter=$allocatedFilter"

        expectCountHeaders(url, pageNumber.toInt(), itemCount)
      }

      @ParameterizedTest
      @CsvSource(
        "allocated,1",
        "allocated,2",
        "unallocated,1",
        "unallocated,1",
      )
      fun `get all tasks returns page counts for all tasks when allocated filter is set`(allocatedFilter: String, pageNumber: String) {
        val itemCount = listOf(
          counts[TaskType.assessment]!![allocatedFilter]!!,
          counts[TaskType.placementRequest]!![allocatedFilter]!!,
          counts[TaskType.placementApplication]!![allocatedFilter]!!,
        ).sum()

        val url = "/tasks?&page=$pageNumber&perPage=$pageSize&allocatedFilter=$allocatedFilter"

        expectCountHeaders(url, pageNumber.toInt(), itemCount)
      }

      @ParameterizedTest
      @ValueSource(ints = [1, 2])
      fun `get all tasks returns page count when no allocated filter is set`(pageNumber: Int) {
        val itemCount = listOf(
          counts[TaskType.assessment]!!["allocated"]!!,
          counts[TaskType.assessment]!!["unallocated"]!!,
          counts[TaskType.placementRequest]!!["allocated"]!!,
          counts[TaskType.placementRequest]!!["unallocated"]!!,
          counts[TaskType.placementApplication]!!["allocated"]!!,
          counts[TaskType.placementApplication]!!["unallocated"]!!,
        ).sum()

        expectCountHeaders("/tasks?&page=$pageNumber&perPage=$pageSize", pageNumber, itemCount)
      }

      private fun expectCountHeaders(url: String, pageNumber: Int, itemCount: Int) {
        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectHeader().valueEquals("X-Pagination-CurrentPage", pageNumber.toLong())
          .expectHeader().valueEquals("X-Pagination-TotalPages", expectedTotalPages(itemCount))
          .expectHeader().valueEquals("X-Pagination-TotalResults", itemCount.toLong())
          .expectHeader().valueEquals("X-Pagination-PageSize", pageSize.toLong())
      }

      private fun expectedTotalPages(count: Int) = ceil(count.toDouble() / pageSize).toLong()
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
