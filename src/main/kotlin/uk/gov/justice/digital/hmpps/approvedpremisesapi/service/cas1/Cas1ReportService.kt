package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ApplicationV2ReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1PlacementMatchingOutcomesV2ReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1PlacementReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1RequestForPlacementReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.Cas1OutOfServiceBedsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.Cas1OutOfServiceBedsReportGenerator.Cas1BedIdentifier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.DailyMetricsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ApprovedPremisesApplicationMetricsSummaryDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.CsvJdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate
import java.io.OutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

@Service
class Cas1ReportService(
  private val applicationRepository: ApplicationRepository,
  private val cas1PlacementMatchingOutcomesV2ReportRepository: Cas1PlacementMatchingOutcomesV2ReportRepository,
  private val cas1ApplicationV2ReportRepository: Cas1ApplicationV2ReportRepository,
  private val cas1PlacementRequestReportRepository: Cas1RequestForPlacementReportRepository,
  private val domainEventRepository: DomainEventRepository,
  private val cas1OutOfServiceBedRepository: Cas1OutOfServiceBedRepository,
  private val domainEventService: Cas1DomainEventService,
  private val cas1PlacementReportRepository: Cas1PlacementReportRepository,
) {

  companion object {
    val PII_COLUMN_NAMES = listOf(
      "referrer_username",
      "referrer_name",
      "applicant_reason_for_late_application_detail",
      "initial_assessor_reason_for_late_application",
      "initial_assessor_username",
      "initial_assessor_name",
      "last_appealed_assessor_username",
      "matcher_username",
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
      PII_COLUMN_NAMES
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

  fun createDailyMetricsReport(properties: MonthSpecificReportParams, outputStream: OutputStream) {
    val applications = applicationRepository.findAllApprovedPremisesApplicationsCreatedInMonth(properties.month, properties.year).map {
      ApprovedPremisesApplicationMetricsSummaryDto(
        it.getCreatedAt().toLocalDate(),
        it.getCreatedByUserId(),
      )
    }
    val domainEvents = domainEventRepository.findAllCreatedInMonth(properties.month, properties.year)

    val startDate = LocalDate.of(properties.year, properties.month, 1)
    val endDate = startDate.with(TemporalAdjusters.firstDayOfNextMonth())

    val dates = startDate.datesUntil(endDate).toList()

    DailyMetricsReportGenerator(domainEvents, applications, domainEventService)
      .createReport(dates, properties)
      .writeExcel(
        outputStream = outputStream,
        factory = WorkbookFactory.create(true),
      )
  }

  fun createOutOfServiceBedReport(properties: MonthSpecificReportParams, outputStream: OutputStream) {
    Cas1OutOfServiceBedsReportGenerator(cas1OutOfServiceBedRepository)
      .createReport(cas1OutOfServiceBedRepository.findBedIdsWithAtLeastOneOutOfServiceBedRecord().map { Cas1BedIdentifier(it) }, properties)
      .writeExcel(
        outputStream = outputStream,
        factory = WorkbookFactory.create(true),
      )
  }

  fun createRequestForPlacementReport(
    properties: MonthSpecificReportParams,
    includePii: Boolean,
    outputStream: OutputStream,
  ) {
    val columnsToExclude = if (includePii) {
      emptyList()
    } else {
      PII_COLUMN_NAMES
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
      PII_COLUMN_NAMES
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
      PII_COLUMN_NAMES
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

  data class MonthSpecificReportParams(
    val year: Int,
    val month: Int,
  )

  @SuppressWarnings("MagicNumber")
  private fun getFirstSecondOfMonth(year: Int, month: Int) = LocalDate.of(year, month, 1).atStartOfDay()

  @SuppressWarnings("MagicNumber")
  private fun getLastSecondOfMonth(year: Int, month: Int) = YearMonth.of(year, month).atEndOfMonth().atTime(23, 59, 59, 999999999)
}
