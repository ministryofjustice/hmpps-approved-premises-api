package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskWrapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForTemporaryAccommodation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision.ACCEPTED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision.REJECTED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.UserWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.TaskTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.ceil
import kotlin.random.Random

class Cas1TasksTest {

  private val baseUrls = listOf(
    "/tasks",
    "/cas1/tasks",
  )

  @Nested
  inner class GetTasksTest {

    @SuppressWarnings("LargeClass")
    @Nested
    inner class PermissionsTest : IntegrationTestBase() {

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      @BeforeEach
      fun stubBankHolidaysApi() {
        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks without JWT returns 401`(url: String) {
        webTestClient.get()
          .uri(url)
          .exchange()
          .expectStatus()
          .isUnauthorized
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks without cru member, matcher or assessor permissions returns 403`(url: String) {
        givenAUser { _, jwt ->
          webTestClient.get()
            .uri(url)
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER"])
      fun `Get all tasks returns 200 when have CAS1_CRU_MEMBER roles`(role: UserRole) {
        givenAUser(roles = listOf(role)) { _, jwt ->
          givenAUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->

              val offenderSummaries = getOffenderSummaries(offenderDetails)

              val task = givenAPlacementApplication(
                createdByUser = otherUser,
                allocatedToUser = otherUser,
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
                expectedArrival = LocalDate.now(),
                duration = 1,
              )

              val expectedTasks = listOf(
                taskTransformer.transformPlacementApplicationToTask(
                  task,
                  offenderSummaries,
                ),
              )
              baseUrls.forEach { url ->
                webTestClient.get()
                  .uri("$url?page=1&sortBy=createdAt&sortDirection=asc")
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
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks returns 200 when no type retains original sort order`(baseUrl: String) {
        givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
          givenAUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->

              val offenderSummaries = getOffenderSummaries(offenderDetails)

              val (task2, _) = givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )

              val task4 = givenAPlacementApplication(
                createdByUser = user,
                allocatedToUser = user,
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
                expectedArrival = LocalDate.now(),
                duration = 1,
              )

              val (task5) = givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )

              val expectedTasks = listOf(
                taskTransformer.transformAssessmentToTask(
                  task2,
                  offenderSummaries,
                ),
                taskTransformer.transformPlacementApplicationToTask(
                  task4,
                  offenderSummaries,
                ),
                taskTransformer.transformAssessmentToTask(
                  task5,
                  offenderSummaries,
                ),
              )

              webTestClient.get()
                .uri("$baseUrl?page=1&sortBy=createdAt&sortDirection=asc")
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
                .uri("$baseUrl?page=1&sortBy=createdAt&sortDirection=desc")
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
    inner class FilterByType : InitialiseDatabasePerClassTestBase() {
      private lateinit var tasks: Map<TaskType, List<Task>>
      lateinit var jwt: String

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      @BeforeAll
      fun stubBankHolidaysApi() {
        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()
      }

      @BeforeAll
      fun setup() {
        givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
          givenAUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              this.jwt = jwt

              val offenderSummaries = getOffenderSummaries(offenderDetails)

              val (task2, _) = givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )

              val task4 = givenAPlacementApplication(
                createdByUser = user,
                allocatedToUser = user,
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
                expectedArrival = LocalDate.now(),
                duration = 1,
              )

              val (task5) = givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )

              val placementApplications = listOf(
                taskTransformer.transformPlacementApplicationToTask(
                  task4,
                  offenderSummaries,
                ),
              )

              val assessments = listOf(
                taskTransformer.transformAssessmentToTask(
                  task2,
                  offenderSummaries,
                ),
                taskTransformer.transformAssessmentToTask(
                  task5,
                  offenderSummaries,
                ),
              )

              tasks = mapOf(
                TaskType.assessment to assessments,
                TaskType.placementApplication to placementApplications,
              )
            }
          }
        }
      }

      @ParameterizedTest
      @EnumSource(value = TaskType::class, names = ["assessment", "placementApplication"])
      fun `Get all tasks filters by a single type`(taskType: TaskType) {
        val expectedTasks = tasks[taskType]!!.sortedBy { it.dueDate }
        baseUrls.forEach { baseUrl ->
          val url = "$baseUrl?page=1&sortBy=createdAt&sortDirection=asc&types=${taskType.value}"

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

      @ParameterizedTest
      @CsvSource(
        "/tasks,assessment,placementApplication",
        "/cas1/tasks,assessment,placementApplication",
      )
      fun `Get all tasks filters by multiple types`(baseUrl: String, taskType1: TaskType, taskType2: TaskType) {
        val url = "$baseUrl?page=1&sortBy=createdAt&sortDirection=asc&types=${taskType1.value}&types=${taskType2.value}"
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

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks returns all task types`(baseUrl: String) {
        val url = "$baseUrl?page=1&sortBy=createdAt&sortDirection=asc&types=Assessment&types=PlacementApplication"
        val expectedTasks = listOf(
          tasks[TaskType.assessment]!!,
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

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks returns all task types by default`(baseUrl: String) {
        val url = "$baseUrl?page=1&sortBy=createdAt&sortDirection=asc"
        val expectedTasks = listOf(
          tasks[TaskType.assessment]!!,
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

    @Deprecated("Superseded by FilterByCruManagementArea")
    @Nested
    inner class FilterByApArea : InitialiseDatabasePerClassTestBase() {
      private lateinit var tasks: Map<TaskType, List<Task>>

      lateinit var jwt: String
      lateinit var apArea: ApAreaEntity

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      @BeforeAll
      fun setup() {
        givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
          givenAUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              this.jwt = jwt

              apArea = givenAnApArea()
              val apArea2 = givenAnApArea()

              val offenderSummaries = getOffenderSummaries(offenderDetails)

              val (assessment) = givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
                apArea = apArea,
              )

              givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
                apArea = apArea2,
              )

              val placementApplication = givenAPlacementApplication(
                createdByUser = user,
                allocatedToUser = user,
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
                apArea = apArea,
                expectedArrival = LocalDate.now(),
                duration = 1,
              )

              givenAPlacementApplication(
                createdByUser = user,
                allocatedToUser = user,
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
                apArea = apArea2,
                expectedArrival = LocalDate.now(),
                duration = 1,
              )

              val assessments = listOf(
                taskTransformer.transformAssessmentToTask(
                  assessment,
                  offenderSummaries,
                ),
              )

              val placementApplications = listOf(
                taskTransformer.transformPlacementApplicationToTask(
                  placementApplication,
                  offenderSummaries,
                ),
              )

              tasks = mapOf(
                TaskType.assessment to assessments,
                TaskType.placementApplication to placementApplications,
              )
            }
          }
        }
      }

      @ParameterizedTest
      @EnumSource(value = TaskType::class, names = ["assessment", "placementApplication"])
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
    inner class FilterByCruManagementArea : InitialiseDatabasePerClassTestBase() {
      private lateinit var tasks: Map<TaskType, List<Task>>

      lateinit var jwt: String
      lateinit var cruArea: Cas1CruManagementAreaEntity

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      @BeforeAll
      fun setup() {
        givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
          givenAUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              this.jwt = jwt

              cruArea = givenACas1CruManagementArea()
              val cruArea2 = givenACas1CruManagementArea()

              val offenderSummaries = getOffenderSummaries(offenderDetails)

              val (assessment) = givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
                cruManagementArea = cruArea,
              )

              givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
                cruManagementArea = cruArea2,
              )

              val placementApplication = givenAPlacementApplication(
                createdByUser = user,
                allocatedToUser = user,
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
                cruManagementArea = cruArea,
                expectedArrival = LocalDate.now(),
                duration = 1,
              )

              givenAPlacementApplication(
                createdByUser = user,
                allocatedToUser = user,
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
                cruManagementArea = cruArea2,
                expectedArrival = LocalDate.now(),
                duration = 1,
              )

              val assessments = listOf(
                taskTransformer.transformAssessmentToTask(
                  assessment,
                  offenderSummaries,
                ),
              )

              val placementApplications = listOf(
                taskTransformer.transformPlacementApplicationToTask(
                  placementApplication,
                  offenderSummaries,
                ),
              )

              tasks = mapOf(
                TaskType.assessment to assessments,
                TaskType.placementApplication to placementApplications,
              )
            }
          }
        }
      }

      @ParameterizedTest
      @EnumSource(value = TaskType::class, names = ["assessment", "placementApplication"])
      fun `it filters by CRU area and task type`(taskType: TaskType) {
        val expectedTasks = tasks[taskType]
        baseUrls.forEach { baseUrl ->
          val url = "$baseUrl?type=${taskType.value}&cruManagementAreaId=${cruArea.id}"

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

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `it filters by all areas with no task type`(baseUrl: String) {
        val expectedTasks = listOf(
          tasks[TaskType.assessment]!!,
          tasks[TaskType.placementApplication]!!,
        ).flatten().sortedBy { it.dueDate }

        webTestClient.get()
          .uri("$baseUrl?cruManagementAreaId=${cruArea.id}")
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
    inner class FilterByUser : InitialiseDatabasePerClassTestBase() {
      private lateinit var tasks: Map<TaskType, List<Task>>

      lateinit var jwt: String
      lateinit var user: UserEntity

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      @BeforeAll
      fun setup() {
        givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
          givenAUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              this.jwt = jwt
              this.user = user

              val offenderSummaries = getOffenderSummaries(offenderDetails)

              val (allocatableAssessment) = givenAnAssessmentForApprovedPremises(
                allocatedToUser = user,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )

              givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
              )

              val allocatablePlacementApplication = givenAPlacementApplication(
                createdByUser = user,
                allocatedToUser = user,
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
                expectedArrival = LocalDate.now(),
                duration = 1,
              )

              givenAPlacementApplication(
                createdByUser = user,
                allocatedToUser = otherUser,
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
                expectedArrival = LocalDate.now(),
                duration = 1,
              )

              val assessments = listOf(
                taskTransformer.transformAssessmentToTask(
                  allocatableAssessment,
                  offenderSummaries,
                ),
              )

              val placementApplications = listOf(
                taskTransformer.transformPlacementApplicationToTask(
                  allocatablePlacementApplication,
                  offenderSummaries,
                ),
              )

              tasks = mapOf(
                TaskType.assessment to assessments,
                TaskType.placementApplication to placementApplications,
              )
            }
          }
        }
      }

      @ParameterizedTest
      @EnumSource(value = TaskType::class, names = ["assessment", "placementApplication"])
      fun `it filters by user and task type`(taskType: TaskType) {
        val expectedTasks = tasks[taskType]
        baseUrls.forEach { baseUrl ->
          val url = "$baseUrl?type=${taskType.value}&allocatedToUserId=${user.id}"

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

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `it filters by user with all tasks`(baseUrl: String) {
        val expectedTasks = listOf(
          tasks[TaskType.assessment]!!,
          tasks[TaskType.placementApplication]!!,
        ).flatten().sortedBy { it.dueDate }

        val url = "$baseUrl?allocatedToUserId=${user.id}"

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
    inner class PaginationAndExclusionOfIrrelevantTasks : InitialiseDatabasePerClassTestBase() {
      private val pageSize = 1
      private lateinit var counts: Map<TaskType, Map<String, Int>>

      lateinit var jwt: String

      @BeforeAll
      fun setup() {
        givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
          givenAUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              this.jwt = jwt

              counts = mapOf(
                TaskType.assessment to mapOf(
                  "allocated" to 2,
                  "unallocated" to 3,
                ),
                TaskType.placementApplication to mapOf(
                  "allocated" to 3,
                  "unallocated" to 2,
                ),
              )

              givenAnAssessmentForTemporaryAccommodation(
                createdByUser = otherUser,
                allocatedToUser = null,
              )

              repeat(counts[TaskType.assessment]!!["allocated"]!!) {
                givenAnAssessmentForApprovedPremises(
                  allocatedToUser = otherUser,
                  createdByUser = otherUser,
                  crn = offenderDetails.otherIds.crn,
                )
              }

              repeat(counts[TaskType.assessment]!!["unallocated"]!!) {
                givenAnAssessmentForApprovedPremises(
                  null,
                  createdByUser = otherUser,
                  crn = offenderDetails.otherIds.crn,
                )
              }

              // withdrawn, ignored
              givenAnAssessmentForApprovedPremises(
                null,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
                isWithdrawn = true,
              )

              repeat(counts[TaskType.placementApplication]!!["allocated"]!!) {
                givenAPlacementApplication(
                  createdByUser = user,
                  allocatedToUser = user,
                  crn = offenderDetails.otherIds.crn,
                  submittedAt = OffsetDateTime.now(),
                  expectedArrival = LocalDate.now(),
                  duration = 1,
                )
              }

              repeat(counts[TaskType.placementApplication]!!["unallocated"]!!) {
                givenAPlacementApplication(
                  createdByUser = user,
                  crn = offenderDetails.otherIds.crn,
                  submittedAt = OffsetDateTime.now(),
                  expectedArrival = LocalDate.now(),
                  duration = 1,
                )
              }

              // withdrawn, ignored
              givenAPlacementApplication(
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
                isWithdrawn = true,
                expectedArrival = LocalDate.now(),
                duration = 1,
              )

              // automatic, ignored
              givenAPlacementApplication(
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
                expectedArrival = LocalDate.now(),
                duration = 1,
                automatic = true,
              )
            }
          }
        }
      }

      @ParameterizedTest
      @CsvSource(
        "/tasks,assessment,allocated,1",
        "/tasks,assessment,allocated,2",
        "/tasks,assessment,unallocated,1",
        "/tasks,assessment,unallocated,1",
        "/tasks,placementApplication,allocated,1",
        "/tasks,placementApplication,allocated,2",
        "/tasks,placementApplication,unallocated,1",
        "/tasks,placementApplication,unallocated,2",

        "/cas1/tasks,assessment,allocated,1",
        "/cas1/tasks,assessment,allocated,2",
        "/cas1/tasks,assessment,unallocated,1",
        "/cas1/tasks,assessment,unallocated,1",
        "/cas1/tasks,placementApplication,allocated,1",
        "/cas1/tasks,placementApplication,allocated,2",
        "/cas1/tasks,placementApplication,unallocated,1",
        "/cas1/tasks,placementApplication,unallocated,2",
      )
      fun `get all tasks returns page counts when taskType and allocated filter are set`(
        baseUrl: String,
        taskType: TaskType,
        allocatedFilter: String,
        pageNumber: String,
      ) {
        val itemCount = counts[taskType]!![allocatedFilter]!!
        val url = "$baseUrl?type=${taskType.value}&perPage=$pageSize&page=$pageNumber&allocatedFilter=$allocatedFilter"

        expectCountHeaders(url, pageNumber.toInt(), itemCount)
      }

      @ParameterizedTest
      @CsvSource(
        "/tasks,allocated,1",
        "/tasks,allocated,2",
        "/tasks,unallocated,1",
        "/tasks,unallocated,1",

        "/cas1/tasks,allocated,1",
        "/cas1/tasks,allocated,2",
        "/cas1/tasks,unallocated,1",
        "/cas1/tasks,unallocated,1",
      )
      fun `get all tasks returns page counts for all tasks when allocated filter is set`(
        baseUrl: String,
        allocatedFilter: String,
        pageNumber: String,
      ) {
        val itemCount = listOf(
          counts[TaskType.assessment]!![allocatedFilter]!!,
          counts[TaskType.placementApplication]!![allocatedFilter]!!,
        ).sum()

        val url = "$baseUrl?&page=$pageNumber&perPage=$pageSize&allocatedFilter=$allocatedFilter"

        expectCountHeaders(url, pageNumber.toInt(), itemCount)
      }

      @ParameterizedTest
      @CsvSource(
        "/tasks,1",
        "/tasks,2",

        "/cas1/tasks,1",
        "/cas1/tasks,2",
      )
      fun `get all tasks returns page count when no allocated filter is set`(baseUrl: String, pageNumber: Int) {
        val itemCount = listOf(
          counts[TaskType.assessment]!!["allocated"]!!,
          counts[TaskType.assessment]!!["unallocated"]!!,
          counts[TaskType.placementApplication]!!["allocated"]!!,
          counts[TaskType.placementApplication]!!["unallocated"]!!,
        ).sum()

        expectCountHeaders("$baseUrl?&page=$pageNumber&perPage=$pageSize", pageNumber, itemCount)
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

    @Nested
    inner class FilterQualification : InitialiseDatabasePerClassTestBase() {
      lateinit var jwt: String

      lateinit var tasks: Map<TaskType, Map<UserQualification, List<Task>>>

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      @BeforeAll
      fun setup() {
        givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
          givenAUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              this.jwt = jwt

              val offenderSummaries = getOffenderSummaries(offenderDetails)
              val assessmentTasks = mutableMapOf<UserQualification, List<Task>>()
              val placementApplicationTasks = mutableMapOf<UserQualification, List<Task>>()

              fun createAssessmentTask(
                requiredQualification: UserQualification?,
                noticeType: Cas1ApplicationTimelinessCategory? = Cas1ApplicationTimelinessCategory.standard,
              ): Task {
                val (assessment) = givenAnAssessmentForApprovedPremises(
                  allocatedToUser = otherUser,
                  createdByUser = otherUser,
                  crn = offenderDetails.otherIds.crn,
                  requiredQualification = requiredQualification,
                  noticeType = noticeType,
                )

                return taskTransformer.transformAssessmentToTask(
                  assessment,
                  offenderSummaries,
                )
              }

              fun createPlacementApplicationTask(
                requiredQualification: UserQualification?,
                noticeType: Cas1ApplicationTimelinessCategory? = Cas1ApplicationTimelinessCategory.standard,
              ): Task {
                val placementApplication = givenAPlacementApplication(
                  createdByUser = user,
                  allocatedToUser = user,
                  crn = offenderDetails.otherIds.crn,
                  submittedAt = OffsetDateTime.now(),
                  requiredQualification = requiredQualification,
                  noticeType = noticeType,
                  expectedArrival = LocalDate.now(),
                  duration = 1,
                )

                return taskTransformer.transformPlacementApplicationToTask(
                  placementApplication,
                  offenderSummaries,
                )
              }

              listOf(
                UserQualification.ESAP,
                UserQualification.PIPE,
                UserQualification.RECOVERY_FOCUSED,
                UserQualification.MENTAL_HEALTH_SPECIALIST,
              ).forEach { qualification ->
                assessmentTasks[qualification] = listOf(
                  createAssessmentTask(qualification),
                )
                placementApplicationTasks[qualification] = listOf(
                  createPlacementApplicationTask(qualification),
                )
              }

              assessmentTasks[UserQualification.EMERGENCY] = listOf(
                createAssessmentTask(null, Cas1ApplicationTimelinessCategory.shortNotice),
                createAssessmentTask(null, Cas1ApplicationTimelinessCategory.emergency),
              )
              placementApplicationTasks[UserQualification.EMERGENCY] = listOf(
                createPlacementApplicationTask(null, Cas1ApplicationTimelinessCategory.shortNotice),
                createPlacementApplicationTask(null, Cas1ApplicationTimelinessCategory.emergency),
              )

              tasks = mapOf(
                TaskType.assessment to assessmentTasks,
                TaskType.placementApplication to placementApplicationTasks,
              )
            }
          }
        }
      }

      @ParameterizedTest
      @CsvSource(
        "/tasks,assessment,PIPE",
        "/tasks,assessment,ESAP",
        "/tasks,assessment,EMERGENCY",
        "/tasks,assessment,RECOVERY_FOCUSED",
        "/tasks,assessment,MENTAL_HEALTH_SPECIALIST",

        "/tasks,placementApplication,PIPE",
        "/tasks,placementApplication,ESAP",
        "/tasks,placementApplication,EMERGENCY",
        "/tasks,placementApplication,RECOVERY_FOCUSED",
        "/tasks,placementApplication,MENTAL_HEALTH_SPECIALIST",

        "/cas1/tasks,assessment,PIPE",
        "/cas1/tasks,assessment,ESAP",
        "/cas1/tasks,assessment,EMERGENCY",
        "/cas1/tasks,assessment,RECOVERY_FOCUSED",
        "/cas1/tasks,assessment,MENTAL_HEALTH_SPECIALIST",

        "/cas1/tasks,placementApplication,PIPE",
        "/cas1/tasks,placementApplication,ESAP",
        "/cas1/tasks,placementApplication,EMERGENCY",
        "/cas1/tasks,placementApplication,RECOVERY_FOCUSED",
        "/cas1/tasks,placementApplication,MENTAL_HEALTH_SPECIALIST",
      )
      fun `Get all tasks filters by task type and required qualification`(
        baseUrl: String,
        taskType: TaskType,
        qualification: UserQualification,
      ) {
        val url = "$baseUrl?type=${taskType.value}&requiredQualification=${qualification.name.lowercase()}"
        val expectedTasks = tasks[taskType]!![qualification]!!

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
      @EnumSource(
        value = UserQualification::class,
        names = ["EMERGENCY", "ESAP", "PIPE", "RECOVERY_FOCUSED", "MENTAL_HEALTH_SPECIALIST"],
      )
      fun `Get all tasks required qualification`(qualification: UserQualification) {
        val expectedTasks = listOf(
          tasks[TaskType.assessment]!![qualification]!!,
          tasks[TaskType.placementApplication]!![qualification]!!,
        ).flatten()

        baseUrls.forEach { baseUrl ->
          val url = "$baseUrl?requiredQualification=${qualification.name.lowercase()}"

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
    }

    @Nested
    inner class FilterByNameOrCrn : InitialiseDatabasePerClassTestBase() {
      lateinit var jwt: String
      lateinit var crn: String

      private lateinit var nameMatchTasks: Map<TaskType, Task>
      private lateinit var crnMatchTasks: Map<TaskType, Task>

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      @BeforeAll
      fun setup() {
        givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
          givenAUser { otherUser, _ ->
            givenAnOffender { offenderDetails1, _ ->
              givenAnOffender { offenderDetails2, _ ->
                this.jwt = jwt
                this.crn = offenderDetails2.otherIds.crn

                val offenderSummaries1 = getOffenderSummaries(offenderDetails1)
                val offenderSummaries2 = getOffenderSummaries(offenderDetails2)
                val (assessment1, _) = givenAnAssessmentForApprovedPremises(
                  allocatedToUser = otherUser,
                  createdByUser = otherUser,
                  crn = offenderDetails1.otherIds.crn,
                  name = "SOMEONE",
                )

                val (assessment2, _) = givenAnAssessmentForApprovedPremises(
                  allocatedToUser = otherUser,
                  createdByUser = otherUser,
                  crn = offenderDetails2.otherIds.crn,
                  name = "ANOTHER",
                )

                val placementApplication1 = givenAPlacementApplication(
                  createdByUser = user,
                  allocatedToUser = user,
                  crn = offenderDetails1.otherIds.crn,
                  name = "SOMEONE",
                  submittedAt = OffsetDateTime.now(),
                  expectedArrival = LocalDate.now(),
                  duration = 1,
                )

                val placementApplication2 = givenAPlacementApplication(
                  createdByUser = user,
                  allocatedToUser = user,
                  crn = offenderDetails2.otherIds.crn,
                  submittedAt = OffsetDateTime.now(),
                  name = "ANOTHER",
                  expectedArrival = LocalDate.now(),
                  duration = 1,
                )

                nameMatchTasks = mapOf(
                  TaskType.assessment to taskTransformer.transformAssessmentToTask(
                    assessment1,
                    offenderSummaries1,
                  ),
                  TaskType.placementApplication to taskTransformer.transformPlacementApplicationToTask(
                    placementApplication1,
                    offenderSummaries1,
                  ),
                )

                crnMatchTasks = mapOf(
                  TaskType.assessment to taskTransformer.transformAssessmentToTask(
                    assessment2,
                    offenderSummaries2,
                  ),
                  TaskType.placementApplication to taskTransformer.transformPlacementApplicationToTask(
                    placementApplication2,
                    offenderSummaries2,
                  ),
                )
              }
            }
          }
        }
      }

      @ParameterizedTest
      @EnumSource(value = TaskType::class, names = ["assessment", "placementApplication"])
      fun `Get all tasks filters by name and task type`(taskType: TaskType) {
        baseUrls.forEach { baseUrl ->
          val url = "$baseUrl?type=${taskType.value}&crnOrName=someone"

          webTestClient.get()
            .uri(url)
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                listOf(nameMatchTasks[taskType]),
              ),
            )
        }
      }

      @ParameterizedTest
      @EnumSource(value = TaskType::class, names = ["assessment", "placementApplication"])
      fun `Get all tasks filters by CRN and task type`(taskType: TaskType) {
        baseUrls.forEach { baseUrl ->
          val url = "$baseUrl?type=${taskType.value}&crnOrName=$crn"

          webTestClient.get()
            .uri(url)
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                listOf(crnMatchTasks[taskType]),
              ),
            )
        }
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks filters by name without task type`(baseUrl: String) {
        val url = "$baseUrl?crnOrName=someone"
        val expectedTasks = listOf(
          nameMatchTasks[TaskType.assessment],
          nameMatchTasks[TaskType.placementApplication],
        )

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
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks filters by CRN without task type`(baseUrl: String) {
        val url = "$baseUrl?crnOrName=$crn"
        val expectedTasks = listOf(
          crnMatchTasks[TaskType.assessment],
          crnMatchTasks[TaskType.placementApplication],
        )

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
    inner class FilterByCompleted : InitialiseDatabasePerClassTestBase() {
      lateinit var jwt: String
      lateinit var crn: String

      private lateinit var incompleteTasks: List<Task>
      private lateinit var completeTasks: List<Task>

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      @BeforeAll
      fun setup() {
        givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
          givenAUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              this.jwt = jwt
              this.crn = offenderDetails.otherIds.crn

              val offenderSummaries = getOffenderSummaries(offenderDetails)

              val (assessment1, _) = givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
                createdAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS),
              )

              val (assessment2, _) = givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
              )

              val placementApplication1 = givenAPlacementApplication(
                createdByUser = otherUser,
                allocatedToUser = user,
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
                expectedArrival = LocalDate.now(),
                duration = 1,
              )

              val placementApplication2 = givenAPlacementApplication(
                createdByUser = otherUser,
                allocatedToUser = user,
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
                decision = ACCEPTED,
                expectedArrival = LocalDate.now(),
                duration = 1,
              )

              incompleteTasks = listOf(
                taskTransformer.transformAssessmentToTask(
                  assessment1,
                  offenderSummaries,
                ),
                taskTransformer.transformPlacementApplicationToTask(
                  placementApplication1,
                  offenderSummaries,
                ),
              )

              completeTasks = listOf(
                taskTransformer.transformAssessmentToTask(
                  assessment2,
                  offenderSummaries,
                ),
                taskTransformer.transformAssessmentToTask(
                  assessmentTestRepository.findAllByApplication(placementApplication1.application)[0],
                  offenderSummaries,
                ),
                taskTransformer.transformAssessmentToTask(
                  assessmentTestRepository.findAllByApplication(placementApplication2.application)[0],
                  offenderSummaries,
                ),
                taskTransformer.transformPlacementApplicationToTask(
                  placementApplication2,
                  offenderSummaries,
                ),
              )
            }
          }
        }
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks shows incomplete tasks by default`(baseUrl: String) {
        webTestClient.get()
          .uri(baseUrl)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              incompleteTasks,
            ),
          )
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks shows allows showing completed tasks`(baseUrl: String) {
        objectMapper.setDateFormat(SimpleDateFormat("yyyy-mm-dd'T'HH:mm:ss"))

        val rawResponseBody = webTestClient.get()
          .uri("$baseUrl?isCompleted=true")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<String>()
          .responseBody
          .blockFirst()

        val responseBody = objectMapper.readValue(rawResponseBody, object : TypeReference<List<Task>>() {})

        assertThat(responseBody)
          .usingRecursiveFieldByFieldElementComparator(
            RecursiveComparisonConfiguration.builder().withComparatorForType(
              { a: Instant, b: Instant ->
                a.truncatedTo(ChronoUnit.MILLIS).compareTo(b.truncatedTo(ChronoUnit.MILLIS))
              },
              Instant::class.java,
            ).build(),
          )
          .hasSameElementsAs(completeTasks)
      }
    }

    @Nested
    inner class SortByTest : InitialiseDatabasePerClassTestBase() {
      lateinit var jwt: String
      lateinit var crn: String

      private lateinit var tasks: Map<UUID, Task>
      private lateinit var assessments: Map<UUID, AssessmentEntity>
      private lateinit var placementRequests: Map<UUID, PlacementRequestEntity>
      private lateinit var placementApplications: Map<UUID, PlacementApplicationEntity>

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      @BeforeAll
      fun setup() {
        givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
          givenAUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              this.jwt = jwt
              this.crn = offenderDetails.otherIds.crn

              val (assessment1, _) = givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
                createdAt = OffsetDateTime.now().minusDays(14).randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                submittedAt = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                dueAt = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                arrivalDate = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                apType = ApprovedPremisesType.ESAP,
              )

              val (assessment2, _) = givenAnAssessmentForApprovedPremises(
                allocatedToUser = otherUser,
                createdByUser = otherUser,
                crn = offenderDetails.otherIds.crn,
                createdAt = OffsetDateTime.now().minusDays(14).randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                submittedAt = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                dueAt = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                arrivalDate = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                apType = ApprovedPremisesType.MHAP_ELLIOTT_HOUSE,
              )

              val (placementRequest1, _) = givenAPlacementRequest(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
                booking = bookingEntityFactory.produceAndPersist {
                  withPremises(givenAnApprovedPremises())
                },
                apType = ApprovedPremisesType.ESAP,
              )

              val (placementRequest2, _) = givenAPlacementRequest(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
                dueAt = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                booking = bookingEntityFactory.produceAndPersist {
                  withPremises(givenAnApprovedPremises())
                },
                apType = ApprovedPremisesType.RFAP,
              )

              val offenderSummaries = getOffenderSummaries(offenderDetails)
              val (placementRequest3, _) = givenAPlacementRequest(
                placementRequestAllocatedTo = otherUser,
                assessmentAllocatedTo = otherUser,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
                dueAt = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                apType = ApprovedPremisesType.PIPE,
              )

              placementRequest3.bookingNotMades = mutableListOf(
                bookingNotMadeFactory.produceAndPersist {
                  withPlacementRequest(placementRequest3)
                },
              )

              val placementApplication1 = givenAPlacementApplication(
                createdByUser = otherUser,
                allocatedToUser = user,
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                dueAt = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                decision = REJECTED,
                apType = ApprovedPremisesType.MHAP_ST_JOSEPHS,
                expectedArrival = LocalDate.now(),
                duration = 1,
              )

              val placementApplication2 = givenAPlacementApplication(
                createdByUser = otherUser,
                allocatedToUser = user,
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                dueAt = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
                decision = ACCEPTED,
                apType = ApprovedPremisesType.NORMAL,
                expectedArrival = LocalDate.now(),
                duration = 1,
              )

              assessments = mapOf(
                assessment1.id to assessment1,
                assessment2.id to assessment2,
                placementRequest1.assessment.id to placementRequest1.assessment,
                placementRequest2.assessment.id to placementRequest2.assessment,
                placementRequest3.assessment.id to placementRequest3.assessment,
                placementApplication1.application.getLatestAssessment()!!.id to placementApplication1.application.getLatestAssessment()!!,
                placementApplication2.application.getLatestAssessment()!!.id to placementApplication2.application.getLatestAssessment()!!,
              )

              placementRequests = mapOf(
                placementRequest1.id to placementRequest1,
                placementRequest2.id to placementRequest2,
                placementRequest3.id to placementRequest3,
              )

              placementApplications = mapOf(
                placementApplication1.id to placementApplication1,
                placementApplication2.id to placementApplication2,
              )

              tasks = mapOf()
              tasks += assessments.mapValues {
                taskTransformer.transformAssessmentToTask(
                  it.value,
                  offenderSummaries,
                )
              }
              tasks += placementApplications.mapValues {
                taskTransformer.transformPlacementApplicationToTask(
                  it.value,
                  offenderSummaries,
                )
              }
            }
          }
        }
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks sorts by createdAt in ascending order by default`(baseUrl: String) {
        val response = webTestClient.get()
          .uri("$baseUrl?isCompleted=true&page=1&perPage=10")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskCreatedAt = tasks.values.map { getCreatedAt(it) }
          .sortedWith(compareBy(nullsLast()) { it })

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(getCreatedAt(task)).isEqualTo(expectedTaskCreatedAt[index])
        }
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks sorts by createdAt in ascending order`(baseUrl: String) {
        val url = "$baseUrl?isCompleted=true&sortBy=createdAt&sortDirection=asc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskCreatedAt = tasks.values.map { getCreatedAt(it) }
          .sortedWith(compareBy(nullsLast()) { it })

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(getCreatedAt(task)).isEqualTo(expectedTaskCreatedAt[index])
        }
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks sorts by createdAt in descending order`(baseUrl: String) {
        val url = "$baseUrl?isCompleted=true&sortBy=createdAt&sortDirection=desc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskCreatedAt = tasks.values.map { getCreatedAt(it) }
          .sortedWith(compareByDescending(nullsLast()) { it })

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(getCreatedAt(task)).isEqualTo(expectedTaskCreatedAt[index])
        }
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks sorts by dueAt in ascending order`(baseUrl: String) {
        val url = "$baseUrl?isCompleted=true&sortBy=dueAt&sortDirection=asc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskDueAt = tasks.values.map { it.dueAt }.sorted()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.dueAt).isEqualTo(expectedTaskDueAt[index])
        }
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks sorts by dueAt in descending order`(baseUrl: String) {
        val url = "$baseUrl?isCompleted=true&sortBy=dueAt&sortDirection=desc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskDueAt = tasks.values.map { it.dueAt }.sortedDescending()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.dueAt).isEqualTo(expectedTaskDueAt[index])
        }
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks sorts by allocatedTo in ascending order`(baseUrl: String) {
        val url = "$baseUrl?isCompleted=true&sortBy=allocatedTo&sortDirection=asc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskAllocatedName = tasks.values.map { it.allocatedToStaffMember!!.name }.sorted()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.allocatedToStaffMember!!.name).isEqualTo(expectedTaskAllocatedName[index])
        }
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks sorts by allocatedTo in descending order`(baseUrl: String) {
        val url = "$baseUrl?isCompleted=true&sortBy=allocatedTo&sortDirection=desc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskAllocatedName = tasks.values.map { it.allocatedToStaffMember!!.name }.sortedDescending()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.allocatedToStaffMember!!.name).isEqualTo(expectedTaskAllocatedName[index])
        }
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks sorts by person in ascending order`(baseUrl: String) {
        val url = "$baseUrl?isCompleted=true&sortBy=person&sortDirection=asc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskPersonNames = tasks.values.map { it.personName }.sorted()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.personName).isEqualTo(expectedTaskPersonNames[index])
        }
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks sorts by person in descending order`(baseUrl: String) {
        val url = "$baseUrl?isCompleted=true&sortBy=person&sortDirection=desc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskPersonNames = tasks.values.map { it.personName }.sortedDescending()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.personName).isEqualTo(expectedTaskPersonNames[index])
        }
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks sorts by completedAt in ascending order`(baseUrl: String) {
        val url = "$baseUrl?isCompleted=true&sortBy=completedAt&sortDirection=asc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskCompletedAts = tasks.values.map { it.outcomeRecordedAt }
          .sortedWith(compareBy(nullsLast()) { it })

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.outcomeRecordedAt).isEqualTo(expectedTaskCompletedAts[index])
        }
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks sorts by completedAt in descending order`(baseUrl: String) {
        val url = "$baseUrl?isCompleted=true&sortBy=completedAt&sortDirection=desc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskCompletedAts = tasks.values.map { it.outcomeRecordedAt }
          .sortedWith(compareByDescending(nullsLast()) { it })

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.outcomeRecordedAt).isEqualTo(expectedTaskCompletedAts[index])
        }
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks sorts by taskType in ascending order`(baseUrl: String) {
        val url = "$baseUrl?isCompleted=true&sortBy=taskType&sortDirection=asc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskTypes = tasks.values.map { it.taskType }.sorted()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.taskType).isEqualTo(expectedTaskTypes[index])
        }
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks sorts by expected arrival date in ascending order`(baseUrl: String) {
        val url = "$baseUrl?isCompleted=true&sortBy=expectedArrivalDate&sortDirection=asc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedArrivalDates = tasks.values.map { it.expectedArrivalDate.toString() }.sorted()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.expectedArrivalDate.toString()).isEqualTo(expectedArrivalDates[index])
        }
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks sorts by expected arrival date in descending order`(baseUrl: String) {
        val url = "$baseUrl?isCompleted=true&sortBy=expectedArrivalDate&sortDirection=desc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedArrivalDates = tasks.values.map { it.expectedArrivalDate.toString() }.sortedDescending()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.expectedArrivalDate.toString()).isEqualTo(expectedArrivalDates[index])
        }
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks sorts by taskType in descending order`(baseUrl: String) {
        val url = "$baseUrl?isCompleted=true&sortBy=taskType&sortDirection=desc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskTypes = tasks.values.map { it.taskType }.sortedDescending()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.taskType).isEqualTo(expectedTaskTypes[index])
        }
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks sorts by decision in ascending order`(baseUrl: String) {
        val url = "$baseUrl?isCompleted=true&sortBy=decision&sortDirection=asc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedDecisions = tasks.values.map { getDecision(it) }
          .sortedWith(compareBy(nullsLast()) { it })

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(getDecision(task)).isEqualTo(expectedDecisions[index])
        }
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks sorts by decision in descending order`(baseUrl: String) {
        val url = "$baseUrl?isCompleted=true&sortBy=decision&sortDirection=desc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedDecisions = tasks.values.map { getDecision(it) }
          .sortedWith(compareByDescending(nullsLast()) { it })

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(getDecision(task)).isEqualTo(expectedDecisions[index])
        }
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks sorts by apType in ascending order`(baseUrl: String) {
        val url = "$baseUrl?isCompleted=true&sortBy=apType&sortDirection=asc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskApType = tasks.values.map { it.apType.value }.sorted()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.apType.value).isEqualTo(expectedTaskApType[index])
        }
      }

      @ParameterizedTest
      @ValueSource(strings = ["/tasks", "/cas1/tasks"])
      fun `Get all tasks sorts by apType in descending order`(baseUrl: String) {
        val url = "$baseUrl?isCompleted=true&sortBy=apType&sortDirection=desc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskApType = tasks.values.map { it.apType.value }.sortedDescending()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.apType.value).isEqualTo(expectedTaskApType[index])
        }
      }

      private fun getDecision(task: Task): String? = when (task) {
        is AssessmentTask -> task.outcome?.value
        is PlacementApplicationTask -> task.outcome?.value
        else -> fail()
      }

      private fun getCreatedAt(task: Task): OffsetDateTime = when (task) {
        is AssessmentTask -> assessments[task.id]!!.createdAt
        is PlacementApplicationTask -> placementApplications[task.id]!!.createdAt
        else -> fail()
      }
    }
  }

  @Nested
  inner class GetTaskTest : IntegrationTestBase() {
    @Autowired
    lateinit var taskTransformer: TaskTransformer

    @Autowired
    lateinit var userTransformer: UserTransformer

    @ParameterizedTest
    @ValueSource(strings = ["/tasks", "/cas1/tasks"])
    fun `Request without JWT returns 401`(baseUrl: String) {
      webTestClient.get()
        .uri("$baseUrl/assessment/f601ff2d-b1e0-4878-8731-ccfa19a2ce84")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @ValueSource(strings = ["/tasks", "/cas1/tasks"])
    fun `Unknown task type for an application returns 404`(baseUrl: String) {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          givenAnAssessmentForApprovedPremises(
            allocatedToUser = user,
            createdByUser = user,
            crn = offenderDetails.otherIds.crn,
          ) { _, application ->
            webTestClient.get()
              .uri("$baseUrl/unknown-task/${application.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isNotFound
          }
        }
      }
    }

    @ParameterizedTest
    @ValueSource(strings = ["/tasks", "/cas1/tasks"])
    fun `If request is for an application only returns active users with ASSESSOR role`(baseUrl: String) {
      val (creator, _) = givenAUser()
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER))
      val (assessor, _) = givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
      )
      // inactive user with correct role
      givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        isActive = false,
      )
      // user with incorrect role
      givenAUser(
        roles = listOf(UserRole.CAS1_CRU_MEMBER),
      )

      givenAnOffender { offenderDetails, _ ->
        givenAnAssessmentForApprovedPremises(
          allocatedToUser = null,
          createdByUser = creator,
          crn = offenderDetails.otherIds.crn,
          dueAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS),
        ) { assessment, _ ->

          webTestClient.get()
            .uri("$baseUrl/assessment/${assessment.id}")
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
                    getOffenderSummaries(offenderDetails),
                  ),
                  users = listOf(
                    userTransformer.transformJpaToAPIUserWithWorkload(
                      assessor,
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

    @ParameterizedTest
    @ValueSource(strings = ["/tasks", "/cas1/tasks"])
    fun `If request is for an appealed application only returns users with CAS1_APPEALS_MANAGER or CAS1_ASSESSOR role`(baseUrl: String) {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { _, jwt ->
        givenAUser(
          roles = listOf(UserRole.CAS1_REPORT_VIEWER),
        ) { user, _ ->
          givenAUser(
            roles = listOf(UserRole.CAS1_APPEALS_MANAGER),
          ) { appealsManager, _ ->
            givenAUser(
              roles = listOf(UserRole.CAS1_ASSESSOR),
            ) { assessor, _ ->
              givenAnOffender { offenderDetails, _ ->
                givenAnAssessmentForApprovedPremises(
                  allocatedToUser = user,
                  createdByUser = user,
                  crn = offenderDetails.otherIds.crn,
                  decision = AssessmentDecision.REJECTED,
                  createdFromAppeal = true,
                  dueAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                ) { assessment, _ ->
                  webTestClient.get()
                    .uri("$baseUrl/assessment/${assessment.id}")
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
                            getOffenderSummaries(offenderDetails),
                          ),
                          users = listOf(
                            userTransformer.transformJpaToAPIUserWithWorkload(
                              appealsManager,
                              UserWorkload(
                                0,
                                0,
                                0,
                              ),
                            ),
                            userTransformer.transformJpaToAPIUserWithWorkload(
                              assessor,
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

    @ParameterizedTest
    @ValueSource(strings = ["/tasks", "/cas1/tasks"])
    fun `If request is for an appealed application returns 0 users if no users with CAS1_APPEALS_MANAGER or CAS1_ASSESSOR role`(baseUrl: String) {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { _, jwt ->
        givenAUser(
          roles = listOf(UserRole.CAS1_REPORT_VIEWER),
        ) { janitor, _ ->
          givenAUser(
            roles = listOf(UserRole.CAS1_CRU_MEMBER),
          ) { _, _ ->
            givenAnOffender { offenderDetails, _ ->
              givenAnAssessmentForApprovedPremises(
                allocatedToUser = null,
                createdByUser = janitor,
                crn = offenderDetails.otherIds.crn,
                decision = AssessmentDecision.REJECTED,
                createdFromAppeal = true,
                dueAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS),
              ) { assessment, _ ->
                webTestClient.get()
                  .uri("$baseUrl/assessment/${assessment.id}")
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
                          getOffenderSummaries(offenderDetails),
                        ),
                        users = emptyList(),
                      ),
                    ),
                  )
              }
            }
          }
        }
      }
    }

    @ParameterizedTest
    @ValueSource(strings = ["/tasks", "/cas1/tasks"])
    fun `If request is for a placement application that is not submitted, return not found because a task doesn't yet exist to complete`(baseUrl: String) {
      val (creatingUser, jwt) = givenAUser()

      val placementApplication = givenAPlacementApplication(
        createdByUser = creatingUser,
        allocatedToUser = creatingUser,
        crn = "cRN123",
        submittedAt = null,
      )

      webTestClient.get()
        .uri("$baseUrl/placement-application/${placementApplication.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @ParameterizedTest
    @ValueSource(strings = ["/tasks", "/cas1/tasks"])
    fun `If request is for a placement application only returns active users with ASSESSOR role, with correct workload`(baseUrl: String) {
      // ignored, wrong role
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER))

      // ignored, inactive
      givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR), isActive = false)

      val (allocatableUser, _) = givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR))

      val (creatingUser, jwt) = givenAUser()

      val (offenderDetails) = givenAnOffender()
      val crn = offenderDetails.otherIds.crn

      val placementApplication = givenAPlacementApplication(
        createdByUser = creatingUser,
        allocatedToUser = creatingUser,
        crn = crn,
        submittedAt = OffsetDateTime.now(),
        expectedArrival = LocalDate.now(),
        duration = 1,
      )

      val numAppAssessPending = 3
      repeat(numAppAssessPending) {
        createAssessment(assessedAt = null, allocatableUser, creatingUser, crn)
      }
      // withdrawn, ignored
      createAssessment(assessedAt = null, allocatableUser, creatingUser, crn, isWithdrawn = true)

      val numPlacementAppAssessPending = 4
      repeat(numPlacementAppAssessPending) {
        createPlacementApplication(assessedAt = null, allocatableUser, creatingUser, crn)
      }
      // withdrawn, ignored
      createPlacementApplication(assessedAt = null, allocatableUser, creatingUser, crn, isWithdrawn = true)

      val numAppAssessCompletedBetween1And7DaysAgo = 4
      repeat(numAppAssessCompletedBetween1And7DaysAgo) {
        val days = kotlin.random.Random.nextInt(1, 7).toLong()
        createAssessment(OffsetDateTime.now().minusDays(days), allocatableUser, creatingUser, crn)
      }

      val numPlacementAppAssessCompletedBetween1And7DaysAgo = 2
      repeat(numPlacementAppAssessCompletedBetween1And7DaysAgo) {
        val days = kotlin.random.Random.nextInt(1, 7).toLong()
        createPlacementApplication(OffsetDateTime.now().minusDays(days), allocatableUser, creatingUser, crn)
      }

      val numAppAssessCompletedBetween8And30DaysAgo = 4
      repeat(numAppAssessCompletedBetween8And30DaysAgo) {
        val days = kotlin.random.Random.nextInt(8, 30).toLong()
        createAssessment(OffsetDateTime.now().minusDays(days), allocatableUser, creatingUser, crn)
      }

      val numPlacementAppAssessCompletedBetween8And30DaysAgo = 3
      repeat(numPlacementAppAssessCompletedBetween8And30DaysAgo) {
        val days = kotlin.random.Random.nextInt(8, 30).toLong()
        createPlacementApplication(OffsetDateTime.now().minusDays(days), allocatableUser, creatingUser, crn)
      }

      // completed after 30 days ago, ignored
      repeat(10) {
        createAssessment(OffsetDateTime.now().minusDays(31), allocatableUser, creatingUser, crn)
      }
      repeat(10) {
        createPlacementApplication(OffsetDateTime.now().minusDays(31), allocatableUser, creatingUser, crn)
      }

      webTestClient.get()
        .uri("$baseUrl/placement-application/${placementApplication.id}")
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
                getOffenderSummaries(offenderDetails),
              ),
              users = listOf(
                userTransformer.transformJpaToAPIUserWithWorkload(
                  allocatableUser,
                  UserWorkload(
                    numTasksPending = 7,
                    numTasksCompleted7Days = 6,
                    numTasksCompleted30Days = 13,
                  ),
                ),
              ),
            ),
          ),
        )
    }

