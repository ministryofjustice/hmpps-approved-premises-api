package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Appeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewAppeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Application`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.withinSeconds
import java.time.LocalDate
import java.util.UUID

class AppealsTest : IntegrationTestBase() {
  @Test
  fun `Create new appeal without JWT returns 401`() {
    webTestClient.post()
      .uri("/applications/${UUID.randomUUID()}/appeals")
      .bodyValue(
        NewAppeal(
          appealDate = LocalDate.parse("2024-01-01"),
          appealDetail = "Some details about the appeal.",
          reviewer = "Someone Else",
          decision = AppealDecision.accepted,
          decisionDetail = "Some details about the decision.",
        ),
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Create new appeal returns 404 when application could not be found`() {
    `Given a User`(roles = listOf(UserRole.CAS1_APPEALS_MANAGER)) { _, jwt ->
      webTestClient.post()
        .uri("/applications/${UUID.randomUUID()}/appeals")
        .bodyValue(
          NewAppeal(
            appealDate = LocalDate.parse("2024-01-01"),
            appealDetail = "Some details about the appeal.",
            reviewer = "Someone Else",
            decision = AppealDecision.accepted,
            decisionDetail = "Some details about the decision.",
          ),
        )
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Create new appeal returns 403 when application is not accessible to user`() {
    `Given a User` { createdByUser, _ ->
      `Given an Application`(createdByUser) { application ->
        `Given a User`(roles = listOf(UserRole.CAS1_APPEALS_MANAGER)) { _, jwt ->
          webTestClient.post()
            .uri("/applications/${application.id}/appeals")
            .bodyValue(
              NewAppeal(
                appealDate = LocalDate.parse("2024-01-01"),
                appealDetail = "Some details about the appeal.",
                reviewer = "Someone Else",
                decision = AppealDecision.accepted,
                decisionDetail = "Some details about the decision.",
              ),
            )
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }
  }

  @Test
  fun `Create new appeal returns 403 when user does not have CAS1_APPEALS_MANAGER role`() {
    `Given a User` { userEntity, jwt ->
      `Given an Application`(userEntity) { application ->
        webTestClient.post()
          .uri("/applications/${application.id}/appeals")
          .bodyValue(
            NewAppeal(
              appealDate = LocalDate.parse("2024-01-01"),
              appealDetail = "Some details about the appeal.",
              reviewer = "Someone Else",
              decision = AppealDecision.accepted,
              decisionDetail = "Some details about the decision.",
            ),
          )
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  @Test
  fun `Create new appeal returns 400 when invalid data is provided`() {
    `Given a User`(roles = listOf(UserRole.CAS1_APPEALS_MANAGER)) { userEntity, jwt ->
      `Given an Application`(userEntity) { application ->
        webTestClient.post()
          .uri("/applications/${application.id}/appeals")
          .bodyValue(
            NewAppeal(
              appealDate = LocalDate.now().plusDays(1),
              appealDetail = "  ",
              reviewer = "\t",
              decision = AppealDecision.rejected,
              decisionDetail = "\n",
            ),
          )
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isBadRequest
      }
    }
  }

  @Test
  fun `Create new appeal returns 201 with correct body and Location header`() {
    `Given a User`(roles = listOf(UserRole.CAS1_APPEALS_MANAGER)) { userEntity, jwt ->
      `Given an Application`(userEntity) { application ->
        val result = webTestClient.post()
          .uri("/applications/${application.id}/appeals")
          .bodyValue(
            NewAppeal(
              appealDate = LocalDate.parse("2024-01-01"),
              appealDetail = "Some details about the appeal.",
              reviewer = "Someone Else",
              decision = AppealDecision.accepted,
              decisionDetail = "Some details about the decision.",
            ),
          )
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isCreated
          .returnResult(Appeal::class.java)

        assertThat(result.responseHeaders["Location"]).anyMatch {
          it.matches(Regex("/applications/${application.id}/appeals/[0-9a-f-]+"))
        }

        assertThat(result.responseBody.blockFirst()).matches {
          it.appealDate == LocalDate.parse("2024-01-01") &&
            it.appealDetail == "Some details about the appeal." &&
            it.reviewer == "Someone Else" &&
            withinSeconds(5L).matches(it.createdAt.toString()) &&
            it.applicationId == application.id &&
            it.createdByUserId == userEntity.id &&
            it.decision == AppealDecision.accepted &&
            it.decisionDetail == "Some details about the decision." &&
            it.assessmentId == null
        }
      }
    }
  }
}
