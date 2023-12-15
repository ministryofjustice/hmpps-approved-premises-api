package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase

class Cas2ReportsTest : IntegrationTestBase() {

  @Nested
  inner class ControlsOnExternalUsers {
    @Test
    fun `downloading report is forbidden to external users without MI role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "auth",
        roles = listOf("ROLE_CAS2_ASSESSOR"),
      )

      webTestClient.get()
        .uri("/cas2/reports/example-report")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `downloading report is permitted to external users with MI role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "auth",
        roles = listOf("ROLE_CAS2_ASSESSOR", "ROLE_CAS2_MI"),
      )

      webTestClient.get()
        .uri("/cas2/reports/example-report")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
    }
  }

  @Nested
  inner class MissingJwt {
    @Test
    fun `Downloading report without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2/reports/example-report")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  @Nested
  inner class ControlsOnInternalUsers {
    @Test
    fun `downloading report is forbidden to NOMIS users without MI role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_PRISON"),
      )

      webTestClient.get()
        .uri("/cas2/reports/example-report")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `downloading report is permitted to NOMIS users with MI role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_PRISON", "ROLE_CAS2_MI"),
      )

      webTestClient.get()
        .uri("/cas2/reports/example-report")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
    }
  }
}
