package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar

import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import java.time.LocalDate

/**
 * Per-service SAR fixture asserter.
 *
 * <p>The HMPPS SAR library interfaces
 * SarApiDataTest and SarReportTest
 * resolve their expected fixtures from a single pair of application properties
 * (`hmpps.sar.tests.expected-*.path`) configured at application context startup.
 * This works well for a service-per-repository architecture, but not for CAS,
 * where CAS1, CAS2, CAS2v2, and CAS3 are hosted within the same Spring
 * application and each service requires its own fixture set.
 *
 * <p>Each service-specific test creates an instance of this class with:
 * <ul>
 *   <li>service-specific classpath fixture locations</li>
 *   <li>service-specific `.log` filenames used when
 *       `SAR_GENERATE_ACTUAL=true` is enabled</li>
 * </ul>
 *
 * <p>Using unique output filenames ensures concurrent test runs do not overwrite
 * each other’s generated fixture output.
 *
 * <p>This implementation mirrors the behavior of
 * [uk.gov.justice.digital.hmpps.subjectaccessrequest.SarApiDataTest]
 * and
 * [uk.gov.justice.digital.hmpps.subjectaccessrequest.SarReportTest].
 */
class CasSarFixtureAsserter(
  private val sarHelper: SarIntegrationTestHelper,
  private val webTestClient: WebTestClient,
  private val expectedApiResponseResourcePath: String,
  private val expectedReportResourcePath: String,
  private val generatedApiResponseFilename: String,
  private val generatedReportFilename: String,
) {

  fun assertApiDataMatchesFixture(
    prn: String? = null,
    crn: String? = null,
    fromDate: LocalDate? = null,
    toDate: LocalDate? = null,
  ) {
    val response = sarHelper.requestSarData(prn, crn, fromDate, toDate, webTestClient)
    val actualJson = sarHelper.toJson(response)

    if (generateActual()) {
      sarHelper.saveContentToFile(actualJson, generatedApiResponseFilename)
    } else {
      assertThatJson(actualJson)
        .`as`("Response content json")
        .isEqualTo(sarHelper.getResourceAsString(expectedApiResponseResourcePath))
      assertThat(response.attachments?.isNotEmpty() == true)
        .`as`("Response has attachments")
        .isEqualTo(sarHelper.attachmentsExpected)
    }
  }

  fun assertReportMatchesFixture(
    prn: String? = null,
    crn: String? = null,
    fromDate: LocalDate? = null,
    toDate: LocalDate? = null,
  ) {
    sarHelper.stubFindPrisonNameWith("Moorland (HMP & YOI)")
    sarHelper.stubFindUserLastNameWith("Johnson")
    sarHelper.stubFindLocationNameByNomisIdWith("PROPERTY BOX 1")
    sarHelper.stubFindLocationNameByDpsIdWith("PROPERTY BOX 2")

    val dataResponse = sarHelper.requestSarData(prn, crn, fromDate, toDate, webTestClient)
    val templateResponse = sarHelper.requestSarTemplate(webTestClient)

    val renderResult = sarHelper.renderServiceReport(
      dataResponse.content,
      "1.0",
      templateResponse,
    )

    if (generateActual()) {
      sarHelper.saveContentToFile(renderResult, generatedReportFilename)
    } else {
      sarHelper.assertHtmlEquals(
        renderResult,
        sarHelper.getResourceAsString(expectedReportResourcePath),
      )
    }
  }

  private fun generateActual() = System.getenv("SAR_GENERATE_ACTUAL").toBoolean()
}
