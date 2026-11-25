package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision.ACCEPTED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1TaskTransformer
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

class Cas1TasksFilterByCompletedTest : IntegrationTestBase() {
  @Autowired
  lateinit var taskTransformer: Cas1TaskTransformer

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
      requestedDuration = 1,
    )

    val placementApplication2 = givenAPlacementApplication(
      createdByUser = otherUser,
      allocatedToUser = user,
      crn = offenderDetails.otherIds.crn,
      submittedAt = OffsetDateTime.now(),
      decision = ACCEPTED,
      expectedArrival = LocalDate.now(),
      requestedDuration = 1,
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
        jsonMapper.writeValueAsString(
          incompleteTasks,
        ),
      )
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.INCLUDE)
  fun `Get all tasks shows allows showing completed tasks`(userRole: UserRole) {
    val (user, jwt) = givenAUser(roles = listOf(userRole))
    val (_, _, completeTasks) = setupForUser(user)
    jsonMapper.setDateFormat(SimpleDateFormat("yyyy-mm-dd'T'HH:mm:ss"))

    val rawResponseBody = webTestClient.get()
      .uri("/cas1/tasks?isCompleted=true")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .returnResult<String>()
      .responseBody
      .blockFirst()

    val responseBody = jsonMapper.readValue(rawResponseBody, object : TypeReference<List<Task>>() {})

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
