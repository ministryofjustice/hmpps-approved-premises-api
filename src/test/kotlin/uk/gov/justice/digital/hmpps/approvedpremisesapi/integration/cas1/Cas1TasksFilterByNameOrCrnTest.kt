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

class Cas1TasksFilterByNameOrCrnTest : IntegrationTestBase() {
  private lateinit var nameMatchTasks: Map<TaskType, Task>
  private lateinit var crnMatchTasks: Map<TaskType, Task>

  @Autowired
  lateinit var taskTransformer: Cas1TaskTransformer

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
      requestedDuration = 1,
    )

    val placementApplication2 = givenAPlacementApplication(
      createdByUser = user,
      allocatedToUser = user,
      crn = offenderDetails2.otherIds.crn,
      submittedAt = OffsetDateTime.now(),
      name = "ANOTHER",
      expectedArrival = LocalDate.now(),
      requestedDuration = 1,
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
        jsonMapper.writeValueAsString(
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
        jsonMapper.writeValueAsString(
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
        jsonMapper.writeValueAsString(
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
        jsonMapper.writeValueAsString(
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
        jsonMapper.writeValueAsString(
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
        jsonMapper.writeValueAsString(
          expectedTasks,
        ),
      )
  }
}
