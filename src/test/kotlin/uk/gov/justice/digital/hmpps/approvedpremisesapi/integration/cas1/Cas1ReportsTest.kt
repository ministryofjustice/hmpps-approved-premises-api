package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_ADMIN
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_ASSESSOR

class Cas1ReportsTest : IntegrationTestBase() {
  val cas1Report: String = Cas1ReportName.lostBeds.value
  val approvedPremisesServiceName: String = ServiceName.approvedPremises.value

  @Test
  fun `Get report returns 400 if query parameters are missing`() {
    `Given a User`(roles = listOf(CAS1_ADMIN)) { _, jwt ->
      webTestClient.get()
        .uri("/cas1/reports/$cas1Report")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", approvedPremisesServiceName)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.detail").isEqualTo("Missing required query parameter year")
    }
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 13])
  fun `Get report returns 400 if month provided is invalid`(month: Int) {
    `Given a User`(roles = listOf(CAS1_ADMIN)) { _, jwt ->
      webTestClient.get()
        .uri("/cas1/reports/$cas1Report?year=2024&month=$month")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", approvedPremisesServiceName)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.detail").isEqualTo("month must be between 1 and 12")
    }
  }

  @Test
  fun `Get report returns 405 Not Allowed if serviceName header is not approvedPremises`() {
    `Given a User`(roles = listOf(CAS3_ASSESSOR)) { _, jwt ->
      webTestClient.get()
        .uri("/cas1/reports/$cas1Report?year=2023&month=4")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.cas2.value)
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("$.detail").isEqualTo("This endpoint only supports CAS1")
    }
  }

  @Test
  fun `Get report returns 403 Forbidden if user does not have CAS1 report access`() {
    `Given a User`(roles = listOf(CAS3_ASSESSOR)) { _, jwt ->
      webTestClient.get()
        .uri("/cas1/reports/$cas1Report?year=2023&month=4")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", approvedPremisesServiceName)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get report returns Server Error if report specified is invalid`() {
    `Given a User`(roles = listOf(CAS1_ADMIN)) { _, jwt ->
      webTestClient.get()
        .uri("/cas1/reports/doesnotexist")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", approvedPremisesServiceName)
        .exchange()
        .expectStatus()
        .is5xxServerError
    }
  }

  @ParameterizedTest
  @EnumSource(value = Cas1ReportName::class)
  fun `Get report returns OK response if user role is valid and parameters are valid `(reportName: Cas1ReportName) {
    `Given a User`(roles = listOf(CAS1_ADMIN)) { _, jwt ->
      webTestClient.get()
        .uri("/cas1/reports/$reportName?year=2024&month=1")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", approvedPremisesServiceName)
        .exchange()
        .expectStatus()
        .isOk
    }
  }
}
