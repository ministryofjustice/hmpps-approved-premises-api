package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision.ACCEPTED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision.REJECTED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1TaskTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class Cas1TasksSortByTest : IntegrationTestBase() {

  @Autowired
  lateinit var taskTransformer: Cas1TaskTransformer

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
      apType = ApprovedPremisesType.ESAP,
    )
    givenACas1SpaceBooking(
      placementRequest = placementRequest1,
    )

    val (placementRequest2, _) = givenAPlacementRequest(
      assessmentAllocatedTo = otherUser,
      createdByUser = user,
      crn = offenderDetails.otherIds.crn,
      dueAt = OffsetDateTime.now().randomDateTimeBefore(14).truncatedTo(ChronoUnit.MICROS),
      apType = ApprovedPremisesType.RFAP,
    )
    givenACas1SpaceBooking(
      placementRequest = placementRequest2,
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
      requestedDuration = 1,
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
      requestedDuration = 1,
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

  data class TaskSortTestData(
    val crn: String,
    val tasks: Map<UUID, Task>,
    val assessments: Map<UUID, AssessmentEntity>,
    val placementRequests: Map<UUID, PlacementRequestEntity>,
    val placementApplications: Map<UUID, PlacementApplicationEntity>,
  )
}