    private fun createAssessment(
      assessedAt: OffsetDateTime?,
      allocatedUser: UserEntity,
      createdByUser: UserEntity,
      crn: String,
      isWithdrawn: Boolean = false,
    ) {
      givenAnAssessmentForApprovedPremises(
        allocatedToUser = allocatedUser,
        createdByUser = createdByUser,
        crn = crn,
        decision = null,
        reallocated = false,
        submittedAt = assessedAt,
        isWithdrawn = isWithdrawn,
      )
    }

    private fun createPlacementApplication(
      assessedAt: OffsetDateTime?,
      allocatedUser: UserEntity,
      createdByUser: UserEntity,
      crn: String,
      isWithdrawn: Boolean = false,
    ) {
      givenAPlacementApplication(
        createdByUser = createdByUser,
        allocatedToUser = allocatedUser,
        submittedAt = assessedAt?.minusDays(1),
        decisionMadeAt = assessedAt,
        crn = crn,
        isWithdrawn = isWithdrawn,
      )
    }
  }

  @Nested
  inner class ReallocateTaskTest : IntegrationTestBase() {
    @Autowired
    lateinit var userTransformer: UserTransformer

    @BeforeEach
    fun stubBankHolidaysApi() {
      govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()
    }

    @ParameterizedTest
    @ValueSource(strings = ["/tasks", "/cas1/tasks"])
    fun `Reallocate application to different assessor without JWT returns 401`(baseUrl: String) {
      webTestClient.post()
        .uri("$baseUrl/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
        .bodyValue(
          NewReallocation(
            userId = UUID.randomUUID(),
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @ValueSource(strings = ["/tasks", "/cas1/tasks"])
    fun `Reallocate application to different assessor without CAS1_CRU_MEMBER role returns 403`(baseUrl: String) {
      givenAUser { _, jwt ->
        webTestClient.post()
          .uri("$baseUrl/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
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

    @ParameterizedTest
    @ValueSource(strings = ["/tasks", "/cas1/tasks"])
    fun `Reallocate assessment to different assessor returns 201, creates new assessment, deallocates old one, sends emails`(baseUrl: String) {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { _, jwt ->
        givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR)) { currentlyAllocatedUser, _ ->
          givenAUser(
            roles = listOf(UserRole.CAS1_ASSESSOR),
          ) { assigneeUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              givenAnAssessmentForApprovedPremises(
                allocatedToUser = currentlyAllocatedUser,
                createdByUser = currentlyAllocatedUser,
                crn = offenderDetails.otherIds.crn,
              ) { existingAssessment, application ->

                webTestClient.post()
                  .uri("$baseUrl/assessment/${existingAssessment.id}/allocations")
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

                assertThat(assessments.first { it.id == existingAssessment.id }.reallocatedAt).isNotNull
                assertThat(assessments)
                  .anyMatch { it.application.id == application.id && it.allocatedToUser!!.id == assigneeUser.id }

                emailAsserter.assertEmailsRequestedCount(2)
                emailAsserter.assertEmailRequested(currentlyAllocatedUser.email!!, Cas1NotifyTemplates.ASSESSMENT_DEALLOCATED)
                emailAsserter.assertEmailRequested(assigneeUser.email!!, Cas1NotifyTemplates.ASSESSMENT_ALLOCATED)
              }
            }
          }
        }
      }
    }

    @ParameterizedTest
    @ValueSource(strings = ["/tasks", "/cas1/tasks"])
    fun `Reallocate assessment to different assessor returns an error if the assessment has already been allocated`(baseUrl: String) {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { _, jwt ->
        givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR)) { user, _ ->
          givenAUser(
            roles = listOf(UserRole.CAS1_ASSESSOR),
          ) { assigneeUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              givenAnAssessmentForApprovedPremises(
                allocatedToUser = user,
                createdByUser = user,
                crn = offenderDetails.otherIds.crn,
                reallocated = true,
              ) { existingAssessment, application ->

                webTestClient.post()
                  .uri("$baseUrl/assessment/${existingAssessment.id}/allocations")
                  .header("Authorization", "Bearer $jwt")
                  .header("X-Service-Name", ServiceName.approvedPremises.value)
                  .bodyValue(
                    NewReallocation(
                      userId = assigneeUser.id,
                    ),
                  )
                  .exchange()
                  .expectStatus()
                  .is4xxClientError()
                  .expectBody()
                  .jsonPath("title").isEqualTo("Conflict")
                  .jsonPath("status").isEqualTo(409)
                  .jsonPath("detail")
                  .isEqualTo("This assessment has already been reallocated: ${existingAssessment.id}")
              }
            }
          }
        }
      }
    }

    @ParameterizedTest
    @ValueSource(strings = ["/tasks", "/cas1/tasks"])
    fun `Reallocating a placement application to different assessor returns 201, creates new placement application, deallocates old one`(baseUrl: String) {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { _, jwt ->
        givenAUser { user, _ ->
          givenAUser(
            roles = listOf(UserRole.CAS1_ASSESSOR),
          ) { assigneeUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              givenAPlacementApplication(
                createdByUser = user,
                allocatedToUser = user,
                crn = offenderDetails.otherIds.crn,
                submittedAt = OffsetDateTime.now(),
                expectedArrival = LocalDate.of(2012, 1, 1),
                duration = 15,
              ) { placementApplication ->
                webTestClient.post()
                  .uri("$baseUrl/placement-application/${placementApplication.id}/allocations")
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

                assertThat(placementApplications.first { it.id == placementApplication.id }.reallocatedAt).isNotNull
                assertThat(allocatedPlacementApplication).isNotNull

                assertThat(allocatedPlacementApplication!!.expectedArrival).isEqualTo(LocalDate.of(2012, 1, 1))
                assertThat(allocatedPlacementApplication.duration).isEqualTo(15)
              }
            }
          }
        }
      }
    }

    @ParameterizedTest
    @ValueSource(strings = ["/tasks", "/cas1/tasks"])
    fun `Reallocating a Temporary Accommodation assessment does not require a request body`(baseUrl: String) {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { originalUser, _ ->
        givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { expectedUser, jwt ->
          givenAnOffender { offenderDetails, _ ->
            givenAnAssessmentForTemporaryAccommodation(
              allocatedToUser = originalUser,
              createdByUser = originalUser,
              crn = offenderDetails.otherIds.crn,
            ) { assessment, _ ->
              webTestClient.post()
                .uri("$baseUrl/assessment/${assessment.id}/allocations")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
                .bodyValue(Unit)
                .exchange()
                .expectStatus()
                .isCreated

              val result = temporaryAccommodationAssessmentRepository.findAll().first { it.id == assessment.id }
              assertThat(result.allocatedToUser).isNotNull()
              assertThat(result.allocatedToUser!!.id).isEqualTo(expectedUser.id)
            }
          }
        }
      }
    }
  }

  @Nested
  inner class DeallocateTaskTest : IntegrationTestBase() {
    @ParameterizedTest
    @ValueSource(strings = ["/tasks", "/cas1/tasks"])
    fun `Deallocate assessment without JWT returns 401 Unauthorized`(baseUrl: String) {
      webTestClient.delete()
        .uri("$baseUrl/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @ValueSource(strings = ["/tasks", "/cas1/tasks"])
    fun `Deallocate Temporary Accommodation assessment without CAS3_ASSESSOR role returns 403 Forbidden`(baseUrl: String) {
      givenAUser { _, jwt ->
        webTestClient.delete()
          .uri("$baseUrl/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @ValueSource(strings = ["/tasks", "/cas1/tasks"])
    fun `Deallocate Approved Premises assessment returns 403 Forbidden`(baseUrl: String) {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          givenAUser { _, _ ->
            givenAnAssessmentForApprovedPremises(
              allocatedToUser = user,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            ) { assessment, _ ->
              webTestClient.delete()
                .uri("$baseUrl/assessment/${assessment.id}/allocations")
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

    @ParameterizedTest
    @ValueSource(strings = ["/tasks", "/cas1/tasks"])
    fun `Deallocate Temporary Accommodation assessment returns 200 and unassigns the allocated user`(baseUrl: String) {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          givenAnAssessmentForTemporaryAccommodation(
            allocatedToUser = user,
            createdByUser = user,
            crn = offenderDetails.otherIds.crn,
          ) { existingAssessment, _ ->

            webTestClient.delete()
              .uri("$baseUrl/assessment/${existingAssessment.id}/allocations")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
              .exchange()
              .expectStatus()
              .isNoContent

            val assessment =
              temporaryAccommodationAssessmentRepository.findAll().first { it.id == existingAssessment.id }

            assertThat(assessment.allocatedToUser).isNull()
            assertThat(assessment.allocatedAt).isNull()
          }
        }
      }
    }
  }

  fun getOffenderSummaries(offenderDetails: OffenderDetailSummary): List<PersonSummaryInfoResult> = listOf(
    PersonSummaryInfoResult.Success.Full(
      offenderDetails.otherIds.crn,
      CaseSummaryFactory().withName(
        NameFactory()
          .withForename(offenderDetails.firstName)
          .withSurname(offenderDetails.surname)
          .produce(),
      )
        .produce(),
    ),
  )
}
