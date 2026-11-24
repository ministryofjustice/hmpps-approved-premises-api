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

class Cas1TasksFilterByUserTest : IntegrationTestBase() {
  @Autowired
  lateinit var taskTransformer: Cas1TaskTransformer

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
      requestedDuration = 1,
    )

    givenAPlacementApplication(
      createdByUser = user,
      allocatedToUser = otherUser,
      crn = offenderDetails.otherIds.crn,
      submittedAt = OffsetDateTime.now(),
      expectedArrival = LocalDate.now(),
      requestedDuration = 1,
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
        jsonMapper.writeValueAsString(
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
        jsonMapper.writeValueAsString(
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
        jsonMapper.writeValueAsString(
          expectedTasks,
        ),
      )
  }
}
