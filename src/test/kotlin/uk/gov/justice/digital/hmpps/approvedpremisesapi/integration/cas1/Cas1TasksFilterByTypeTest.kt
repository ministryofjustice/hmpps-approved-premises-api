package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1TaskTransformer
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas1TasksFilterByTypeTest : IntegrationTestBase() {
  @Autowired
  lateinit var taskTransformer: Cas1TaskTransformer

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
      requestedDuration = 1,
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
        jsonMapper.writeValueAsString(
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
        jsonMapper.writeValueAsString(
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
        jsonMapper.writeValueAsString(
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
        jsonMapper.writeValueAsString(
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
        jsonMapper.writeValueAsString(
          expectedTasks,
        ),
      )
  }
}
