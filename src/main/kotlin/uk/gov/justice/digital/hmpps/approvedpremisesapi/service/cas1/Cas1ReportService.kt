package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.Cas1ApplicationV2ReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.Cas1DailyMetricsReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.Cas1OutOfServiceBedsReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.Cas1OverduePlacementsReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.Cas1PlacementMatchingOutcomesV2ReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.Cas1PlacementReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.Cas1RequestForPlacementReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.CsvJdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.ExcelJdbcResultSetConsumer
import java.io.OutputStream
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class Cas1ReportService(
  private val cas1PlacementMatchingOutcomesV2ReportRepository: Cas1PlacementMatchingOutcomesV2ReportRepository,
  private val cas1ApplicationV2ReportRepository: Cas1ApplicationV2ReportRepository,
  private val cas1PlacementRequestReportRepository: Cas1RequestForPlacementReportRepository,
  private val cas1PlacementReportRepository: Cas1PlacementReportRepository,
  private val cas1DailyMetricsReportRepository: Cas1DailyMetricsReportRepository,
  private val cas1OutOfServiceBedsReportRepository: Cas1OutOfServiceBedsReportRepository,
  private val cas1OverduePlacementsReportRepository: Cas1OverduePlacementsReportRepository,
  private val clock: Clock,
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
    reportDateRange: ReportDateRange,
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
        startDateTimeInclusive = reportDateRange.start,
        endDateTimeInclusive = reportDateRange.end,
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

    ExcelJdbcResultSetConsumer(
      columnsToExclude = columnsToExclude,
    ).use { consumer ->
      cas1OutOfServiceBedsReportRepository.generateOutOfServiceBedsReport(
        startDate = reportDateRange.start,
        endDate = reportDateRange.end,
        jdbcResultSetConsumer = consumer,
      )

      consumer.writeBufferedWorkbook(outputStream)
    }
  }

  fun createRequestForPlacementReport(
    reportDateRange: ReportDateRange,
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
        startDateTimeInclusive = reportDateRange.start,
        endDateTimeInclusive = reportDateRange.end,
        consumer,
      )
    }
  }

  fun createPlacementMatchingOutcomesV2Report(
    reportDateRange: ReportDateRange,
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
        startDateTimeInclusive = reportDateRange.start,
        endDateTimeInclusive = reportDateRange.end,
        consumer,
      )
    }
  }

  fun createPlacementReport(
    reportDateRange: ReportDateRange,
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
        startDateTimeInclusive = reportDateRange.start,
        endDateTimeInclusive = reportDateRange.end,
        consumer,
      )
    }
  }

  fun createOverduePlacementsReport(reportDateRange: ReportDateRange, outputStream: OutputStream) {
    CsvJdbcResultSetConsumer(
      outputStream = outputStream,
      columnsToExclude = emptyList(),
    ).use { consumer ->
      cas1OverduePlacementsReportRepository.generateCas1OverduePlacementsReport(
        startDate = reportDateRange.start,
        endDate = reportDateRange.end,
        jdbcResultSetConsumer = consumer,
        currentDate = LocalDate.now(clock),
      )
    }
  }

  data class ReportDateRange(
    val start: LocalDateTime,
    val end: LocalDateTime,
  )
}
