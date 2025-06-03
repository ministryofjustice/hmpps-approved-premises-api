package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ApplicationV2ReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DailyMetricsReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1OutOfServiceBedsReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1PlacementMatchingOutcomesV2ReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1PlacementReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1RequestForPlacementReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.CsvJdbcResultSetConsumer
import java.io.OutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class Cas1ReportService(
  private val cas1PlacementMatchingOutcomesV2ReportRepository: Cas1PlacementMatchingOutcomesV2ReportRepository,
  private val cas1ApplicationV2ReportRepository: Cas1ApplicationV2ReportRepository,
  private val cas1PlacementRequestReportRepository: Cas1RequestForPlacementReportRepository,
  private val cas1PlacementReportRepository: Cas1PlacementReportRepository,
  private val cas1DailyMetricsReportRepository: Cas1DailyMetricsReportRepository,
  private val cas1OutOfServiceBedsReportRepository: Cas1OutOfServiceBedsReportRepository,
) {

  companion object {
    val CSV_PII_COLUMN_NAMES = listOf(
      "referrer_username",
      "referrer_name",
      "applicant_reason_for_late_application_detail",
      "initial_assessor_reason_for_late_application",
      "initial_assessor_username",
      "initial_assessor_name",
      "last_appealed_assessor_username",
      "last_appealed_assessor_name",
      "matcher_username",
      "matcher_name",
    )
  }

  fun createApplicationReportV2(
    properties: MonthSpecificReportParams,
    includePii: Boolean,
    outputStream: OutputStream,
  ) {
    val columnsToExclude = if (includePii) {
      emptyList()
    } else {
      CSV_PII_COLUMN_NAMES
    }

    CsvJdbcResultSetConsumer(
      outputStream = outputStream,
      columnsToExclude = columnsToExclude,
    ).use { consumer ->
      cas1ApplicationV2ReportRepository.generateForSubmissionOrWithdrawalDate(
        startDateTimeInclusive = getFirstSecondOfMonth(properties.year, properties.month),
        endDateTimeInclusive = getLastSecondOfMonth(properties.year, properties.month),
        jbdcResultSetConsumer = consumer,
      )
    }
  }

  fun createDailyMetricsReport(reportDateRange: ReportDateRange, outputStream: OutputStream) {
    CsvJdbcResultSetConsumer(
      outputStream = outputStream,
      columnsToExclude = emptyList(),
    ).use { consumer ->
      cas1DailyMetricsReportRepository.generateCas1DailyMetricsReport(
        startDateTimeInclusive = reportDateRange.start,
        endDateTimeInclusive = reportDateRange.end,
        jdbcResultSetConsumer = consumer,

      )
    }
  }

  fun createOutOfServiceBedReport(
    reportDateRange: ReportDateRange,
    outputStream: OutputStream,
    includePii: Boolean,
  ) {
    val columnsToExclude = if (includePii) {
      emptyList()
    } else {
      listOf("notes")
    }

    CsvJdbcResultSetConsumer(
      outputStream = outputStream,
      columnsToExclude = columnsToExclude,
    ).use { consumer ->
      cas1OutOfServiceBedsReportRepository.generateOutOfServiceBedsReport(
        startDate = reportDateRange.start,
        endDate = reportDateRange.end,
        jdbcResultSetConsumer = consumer,
      )
    }
  }

  fun createRequestForPlacementReport(
    properties: MonthSpecificReportParams,
    includePii: Boolean,
    outputStream: OutputStream,
  ) {
    val columnsToExclude = if (includePii) {
      emptyList()
    } else {
      CSV_PII_COLUMN_NAMES
    }

    CsvJdbcResultSetConsumer(
      outputStream = outputStream,
      columnsToExclude = columnsToExclude,
    ).use { consumer ->
      cas1PlacementRequestReportRepository.generateForSubmissionOrWithdrawalDate(
        startDateTimeInclusive = getFirstSecondOfMonth(properties.year, properties.month),
        endDateTimeInclusive = getLastSecondOfMonth(properties.year, properties.month),
        consumer,
      )
    }
  }

  fun createPlacementMatchingOutcomesV2Report(
    properties: MonthSpecificReportParams,
    includePii: Boolean,
    outputStream: OutputStream,
  ) {
    val columnsToExclude = if (includePii) {
      emptyList()
    } else {
      CSV_PII_COLUMN_NAMES
    }

    CsvJdbcResultSetConsumer(
      outputStream = outputStream,
      columnsToExclude = columnsToExclude,
    ).use { consumer ->
      cas1PlacementMatchingOutcomesV2ReportRepository.generateForArrivalDateThisMonth(
        startDateTimeInclusive = getFirstSecondOfMonth(properties.year, properties.month),
        endDateTimeInclusive = getLastSecondOfMonth(properties.year, properties.month),
        consumer,
      )
    }
  }

  fun createPlacementReport(
    properties: MonthSpecificReportParams,
    includePii: Boolean,
    outputStream: OutputStream,
  ) {
    val columnsToExclude = if (includePii) {
      emptyList()
    } else {
      CSV_PII_COLUMN_NAMES
    }

    CsvJdbcResultSetConsumer(
      outputStream = outputStream,
      columnsToExclude = columnsToExclude,
    ).use { consumer ->
      cas1PlacementReportRepository.generatePlacementReport(
        startDateTimeInclusive = getFirstSecondOfMonth(properties.year, properties.month),
        endDateTimeInclusive = getLastSecondOfMonth(properties.year, properties.month),
        consumer,
      )
    }
  }

  data class ReportDateRange(
    val start: LocalDateTime,
    val end: LocalDateTime,
  )

  @Deprecated("Will be removed soon", replaceWith = ReplaceWith("ReportDateRange(startDate, endDate)"))
  data class MonthSpecificReportParams(
    val year: Int,
    val month: Int,
  )

  @SuppressWarnings("MagicNumber")
  private fun getFirstSecondOfMonth(year: Int, month: Int) = LocalDate.of(year, month, 1).atStartOfDay()

  @SuppressWarnings("MagicNumber")
  private fun getLastSecondOfMonth(year: Int, month: Int) = YearMonth.of(year, month).atEndOfMonth().atTime(23, 59, 59, 999999999)
}
