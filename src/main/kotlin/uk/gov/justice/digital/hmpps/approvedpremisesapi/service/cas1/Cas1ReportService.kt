package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntityReportRowRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1PlacementMatchingOutcomesReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntityReportRowRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.ApplicationReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.DailyMetricsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.LostBedsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.PlacementApplicationReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ApprovedPremisesApplicationMetricsSummaryDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.ApplicationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.Cas1PlacementMatchingOutcomesReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.DailyMetricReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.LostBedReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.PlacementApplicationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.ExcelJdbcResultSetConsumer
import java.io.OutputStream
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Service
class Cas1ReportService(
  private val applicationRepository: ApplicationRepository,
  private val applicationEntityReportRowRepository: ApplicationEntityReportRowRepository,
  private val bedRepository: BedRepository,
  private val cas1PlacementMatchingOutcomesReportRepository: Cas1PlacementMatchingOutcomesReportRepository,
  private val domainEventRepository: DomainEventRepository,
  private val lostBedsRepository: LostBedsRepository,
  private val objectMapper: ObjectMapper,
  private val placementApplicationEntityReportRowRepository: PlacementApplicationEntityReportRowRepository,
) {
  fun createApplicationReport(properties: ApplicationReportProperties, outputStream: OutputStream) {
    ApplicationReportGenerator()
      .createReport(applicationEntityReportRowRepository.generateApprovedPremisesReportRowsForCalendarMonth(properties.month, properties.year), properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }

  fun createDailyMetricsReport(properties: DailyMetricReportProperties, outputStream: OutputStream) {
    val applications = applicationRepository.findAllApprovedPremisesApplicationsCreatedInMonth(properties.month, properties.year).map {
      ApprovedPremisesApplicationMetricsSummaryDto(
        it.getCreatedAt().toLocalDateTime().toLocalDate(),
        it.getCreatedByUserId(),
      )
    }
    val domainEvents = domainEventRepository.findAllCreatedInMonth(properties.month, properties.year)

    val startDate = LocalDate.of(properties.year, properties.month, 1)
    val endDate = startDate.with(TemporalAdjusters.firstDayOfNextMonth())

    val dates = startDate.datesUntil(endDate).toList()

    DailyMetricsReportGenerator(domainEvents, applications, objectMapper)
      .createReport(dates, properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }

  fun createLostBedReport(properties: LostBedReportProperties, outputStream: OutputStream) {
    LostBedsReportGenerator(lostBedsRepository)
      .createReport(bedRepository.findAll(), properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }

  fun createPlacementApplicationReport(properties: PlacementApplicationReportProperties, outputStream: OutputStream) {
    PlacementApplicationReportGenerator()
      .createReport(placementApplicationEntityReportRowRepository.generatePlacementApplicationEntityReportRowsForCalendarMonth(properties.month, properties.year), properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }
  fun createPlacementMatchingOutcomesReport(properties: Cas1PlacementMatchingOutcomesReportProperties, outputStream: OutputStream) {
    ExcelJdbcResultSetConsumer().use { consumer ->
      cas1PlacementMatchingOutcomesReportRepository.generateReportRowsForExpectedArrivalMonth(
        properties.month,
        properties.year,
        consumer,
      )

      consumer.writeBufferedWorkbook(outputStream)
    }
  }
}
