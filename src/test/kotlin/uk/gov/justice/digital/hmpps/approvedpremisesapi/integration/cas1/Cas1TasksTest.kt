package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
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

class Cas1TasksTest {

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

      @Test
      fun `Get all tasks without JWT returns 401`() {
        webTestClient.get()
          .uri("/cas1/tasks")
          .exchange()
          .expectStatus()
          .isUnauthorized
      }

      @Test
      fun `Get all tasks without cru member, matcher or assessor permissions returns 403`() {
        givenAUser { _, jwt ->
          webTestClient.get()
            .uri("/cas1/tasks")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"])
      fun `Get all tasks returns 200 when have CAS1_CRU_MEMBER or CAS1_AP_AREA_MANAGER roles`(role: UserRole) {
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
              webTestClient.get()
                .uri("/cas1/tasks?page=1&sortBy=createdAt&sortDirection=asc")
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
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"])
      fun `Get all tasks returns 200 when no type retains original sort order`(role: UserRole) {
        givenAUser(roles = listOf(role)) { user, jwt ->
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
                .uri("/cas1/tasks?page=1&sortBy=createdAt&sortDirection=asc")
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
                .uri("/cas1/tasks?page=1&sortBy=createdAt&sortDirection=desc")
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
    inner class FilterByType : IntegrationTestBase() {

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      fun setupTasksForUser(user: UserEntity): Map<TaskType, List<Task>> {
        val (otherUser, _) = givenAUser()
        val (offenderDetails, _) = givenAnOffender()

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

        val tasks = mapOf(
          TaskType.assessment to assessments,
          TaskType.placementApplication to placementApplications,
        )
        return tasks
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks filtered by assessments`(role: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(role))
        val tasks = setupTasksForUser(user)
        val expectedTasks = tasks[TaskType.assessment]!!.sortedBy { it.dueDate }
        val url = "/cas1/tasks?page=1&sortBy=createdAt&sortDirection=asc&types=Assessment"

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
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks filtered by placement applications`(role: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(role))
        val tasks = setupTasksForUser(user)
        val expectedTasks = tasks[TaskType.placementApplication]!!.sortedBy { it.dueDate }
        val url = "/cas1/tasks?page=1&sortBy=createdAt&sortDirection=asc&types=PlacementApplication"

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
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks filters by multiple types`(role: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(role))
        val tasks = setupTasksForUser(user)
        val url = "/cas1/tasks?page=1&sortBy=createdAt&sortDirection=asc&types=Assessment&types=PlacementApplication"
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
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks returns all task types`(role: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(role))
        val tasks = setupTasksForUser(user)
        val url = "/cas1/tasks?page=1&sortBy=createdAt&sortDirection=asc&types=Assessment&types=PlacementApplication"
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
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks returns all task types by default`(role: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(role))
        val tasks = setupTasksForUser(user)
        val url = "/cas1/tasks?page=1&sortBy=createdAt&sortDirection=asc"
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

    @Nested
    inner class FilterByCruManagementArea : IntegrationTestBase() {

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      fun setupTasksForUser(user: UserEntity): Pair<Map<TaskType, List<Task>>, Cas1CruManagementAreaEntity> {
        val (otherUser, _) = givenAUser()
        val (offenderDetails, _) = givenAnOffender()

        val cruArea = givenACas1CruManagementArea()
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

        val tasks = mapOf(
          TaskType.assessment to assessments,
          TaskType.placementApplication to placementApplications,
        )
        return Pair(tasks, cruArea)
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `it filters by CRU area and assessment type`(role: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(role))
        val (tasks, cruArea) = setupTasksForUser(user)
        val expectedTasks = tasks[TaskType.assessment]
        val url = "/cas1/tasks?type=Assessment&cruManagementAreaId=${cruArea.id}"

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
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `it filters by CRU area and placement application type`(role: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(role))
        val (tasks, cruArea) = setupTasksForUser(user)
        val expectedTasks = tasks[TaskType.placementApplication]
        val url = "/cas1/tasks?type=PlacementApplication&cruManagementAreaId=${cruArea.id}"

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
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `it filters by all areas with no task type`(role: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(role))
        val (tasks, cruArea) = setupTasksForUser(user)
        val expectedTasks = listOf(
          tasks[TaskType.assessment]!!,
          tasks[TaskType.placementApplication]!!,
        ).flatten().sortedBy { it.dueDate }

        webTestClient.get()
          .uri("/cas1/tasks?cruManagementAreaId=${cruArea.id}")
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
    inner class FilterByUser : IntegrationTestBase() {

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      fun setupTasksForUser(user: UserEntity): Map<TaskType, List<Task>> {
        val (otherUser, _) = givenAUser()
        val (offenderDetails, _) = givenAnOffender()

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

        val tasks = mapOf(
          TaskType.assessment to assessments,
          TaskType.placementApplication to placementApplications,
        )
        return tasks
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `it filters by user and assessment task type`(role: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(role))
        val tasks = setupTasksForUser(user)
        val expectedTasks = tasks[TaskType.assessment]
        val url = "/cas1/tasks?type=Assessment&allocatedToUserId=${user.id}"

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
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `it filters by user and placement application task type`(role: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(role))
        val tasks = setupTasksForUser(user)
        val expectedTasks = tasks[TaskType.placementApplication]
        val url = "/cas1/tasks?type=PlacementApplication&allocatedToUserId=${user.id}"

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
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `it filters by user with all tasks`(role: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(role))
        val tasks = setupTasksForUser(user)
        val expectedTasks = listOf(
          tasks[TaskType.assessment]!!,
          tasks[TaskType.placementApplication]!!,
        ).flatten().sortedBy { it.dueDate }

        val url = "/cas1/tasks?allocatedToUserId=${user.id}"

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
    inner class PaginationAndExclusionOfIrrelevantTasks : IntegrationTestBase() {
      private val pageSize = 1

      fun setupTasksForUser(user: UserEntity): Map<TaskType, Map<String, Int>> {
        val (otherUser, _) = givenAUser()
        val (offenderDetails, _) = givenAnOffender()

        val counts = mapOf(
          TaskType.assessment to mapOf(
            "allocated" to 2,
            "unallocated" to 3,
          ),
          TaskType.placementApplication to mapOf(
            "allocated" to 3,
            "unallocated" to 2,
          ),
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
        return counts
      }

      @ParameterizedTest
      @CsvSource(
        "CAS1_CRU_MEMBER,assessment,allocated,1",
        "CAS1_CRU_MEMBER,assessment,allocated,2",
        "CAS1_CRU_MEMBER,assessment,unallocated,1",
        "CAS1_CRU_MEMBER,assessment,unallocated,1",
        "CAS1_CRU_MEMBER,placementApplication,allocated,1",
        "CAS1_CRU_MEMBER,placementApplication,allocated,2",
        "CAS1_CRU_MEMBER,placementApplication,unallocated,1",
        "CAS1_CRU_MEMBER,placementApplication,unallocated,2",
        "CAS1_AP_AREA_MANAGER,assessment,allocated,1",
        "CAS1_AP_AREA_MANAGER,assessment,allocated,2",
        "CAS1_AP_AREA_MANAGER,assessment,unallocated,1",
        "CAS1_AP_AREA_MANAGER,assessment,unallocated,1",
        "CAS1_AP_AREA_MANAGER,placementApplication,allocated,1",
        "CAS1_AP_AREA_MANAGER,placementApplication,allocated,2",
        "CAS1_AP_AREA_MANAGER,placementApplication,unallocated,1",
        "CAS1_AP_AREA_MANAGER,placementApplication,unallocated,2",
      )
      fun `get all tasks returns page counts when taskType and allocated filter are set`(
        userRole: String,
        taskType: TaskType,
        allocatedFilter: String,
        pageNumber: String,
      ) {
        val (user, jwt) = givenAUser(roles = listOf(UserRole.valueOf(userRole)))
        val counts = setupTasksForUser(user)
        val itemCount = counts[taskType]!![allocatedFilter]!!
        val url = "/cas1/tasks?type=${taskType.value}&perPage=$pageSize&page=$pageNumber&allocatedFilter=$allocatedFilter"

        expectCountHeaders(url, jwt, pageNumber.toInt(), itemCount)
      }

      @ParameterizedTest
      @CsvSource(
        "CAS1_CRU_MEMBER,allocated,1",
        "CAS1_CRU_MEMBER,allocated,2",
        "CAS1_CRU_MEMBER,unallocated,1",
        "CAS1_CRU_MEMBER,unallocated,1",
        "CAS1_AP_AREA_MANAGER,allocated,1",
        "CAS1_AP_AREA_MANAGER,allocated,2",
        "CAS1_AP_AREA_MANAGER,unallocated,1",
        "CAS1_AP_AREA_MANAGER,unallocated,1",
      )
      fun `get all tasks returns page counts for all tasks when allocated filter is set`(
        userRole: String,
        allocatedFilter: String,
        pageNumber: String,
      ) {
        val (user, jwt) = givenAUser(roles = listOf(UserRole.valueOf(userRole)))
        val counts = setupTasksForUser(user)
        val itemCount = listOf(
          counts[TaskType.assessment]!![allocatedFilter]!!,
          counts[TaskType.placementApplication]!![allocatedFilter]!!,
        ).sum()

        val url = "/cas1/tasks?&page=$pageNumber&perPage=$pageSize&allocatedFilter=$allocatedFilter"

        expectCountHeaders(url, jwt, pageNumber.toInt(), itemCount)
      }

      @ParameterizedTest
      @CsvSource(
        "CAS1_CRU_MEMBER,1",
        "CAS1_CRU_MEMBER,2",
        "CAS1_AP_AREA_MANAGER,1",
        "CAS1_AP_AREA_MANAGER,2",
      )
      fun `get all tasks returns page count when no allocated filter is set`(role: String, pageNumber: Int) {
        val (user, jwt) = givenAUser(roles = listOf(UserRole.valueOf(role)))
        val counts = setupTasksForUser(user)
        val itemCount = listOf(
          counts[TaskType.assessment]!!["allocated"]!!,
          counts[TaskType.assessment]!!["unallocated"]!!,
          counts[TaskType.placementApplication]!!["allocated"]!!,
          counts[TaskType.placementApplication]!!["unallocated"]!!,
        ).sum()

        expectCountHeaders("/cas1/tasks?&page=$pageNumber&perPage=$pageSize", jwt, pageNumber, itemCount)
      }

      private fun expectCountHeaders(url: String, jwt: String, pageNumber: Int, itemCount: Int) {
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
    inner class FilterQualification : IntegrationTestBase() {

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      fun setupForUser(user: UserEntity): Map<TaskType, MutableMap<UserQualification, List<Task>>> {
        val (otherUser, _) = givenAUser()
        val (offenderDetails, _) = givenAnOffender()

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

        val tasks = mapOf(
          TaskType.assessment to assessmentTasks,
          TaskType.placementApplication to placementApplicationTasks,
        )
        return tasks
      }

      @ParameterizedTest
      @CsvSource(
        "CAS1_CRU_MEMBER,assessment,PIPE",
        "CAS1_CRU_MEMBER,assessment,ESAP",
        "CAS1_CRU_MEMBER,assessment,EMERGENCY",
        "CAS1_CRU_MEMBER,assessment,RECOVERY_FOCUSED",
        "CAS1_CRU_MEMBER,assessment,MENTAL_HEALTH_SPECIALIST",

        "CAS1_CRU_MEMBER,placementApplication,PIPE",
        "CAS1_CRU_MEMBER,placementApplication,ESAP",
        "CAS1_CRU_MEMBER,placementApplication,EMERGENCY",
        "CAS1_CRU_MEMBER,placementApplication,RECOVERY_FOCUSED",
        "CAS1_CRU_MEMBER,placementApplication,MENTAL_HEALTH_SPECIALIST",

        "CAS1_AP_AREA_MANAGER,assessment,PIPE",
        "CAS1_AP_AREA_MANAGER,assessment,ESAP",
        "CAS1_AP_AREA_MANAGER,assessment,EMERGENCY",
        "CAS1_AP_AREA_MANAGER,assessment,RECOVERY_FOCUSED",
        "CAS1_AP_AREA_MANAGER,assessment,MENTAL_HEALTH_SPECIALIST",

        "CAS1_AP_AREA_MANAGER,placementApplication,PIPE",
        "CAS1_AP_AREA_MANAGER,placementApplication,ESAP",
        "CAS1_AP_AREA_MANAGER,placementApplication,EMERGENCY",
        "CAS1_AP_AREA_MANAGER,placementApplication,RECOVERY_FOCUSED",
        "CAS1_AP_AREA_MANAGER,placementApplication,MENTAL_HEALTH_SPECIALIST",
      )
      fun `Get all tasks filters by task type and required qualification`(
        userRole: String,
        taskType: TaskType,
        qualification: UserQualification,
      ) {
        val (user, jwt) = givenAUser(roles = listOf(UserRole.valueOf(userRole)))
        val tasks = setupForUser(user)
        val url = "/cas1/tasks?type=${taskType.value}&requiredQualification=${qualification.name.lowercase()}"
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
      fun `Get all tasks required qualification for CAS1_CRU_MEMBER`(qualification: UserQualification) {
        val (user, jwt) = givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER))
        val tasks = setupForUser(user)
        val expectedTasks = listOf(
          tasks[TaskType.assessment]!![qualification]!!,
          tasks[TaskType.placementApplication]!![qualification]!!,
        ).flatten()

        val url = "/cas1/tasks?requiredQualification=${qualification.name.lowercase()}"

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
      fun `Get all tasks required qualification for CAS1_AP_AREA_MANAGER`(qualification: UserQualification) {
        val (user, jwt) = givenAUser(roles = listOf(UserRole.CAS1_AP_AREA_MANAGER))
        val tasks = setupForUser(user)
        val expectedTasks = listOf(
          tasks[TaskType.assessment]!![qualification]!!,
          tasks[TaskType.placementApplication]!![qualification]!!,
        ).flatten()

        val url = "/cas1/tasks?requiredQualification=${qualification.name.lowercase()}"

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
    inner class FilterByNameOrCrn : IntegrationTestBase() {

      private lateinit var nameMatchTasks: Map<TaskType, Task>
      private lateinit var crnMatchTasks: Map<TaskType, Task>

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      fun setupForUser(user: UserEntity): String {
        val (otherUser, _) = givenAUser()
        val (offenderDetails1, _) = givenAnOffender()
        val (offenderDetails2, _) = givenAnOffender()

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
        return offenderDetails2.otherIds.crn
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks filters by name and assessment task type`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        setupForUser(user)
        val url = "/cas1/tasks?type=Assessment&crnOrName=someone"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              listOf(nameMatchTasks[TaskType.assessment]!!),
            ),
          )
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks filters by name and placement application task type`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        setupForUser(user)
        val url = "/cas1/tasks?type=PlacementApplication&crnOrName=someone"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              listOf(nameMatchTasks[TaskType.placementApplication]!!),
            ),
          )
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks filters by CRN and assessment task type`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val crn = setupForUser(user)
        val url = "/cas1/tasks?type=Assessment&crnOrName=$crn"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              listOf(crnMatchTasks[TaskType.assessment]!!),
            ),
          )
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks filters by CRN and placement application task type`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val crn = setupForUser(user)
        val url = "/cas1/tasks?type=PlacementApplication&crnOrName=$crn"

        webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              listOf(crnMatchTasks[TaskType.placementApplication]!!),
            ),
          )
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks filters by name without task type`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        setupForUser(user)
        val url = "/cas1/tasks?crnOrName=someone"
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
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks filters by CRN without task type`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val crn = setupForUser(user)
        val url = "/cas1/tasks?crnOrName=$crn"
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
    inner class FilterByCompleted : IntegrationTestBase() {

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      fun setupForUser(user: UserEntity): Triple<String, List<Task>, List<Task>> {
        val (otherUser, _) = givenAUser()
        val (offenderDetails, _) = givenAnOffender()

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

        val incompleteTasks = listOf(
          taskTransformer.transformAssessmentToTask(
            assessment1,
            offenderSummaries,
          ),
          taskTransformer.transformPlacementApplicationToTask(
            placementApplication1,
            offenderSummaries,
          ),
        )

        val completeTasks = listOf(
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
        return Triple(offenderDetails.otherIds.crn, incompleteTasks, completeTasks)
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks shows incomplete tasks by default`(role: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(role))
        val (_, incompleteTasks, _) = setupForUser(user)
        webTestClient.get()
          .uri("/cas1/tasks")
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
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks shows allows showing completed tasks`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val (_, _, completeTasks) = setupForUser(user)
        objectMapper.setDateFormat(SimpleDateFormat("yyyy-mm-dd'T'HH:mm:ss"))

        val rawResponseBody = webTestClient.get()
          .uri("/cas1/tasks?isCompleted=true")
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
    inner class SortByTest : IntegrationTestBase() {

      @Autowired
      lateinit var taskTransformer: TaskTransformer

      fun setupForUser(user: UserEntity): TaskSortTestData {
        val (otherUser, _) = givenAUser()
        val (offenderDetails, _) = givenAnOffender()
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
          assessmentAllocatedTo = otherUser,
          createdByUser = user,
          crn = offenderDetails.otherIds.crn,
          booking = bookingEntityFactory.produceAndPersist {
            withPremises(givenAnApprovedPremises())
          },
          apType = ApprovedPremisesType.ESAP,
        )

        val (placementRequest2, _) = givenAPlacementRequest(
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

        val assessments = mapOf(
          assessment1.id to assessment1,
          assessment2.id to assessment2,
          placementRequest1.assessment.id to placementRequest1.assessment,
          placementRequest2.assessment.id to placementRequest2.assessment,
          placementRequest3.assessment.id to placementRequest3.assessment,
          placementApplication1.application.getLatestAssessment()!!.id to placementApplication1.application.getLatestAssessment()!!,
          placementApplication2.application.getLatestAssessment()!!.id to placementApplication2.application.getLatestAssessment()!!,
        )

        val placementRequests = mapOf(
          placementRequest1.id to placementRequest1,
          placementRequest2.id to placementRequest2,
          placementRequest3.id to placementRequest3,
        )

        val placementApplications = mapOf(
          placementApplication1.id to placementApplication1,
          placementApplication2.id to placementApplication2,
        )

        val tasks = mutableMapOf<UUID, Task>()
        tasks.putAll(
          assessments.mapValues {
            taskTransformer.transformAssessmentToTask(
              it.value,
              offenderSummaries,
            )
          },
        )
        tasks.putAll(
          placementApplications.mapValues {
            taskTransformer.transformPlacementApplicationToTask(
              it.value,
              offenderSummaries,
            )
          },
        )

        return TaskSortTestData(
          crn = offenderDetails.otherIds.crn,
          tasks = tasks,
          assessments = assessments,
          placementRequests = placementRequests,
          placementApplications = placementApplications,
        )
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks sorts by createdAt in ascending order by default`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val taskSortTestData = setupForUser(user)
        val response = webTestClient.get()
          .uri("/cas1/tasks?isCompleted=true&page=1&perPage=10")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskCreatedAt = taskSortTestData.tasks.values.map { getCreatedAt(it, taskSortTestData) }
          .sortedWith(compareBy(nullsLast()) { it })

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(getCreatedAt(task, taskSortTestData)).isEqualTo(expectedTaskCreatedAt[index])
        }
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks sorts by createdAt in ascending order`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val taskSortTestData = setupForUser(user)
        val url = "/cas1/tasks?isCompleted=true&sortBy=createdAt&sortDirection=asc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskCreatedAt = taskSortTestData.tasks.values.map { getCreatedAt(it, taskSortTestData) }
          .sortedWith(compareBy(nullsLast()) { it })

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(getCreatedAt(task, taskSortTestData)).isEqualTo(expectedTaskCreatedAt[index])
        }
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks sorts by createdAt in descending order`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val taskSortTestData = setupForUser(user)
        val url = "/cas1/tasks?isCompleted=true&sortBy=createdAt&sortDirection=desc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskCreatedAt = taskSortTestData.tasks.values.map { getCreatedAt(it, taskSortTestData) }
          .sortedWith(compareByDescending(nullsLast()) { it })

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(getCreatedAt(task, taskSortTestData)).isEqualTo(expectedTaskCreatedAt[index])
        }
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks sorts by dueAt in ascending order`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val taskSortTestData = setupForUser(user)
        val url = "/cas1/tasks?isCompleted=true&sortBy=dueAt&sortDirection=asc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskDueAt = taskSortTestData.tasks.values.map { it.dueAt }.sorted()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.dueAt).isEqualTo(expectedTaskDueAt[index])
        }
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks sorts by dueAt in descending order`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val taskSortTestData = setupForUser(user)
        val url = "/cas1/tasks?isCompleted=true&sortBy=dueAt&sortDirection=desc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskDueAt = taskSortTestData.tasks.values.map { it.dueAt }.sortedDescending()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.dueAt).isEqualTo(expectedTaskDueAt[index])
        }
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks sorts by allocatedTo in ascending order`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val taskSortTestData = setupForUser(user)
        val url = "/cas1/tasks?isCompleted=true&sortBy=allocatedTo&sortDirection=asc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskAllocatedName = taskSortTestData.tasks.values.map { it.allocatedToStaffMember!!.name }.sorted()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.allocatedToStaffMember!!.name).isEqualTo(expectedTaskAllocatedName[index])
        }
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks sorts by allocatedTo in descending order`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val taskSortTestData = setupForUser(user)
        val url = "/cas1/tasks?isCompleted=true&sortBy=allocatedTo&sortDirection=desc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskAllocatedName = taskSortTestData.tasks.values.map { it.allocatedToStaffMember!!.name }.sortedDescending()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.allocatedToStaffMember!!.name).isEqualTo(expectedTaskAllocatedName[index])
        }
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks sorts by person in ascending order`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val taskSortTestData = setupForUser(user)
        val url = "/cas1/tasks?isCompleted=true&sortBy=person&sortDirection=asc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskPersonNames = taskSortTestData.tasks.values.map { it.personName }.sorted()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.personName).isEqualTo(expectedTaskPersonNames[index])
        }
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks sorts by person in descending order`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val taskSortTestData = setupForUser(user)
        val url = "/cas1/tasks?isCompleted=true&sortBy=person&sortDirection=desc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskPersonNames = taskSortTestData.tasks.values.map { it.personName }.sortedDescending()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.personName).isEqualTo(expectedTaskPersonNames[index])
        }
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks sorts by completedAt in ascending order`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val taskSortTestData = setupForUser(user)
        val url = "/cas1/tasks?isCompleted=true&sortBy=completedAt&sortDirection=asc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskCompletedAts = taskSortTestData.tasks.values.map { it.outcomeRecordedAt }
          .sortedWith(compareBy(nullsLast()) { it })

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.outcomeRecordedAt).isEqualTo(expectedTaskCompletedAts[index])
        }
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks sorts by completedAt in descending order`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val taskSortTestData = setupForUser(user)
        val url = "/cas1/tasks?isCompleted=true&sortBy=completedAt&sortDirection=desc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskCompletedAts = taskSortTestData.tasks.values.map { it.outcomeRecordedAt }
          .sortedWith(compareByDescending(nullsLast()) { it })

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.outcomeRecordedAt).isEqualTo(expectedTaskCompletedAts[index])
        }
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks sorts by taskType in ascending order`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val taskSortTestData = setupForUser(user)
        val url = "/cas1/tasks?isCompleted=true&sortBy=taskType&sortDirection=asc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskTypes = taskSortTestData.tasks.values.map { it.taskType }.sorted()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.taskType).isEqualTo(expectedTaskTypes[index])
        }
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks sorts by expected arrival date in ascending order`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val taskSortTestData = setupForUser(user)
        val url = "/cas1/tasks?isCompleted=true&sortBy=expectedArrivalDate&sortDirection=asc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedArrivalDates = taskSortTestData.tasks.values.map { it.expectedArrivalDate.toString() }.sorted()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.expectedArrivalDate.toString()).isEqualTo(expectedArrivalDates[index])
        }
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks sorts by expected arrival date in descending order`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val taskSortTestData = setupForUser(user)
        val url = "/cas1/tasks?isCompleted=true&sortBy=expectedArrivalDate&sortDirection=desc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedArrivalDates = taskSortTestData.tasks.values.map { it.expectedArrivalDate.toString() }.sortedDescending()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.expectedArrivalDate.toString()).isEqualTo(expectedArrivalDates[index])
        }
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks sorts by taskType in descending order`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val taskSortTestData = setupForUser(user)
        val url = "/cas1/tasks?isCompleted=true&sortBy=taskType&sortDirection=desc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskTypes = taskSortTestData.tasks.values.map { it.taskType }.sortedDescending()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.taskType).isEqualTo(expectedTaskTypes[index])
        }
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks sorts by decision in ascending order`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val taskSortTestData = setupForUser(user)
        val url = "/cas1/tasks?isCompleted=true&sortBy=decision&sortDirection=asc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedDecisions = taskSortTestData.tasks.values.map { getDecision(it) }
          .sortedWith(compareBy(nullsLast()) { it })

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(getDecision(task)).isEqualTo(expectedDecisions[index])
        }
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks sorts by decision in descending order`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val taskSortTestData = setupForUser(user)
        val url = "/cas1/tasks?isCompleted=true&sortBy=decision&sortDirection=desc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedDecisions = taskSortTestData.tasks.values.map { getDecision(it) }
          .sortedWith(compareByDescending(nullsLast()) { it })

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(getDecision(task)).isEqualTo(expectedDecisions[index])
        }
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks sorts by apType in ascending order`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val taskSortTestData = setupForUser(user)
        val url = "/cas1/tasks?isCompleted=true&sortBy=apType&sortDirection=asc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskApType = taskSortTestData.tasks.values.map { it.apType.value }.sorted()

        assertThat(response).hasSize(9)
        response.forEachIndexed { index, task ->
          assertThat(task.apType.value).isEqualTo(expectedTaskApType[index])
        }
      }

      @ParameterizedTest
      @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
      fun `Get all tasks sorts by apType in descending order`(userRole: UserRole) {
        val (user, jwt) = givenAUser(roles = listOf(userRole))
        val taskSortTestData = setupForUser(user)
        val url = "/cas1/tasks?isCompleted=true&sortBy=apType&sortDirection=desc&page=1&perPage=10"

        val response = webTestClient.get()
          .uri(url)
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Task>()

        val expectedTaskApType = taskSortTestData.tasks.values.map { it.apType.value }.sortedDescending()

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

      private fun getCreatedAt(task: Task, taskSortTestData: TaskSortTestData): OffsetDateTime = when (task) {
        is AssessmentTask -> taskSortTestData.assessments[task.id]!!.createdAt
        is PlacementApplicationTask -> taskSortTestData.placementApplications[task.id]!!.createdAt
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
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA", "CAS1_JANITOR", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.EXCLUDE)
    fun `Return 403 forbidden for users with roles other than CAS1_CRU_MEMBER, CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA, CAS1_JANITOR, CAS1_AP_AREA_MANAGER`(userRole: UserRole) {
      val (user, jwt) = givenAUser(roles = listOf(userRole))

      val placementApplication = givenAPlacementApplication(
        createdByUser = user,
        allocatedToUser = user,
        crn = "CRN123",
        submittedAt = OffsetDateTime.now(),
      )

      webTestClient.get()
        .uri("/cas1/tasks/placement-application/${placementApplication.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Request without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/tasks/assessment/f601ff2d-b1e0-4878-8731-ccfa19a2ce84")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Unknown task type for an application returns 404`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          givenAnAssessmentForApprovedPremises(
            allocatedToUser = user,
            createdByUser = user,
            crn = offenderDetails.otherIds.crn,
          ) { _, application ->
            webTestClient.get()
              .uri("/cas1/tasks/unknown-task/${application.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isNotFound
          }
        }
      }
    }

    @Test
    fun `If request is for an application only returns active users with ASSESSOR role`() {
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
            .uri("/cas1/tasks/assessment/${assessment.id}")
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

    @Test
    fun `If request is for an appealed application only returns users with CAS1_APPEALS_MANAGER or CAS1_ASSESSOR role`() {
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
                    .uri("/cas1/tasks/assessment/${assessment.id}")
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

    @Test
    fun `If request is for an appealed application returns 0 users if no users with CAS1_APPEALS_MANAGER or CAS1_ASSESSOR role`() {
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
                  .uri("/cas1/tasks/assessment/${assessment.id}")
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

    @Test
    fun `If request is for a placement application that is not submitted, return not found because a task doesn't yet exist to complete`() {
      val (creatingUser, jwt) = givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER))

      val placementApplication = givenAPlacementApplication(
        createdByUser = creatingUser,
        allocatedToUser = creatingUser,
        crn = "cRN123",
        submittedAt = null,
      )

      webTestClient.get()
        .uri("/cas1/tasks/placement-application/${placementApplication.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `If request is for a placement application only returns active users with ASSESSOR role, with correct workload`() {
      // ignored, wrong role
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER))

      // ignored, inactive
      givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR), isActive = false)

      val (allocatableUser, _) = givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR))

      val (creatingUser, jwt) = givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER))

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
      // automatic, ignored
      createPlacementApplication(assessedAt = null, allocatableUser, creatingUser, crn, automatic = true)

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
      // automatic, ignored
      createPlacementApplication(OffsetDateTime.now().minusDays(1), allocatableUser, creatingUser, crn, automatic = true)

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
      // automatic, ignored
      createPlacementApplication(OffsetDateTime.now().minusDays(10), allocatableUser, creatingUser, crn, automatic = true)

      // completed after 30 days ago, ignored
      repeat(10) {
        createAssessment(OffsetDateTime.now().minusDays(31), allocatableUser, creatingUser, crn)
      }
      repeat(10) {
        createPlacementApplication(OffsetDateTime.now().minusDays(31), allocatableUser, creatingUser, crn)
      }

      webTestClient.get()
        .uri("/cas1/tasks/placement-application/${placementApplication.id}")
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

    @SuppressWarnings("LongParameterList")
    private fun createPlacementApplication(
      assessedAt: OffsetDateTime?,
      allocatedUser: UserEntity,
      createdByUser: UserEntity,
      crn: String,
      isWithdrawn: Boolean = false,
      automatic: Boolean = false,
    ) {
      givenAPlacementApplication(
        createdByUser = createdByUser,
        allocatedToUser = allocatedUser,
        submittedAt = assessedAt?.minusDays(1),
        decisionMadeAt = assessedAt,
        crn = crn,
        isWithdrawn = isWithdrawn,
        automatic = automatic,
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
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER"], mode = EnumSource.Mode.EXCLUDE)
    fun `Return 400 bad request for users with roles other than CAS1_CRU_MEMBER`(userRole: UserRole) {
      val (_, jwt) = givenAUser(roles = listOf(userRole))
      webTestClient.post()
        .uri("/cas1/tasks/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewReallocation(
            userId = UUID.randomUUID(),
          ),
        )
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `Reallocate application to different assessor without JWT returns 401`() {
      webTestClient.post()
        .uri("/cas1/tasks/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
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
    fun `Reallocate application to different assessor without CAS1_CRU_MEMBER role returns 403`() {
      givenAUser { _, jwt ->
        webTestClient.post()
          .uri("/cas1/tasks/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
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
    fun `Reallocate assessment to different assessor returns 201, creates new assessment, deallocates old one, sends emails`() {
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
                  .uri("/cas1/tasks/assessment/${existingAssessment.id}/allocations")
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

    @Test
    fun `Reallocate assessment to different assessor returns an error if the assessment has already been allocated`() {
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
                  .uri("/cas1/tasks/assessment/${existingAssessment.id}/allocations")
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

    @Test
    fun `Reallocating a placement application to different assessor returns 201, creates new placement application, deallocates old one`() {
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
                  .uri("/cas1/tasks/placement-application/${placementApplication.id}/allocations")
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
  }

  @Nested
  inner class DeallocateTaskTest : IntegrationTestBase() {

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER"], mode = EnumSource.Mode.EXCLUDE)
    fun `Return 403 forbidden for users with roles other than CAS1_CRU_MEMBER`(userRole: UserRole) {
      val (_, jwt) = givenAUser(roles = listOf(userRole))
      webTestClient.delete()
        .uri("/cas1/tasks/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Deallocate assessment without JWT returns 401 Unauthorized`() {
      webTestClient.delete()
        .uri("/cas1/tasks/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Deallocate Approved Premises assessment returns 403 Forbidden`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          givenAUser { _, _ ->
            givenAnAssessmentForApprovedPremises(
              allocatedToUser = user,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            ) { assessment, _ ->
              webTestClient.delete()
                .uri("/cas1/tasks/assessment/${assessment.id}/allocations")
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

  data class TaskSortTestData(
    val crn: String,
    val tasks: Map<UUID, Task>,
    val assessments: Map<UUID, AssessmentEntity>,
    val placementRequests: Map<UUID, PlacementRequestEntity>,
    val placementApplications: Map<UUID, PlacementApplicationEntity>,
  )
}
