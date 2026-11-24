package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1TaskTransformer
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas1TasksPermissionsTest : IntegrationTestBase() {
  @Autowired
  lateinit var taskTransformer: Cas1TaskTransformer

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
            requestedDuration = 1,
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
              jsonMapper.writeValueAsString(
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
            requestedDuration = 1,
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
              jsonMapper.writeValueAsString(
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
              jsonMapper.writeValueAsString(
                expectedTasks.reversed(),
              ),
            )
        }
      }
    }
  }
}
