package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1TaskTransformer
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas1TasksFilterByQualificationTest : IntegrationTestBase() {

  @Autowired
  lateinit var taskTransformer: Cas1TaskTransformer

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
        requestedDuration = 1,
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
        jsonMapper.writeValueAsString(
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
        jsonMapper.writeValueAsString(
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
        jsonMapper.writeValueAsString(
          expectedTasks,
        ),
      )
  }
}
