package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import org.assertj.core.api.Assertions
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.sortBy
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.Cas2ExampleMetricsRow

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

  @Test
  fun `downloaded report is streamed as a spreadsheet`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis",
      roles = listOf("ROLE_PRISON", "ROLE_CAS2_MI"),
    )

    val expectedDataFrame = listOf(Cas2ExampleMetricsRow(id = "123", data = "example"))
      .toDataFrame()

    webTestClient.get()
      .uri("/cas2/reports/example-report")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .consumeWith {
        val actual = DataFrame
          .readExcel(it.responseBody!!.inputStream())
          .convertTo<Cas2ExampleMetricsRow>(ExcessiveColumns.Remove)
          .sortBy(Cas2ExampleMetricsRow::id)
        Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
      }
  }
}
