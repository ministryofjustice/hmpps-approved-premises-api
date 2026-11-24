package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1TaskTransformer
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas1TasksFilterByCruManagementAreaTest : IntegrationTestBase() {
  @Autowired
  lateinit var taskTransformer: Cas1TaskTransformer

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
      requestedDuration = 1,
    )

    givenAPlacementApplication(
      createdByUser = user,
      allocatedToUser = user,
      crn = offenderDetails.otherIds.crn,
      submittedAt = OffsetDateTime.now(),
      cruManagementArea = cruArea2,
      expectedArrival = LocalDate.now(),
      requestedDuration = 1,
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
        jsonMapper.writeValueAsString(
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
        jsonMapper.writeValueAsString(
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
        jsonMapper.writeValueAsString(
          expectedTasks,
        ),
      )
  }
}
