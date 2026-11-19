package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.reporting.generator

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.TransitionalAccommodationReferralReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.TransitionalAccommodationReferralReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.TransitionalAccommodationReferralReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.TransitionalAccommodationReferralReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.TransitionalAccommodationReferralReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import java.time.Instant
import java.time.LocalDate

class TransitionalAccommodationReferralReportGeneratorTest {
  @ParameterizedTest
  @MethodSource("rejectionCases")
  fun `rejection fields depend on the assessment decision`(
    decision: AssessmentDecision?,
    reason: String?,
    detail: String?,
    expectedRejected: String,
    expectedReason: String?,
    expectedDetail: String?,
  ) {
    val referralData = mockk<TransitionalAccommodationReferralReportData>(relaxed = true)
    every { referralData.referralCreatedDate } returns Instant.parse("2024-01-01T00:00:00Z")
    every { referralData.assessmentDecision } returns decision?.name
    every { referralData.referralRejectionReason } returns reason
    every { referralData.referralRejectionReasonDetail } returns detail
    every { referralData.assessmentSubmittedDate } returns null
    val input = TransitionalAccommodationReferralReportDataAndPersonInfo(
      referralData,
      PersonSummaryInfoResult.Success.Full("X123456", CaseSummaryFactory().produce(), tier = null),
    )
    val properties = TransitionalAccommodationReferralReportProperties(null, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31))

    val report = TransitionalAccommodationReferralReportGenerator().createReport(listOf(input), properties)

    assertThat(report.rowsCount()).isEqualTo(1)
    assertThat(report[0][TransitionalAccommodationReferralReportRow::referralRejected]).isEqualTo(expectedRejected)
    assertThat(report[0][TransitionalAccommodationReferralReportRow::rejectionReason]).isEqualTo(expectedReason)
    assertThat(report[0][TransitionalAccommodationReferralReportRow::rejectionReasonExplained]).isEqualTo(expectedDetail)
  }

  companion object {
    @JvmStatic
    fun rejectionCases() = listOf(
      Arguments.of(AssessmentDecision.REJECTED, "Reason", "Details", "Yes", "Reason", "Details"),
      Arguments.of(AssessmentDecision.REJECTED, null, null, "Yes", null, null),
      Arguments.of(AssessmentDecision.REJECTED, null, "Details", "Yes", null, "Details"),
      Arguments.of(AssessmentDecision.ACCEPTED, "Reason", "Details", "No", null, null),
      Arguments.of(null, "Reason", "Details", "No", null, null),
    )
  }
}
