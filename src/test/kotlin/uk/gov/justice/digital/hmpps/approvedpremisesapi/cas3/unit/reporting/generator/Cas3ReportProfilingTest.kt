package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.reporting.generator

import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3ReportType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import kotlin.text.get

@TestPropertySource(
  properties = [
    "spring.datasource.url=jdbc:postgresql://localhost:5432/dbe683abb0b1eddb61",


    "spring.jpa.show-sql=true",
    "spring.flyway.enabled=false"
  ]
)
class Cas3ReportProfilingTest : IntegrationTestBase() {

  @Test
  fun `Profile Bed Usage Report`() {
    // You may need to create or mock a user with appropriate roles (CAS3_REPORTER or CAS3_ASSESSOR)
    // and ensure they have access to the regions you are reporting on.
//    givenAUser(roles = listOf(UserRole.CAS3_REPORTER)) { userEntity, jwt ->
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("abcdef")

    val startDate = LocalDate.parse("2025-01-01")
      val endDate = LocalDate.parse("2025-06-30")
      val reportType = Cas3ReportType.bedUsage

      println("Starting profiling for $reportType...")
      val startTime = System.currentTimeMillis()
      govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()
      val responseBody = webTestClient.get()
        .uri("/cas3/reports/$reportType?startDate=$startDate&endDate=$endDate"
           + "&probationRegionId=d73ae6b5-041e-4d44-b859-b8c77567d893"
        )
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectBody<ByteArray>()
        .returnResult()
        .responseBody!!

      val duration = System.currentTimeMillis() - startTime
      println("Report $reportType took ${duration}ms")
      Files.write(Paths.get("build/reports/report.xlsx"), responseBody)
    }
  //}
}