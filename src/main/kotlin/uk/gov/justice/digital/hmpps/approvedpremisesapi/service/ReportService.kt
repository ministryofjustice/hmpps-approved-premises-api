package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntityReportRowRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.ApplicationReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BedUsageReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BedUtilisationReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BookingsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.DailyMetricsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.LostBedsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.ReferralsMetricsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.ApplicationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUsageReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUtilisationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.DailyMetricReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.LostBedReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.ReferralsMetricsProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import java.io.OutputStream
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Service
class ReportService(
  private val bookingRepository: BookingRepository,
  private val bedRepository: BedRepository,
  private val lostBedsRepository: LostBedsRepository,
  private val bookingTransformer: BookingTransformer,
  private val workingDayCountService: WorkingDayCountService,
  private val applicationEntityReportRowRepository: ApplicationEntityReportRowRepository,
  private val offenderService: OffenderService,
  private val applicationRepository: ApplicationRepository,
  private val domainEventRepository: DomainEventRepository,
  private val assessmentRepository: AssessmentRepository,
  private val objectMapper: ObjectMapper,
) {
  fun createBookingsReport(properties: BookingsReportProperties, outputStream: OutputStream) {
    val startOfMonth = LocalDate.of(properties.year, properties.month, 1)
    val endOfMonth = LocalDate.of(properties.year, properties.month, startOfMonth.month.length(startOfMonth.isLeapYear))

    val bookingsInScope = bookingRepository.findAllByOverlappingDate(startOfMonth, endOfMonth)

    BookingsReportGenerator()
      .createReport(bookingsInScope, properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }

  fun createBedUsageReport(properties: BedUsageReportProperties, outputStream: OutputStream) {
    BedUsageReportGenerator(bookingTransformer, bookingRepository, lostBedsRepository, workingDayCountService)
      .createReport(bedRepository.findAll(), properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }

  fun createBedUtilisationReport(properties: BedUtilisationReportProperties, outputStream: OutputStream) {
    BedUtilisationReportGenerator(bookingRepository, lostBedsRepository, workingDayCountService)
      .createReport(bedRepository.findAll(), properties)
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

  fun createCas1ApplicationPerformanceReport(properties: ApplicationReportProperties, outputStream: OutputStream) {
    ApplicationReportGenerator(offenderService)
      .createReport(applicationEntityReportRowRepository.generateApprovedPremisesReportRowsForCalendarMonth(properties.month, properties.year), properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }

  fun createDailyMetricsReport(properties: DailyMetricReportProperties, outputStream: OutputStream) {
    val applications = applicationRepository.findAllApprovedPremisesApplicationsCreatedInMonth(properties.month, properties.year)
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

  fun <T : Any> createReferralsMetricsReport(properties: ReferralsMetricsProperties, outputStream: OutputStream, categories: List<T>) {
    val referrals = assessmentRepository.findAllCreatedInMonthAndYear(properties.month, properties.year)

    ReferralsMetricsReportGenerator<T>(referrals, workingDayCountService)
      .createReport(categories, properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }
}
