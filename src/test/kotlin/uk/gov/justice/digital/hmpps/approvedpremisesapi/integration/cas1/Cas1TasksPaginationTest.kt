package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.math.ceil

class Cas1TasksPaginationTest : IntegrationTestBase() {
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
        requestedDuration = 1,
      )
    }

    repeat(counts[TaskType.placementApplication]!!["unallocated"]!!) {
      givenAPlacementApplication(
        createdByUser = user,
        crn = offenderDetails.otherIds.crn,
        submittedAt = OffsetDateTime.now(),
        expectedArrival = LocalDate.now(),
        requestedDuration = 1,
      )
    }

    // withdrawn, ignored
    givenAPlacementApplication(
      createdByUser = user,
      crn = offenderDetails.otherIds.crn,
      submittedAt = OffsetDateTime.now(),
      isWithdrawn = true,
      expectedArrival = LocalDate.now(),
      requestedDuration = 1,
    )

    // automatic, ignored
    givenAPlacementApplication(
      createdByUser = user,
      crn = offenderDetails.otherIds.crn,
      submittedAt = OffsetDateTime.now(),
      expectedArrival = LocalDate.now(),
      requestedDuration = 1,
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
