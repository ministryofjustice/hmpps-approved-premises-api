package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.reporting

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_CRU_MEMBER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_REPORT_VIEWER

class Cas1ReportsTest : IntegrationTestBase() {
  val cas1Report: String = Cas1ReportName.outOfServiceBeds.value
  val approvedPremisesServiceName: String = ServiceName.approvedPremises.value

  @Test
  fun `Get report returns 400 if query parameters are missing`() {
    givenAUser(roles = listOf(CAS1_REPORT_VIEWER)) { _, jwt ->
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
    givenAUser(roles = listOf(CAS1_REPORT_VIEWER)) { _, jwt ->
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
    givenAUser(roles = listOf(CAS1_REPORT_VIEWER)) { _, jwt ->
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
    givenAUser(roles = listOf(CAS1_CRU_MEMBER)) { _, jwt ->
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
    givenAUser(roles = listOf(CAS1_REPORT_VIEWER)) { _, jwt ->
      webTestClient.get()
        .uri("/cas1/reports/doesnotexist")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", approvedPremisesServiceName)
        .exchange()
        .expectStatus()
        .is5xxServerError
    }
  }
}
