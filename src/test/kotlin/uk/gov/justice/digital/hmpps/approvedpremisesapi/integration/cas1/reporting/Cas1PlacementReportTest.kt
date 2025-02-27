package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.reporting

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.toList
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.reporting.Cas1PlacementReportTest.Constants.REPORT_MONTH
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.reporting.Cas1PlacementReportTest.Constants.REPORT_YEAR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole

class Cas1PlacementReportTest : InitialiseDatabasePerClassTestBase() {

  lateinit var assessor: UserEntity
  lateinit var assessorJwt: String

  lateinit var assessmentSchema: ApprovedPremisesAssessmentJsonSchemaEntity
  lateinit var applicationSchema: ApprovedPremisesApplicationJsonSchemaEntity

  object Constants {
    const val REPORT_MONTH = 2
    const val REPORT_YEAR = 2024
  }

  @BeforeAll
  fun setup() {
    govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

    val assessorDetails = givenAUser(
      roles = listOf(UserRole.CAS1_ASSESSOR, UserRole.CAS1_MATCHER),
      qualifications = UserQualification.entries,
      staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "ASSESSOR1"),
    )
    assessor = assessorDetails.first
    assessorJwt = assessorDetails.second

    applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist { withDefaults() }
    assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist { withDefaults() }
  }

  @Test
  fun `Get report is empty if no space bookings`() {
    givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->

      webTestClient.get()
        .uri(getReportUrl(Cas1ReportName.placements, year = REPORT_YEAR - 1, month = REPORT_MONTH))
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valuesMatch("content-disposition", "attachment; filename=\"placements-2023-02-[0-9_]*.csv\"")
        .expectBody()
        .consumeWith {
          val actual = DataFrame
            .readCSV(it.responseBody!!.inputStream())
            .convertTo<PlacementReportRow>(ExcessiveColumns.Remove)
            .toList()

          assertThat(actual.size).isEqualTo(0)
        }
    }
  }

  @Test
  fun `Permission denied if trying to access report with PII without correct role`() {
    givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->
      webTestClient.get()
        .uri(getReportUrl(Cas1ReportName.placementsWithPii, year = REPORT_YEAR, month = REPORT_MONTH))
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get report returns OK with correct applications, excluding PII`() {
    givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->

      webTestClient.get()
        .uri(getReportUrl(Cas1ReportName.placements, year = REPORT_YEAR, month = REPORT_MONTH))
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valuesMatch("content-disposition", "attachment; filename=\"placements-2024-02-[0-9_]*.csv\"")
        .expectBody()
        .consumeWith {
          val actual = DataFrame
            .readCSV(it.responseBody!!.inputStream())
            .convertTo<PlacementReportRow>(ExcessiveColumns.Remove)
            .toList()

          assertThat(actual.size).isEqualTo(0)
        }
    }
  }

  private fun getReportUrl(reportName: Cas1ReportName, year: Int, month: Int) = "/cas1/reports/${reportName.value}?year=$year&month=$month"
}

@SuppressWarnings("ConstructorParameterNaming")
data class PlacementReportRow(
  val placement_id: String?,
  val premises_name: String?,
  val premises_region: String?,
  val expected_arrival_date: String?,
  val expected_duration_nights: String?,
  val expected_departure_date: String?,
  val actual_arrival_date_time: String?,
  val actual_departure_date_time: String?,
  val actual_duration_nights: String?,
  val departure_reason: String?,
  val departure_move_on_category: String?,
  val non_arrival_recorded_date_time: String?,
  val non_arrival_reason: String?,
  val placement_withdrawn_date_time: String?,
  val placement_withdrawn_reason: String?,
  val criteria: String?,
  val placement_request_id: String?,
  val matcher_cru: String?,
  val matcher_username: String?,
  val match_outcome: String?,
  val crn: String?,
  val request_for_placement_id: String?,
  val request_for_placement_type: String?,
  val requested_arrival_date: String?,
  val first_match_attempt_date_time: String?,
  val first_successful_match_attempt_date_time: String?,
  val first_successful_match_attempt_premises_name: String?,
  val last_successful_match_attempt_date_time: String?,
  val last_successful_match_attempt_premises_name: String?,
)
