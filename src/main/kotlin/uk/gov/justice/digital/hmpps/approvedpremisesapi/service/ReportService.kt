package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.collections4.ListUtils
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName.temporaryAccommodation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntityReportRowRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelinessEntityRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntityReportRowRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementMatchingOutcomesEntityReportRowRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.ApplicationReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BedUsageReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BedUtilisationReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BookingsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.Cas1PlacementMatchingOutcomesReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.DailyMetricsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.LostBedsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.PlacementApplicationReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.PlacementMetricsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.ReferralsMetricsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ApprovedPremisesApplicationMetricsSummaryDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ReferralsDataDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.TierCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.ApplicationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUsageReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUtilisationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.Cas1PlacementMatchingOutcomesReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.DailyMetricReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.LostBedReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.PlacementApplicationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.PlacementMetricsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.ReferralsMetricsProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BookingsReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import java.io.OutputStream
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.stream.Collectors

@Service
class ReportService(
  private val bookingRepository: BookingRepository,
  private val bedRepository: BedRepository,
  private val lostBedsRepository: LostBedsRepository,
  private val bookingTransformer: BookingTransformer,
  private val workingDayCountService: WorkingDayCountService,
  private val applicationEntityReportRowRepository: ApplicationEntityReportRowRepository,
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val applicationRepository: ApplicationRepository,
  private val domainEventRepository: DomainEventRepository,
  private val assessmentRepository: AssessmentRepository,
  private val timelinessEntityRepository: ApplicationTimelinessEntityRepository,
  private val bookingsReportRepository: BookingsReportRepository,
  private val placementApplicationEntityReportRowRepository: PlacementApplicationEntityReportRowRepository,
  private val placementMatchingOutcomesEntityReportRowRepository: PlacementMatchingOutcomesEntityReportRowRepository,
  private val objectMapper: ObjectMapper,
  @Value("\${cas3-report.end-date-override:0}") private val cas3EndDateOverride: Int,
  @Value("\${cas3-report.crn-search-limit:400}") private val numberOfCrn: Int,
) {
  fun createBookingsReport(properties: BookingsReportProperties, outputStream: OutputStream) {
    val startOfMonth = LocalDate.of(properties.year, properties.month, 1)
    val endOfMonth = if (properties.serviceName == temporaryAccommodation && cas3EndDateOverride != 0) {
      startOfMonth.plusMonths(cas3EndDateOverride.toLong())
    } else {
      LocalDate.of(properties.year, properties.month, startOfMonth.month.length(startOfMonth.isLeapYear))
    }

    val bookingsInScope = bookingsReportRepository.findAllByOverlappingDate(
      startOfMonth,
      endOfMonth,
      properties.serviceName.value,
      properties.probationRegionId,
    )

    val crns = bookingsInScope.map { it.crn }.distinct().sorted()
    val personInfos = splitAndRetrievePersonInfo(crns.toSet())

    val reportData = bookingsInScope.map {
      val personInfo = personInfos[it.crn] ?: PersonSummaryInfoResult.Unknown(it.crn)
      BookingsReportDataAndPersonInfo(it, personInfo)
    }

    BookingsReportGenerator()
      .createReport(reportData, properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }

  fun createBedUsageReport(properties: BedUsageReportProperties, outputStream: OutputStream) {
    BedUsageReportGenerator(bookingTransformer, bookingRepository, lostBedsRepository, workingDayCountService, cas3EndDateOverride)
      .createReport(bedRepository.findAll(), properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }

  fun createBedUtilisationReport(properties: BedUtilisationReportProperties, outputStream: OutputStream) {
    BedUtilisationReportGenerator(bookingRepository, lostBedsRepository, workingDayCountService, cas3EndDateOverride)
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
    ApplicationReportGenerator()
      .createReport(applicationEntityReportRowRepository.generateApprovedPremisesReportRowsForCalendarMonth(properties.month, properties.year), properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }

  fun createCas1ApplicationReferralsReport(properties: ApplicationReportProperties, outputStream: OutputStream) {
    ApplicationReportGenerator()
      .createReport(applicationEntityReportRowRepository.generateApprovedPremisesReferralReportRowsForCalendarMonth(properties.month, properties.year), properties)
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

  fun <T : Any> createReferralsMetricsReport(properties: ReferralsMetricsProperties, outputStream: OutputStream, categories: List<T>) {
    val referrals = assessmentRepository.findAllReferralsDataForMonthAndYear(properties.month, properties.year).map {
      ReferralsDataDto(
        it.getTier(),
        it.getIsEsapApplication(),
        it.getIsPipeApplication(),
        it.getDecision(),
        it.getApplicationSubmittedAt()?.toLocalDateTime()?.toLocalDate(),
        it.getAssessmentSubmittedAt()?.toLocalDateTime()?.toLocalDate(),
        it.getRejectionRationale(),
        it.getReleaseType(),
        it.getClarificationNoteCount(),
      )
    }

    ReferralsMetricsReportGenerator<T>(referrals, workingDayCountService)
      .createReport(categories, properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }

  fun createPlacementMetricsReport(properties: PlacementMetricsReportProperties, outputStream: OutputStream) {
    val timelinessEntities = timelinessEntityRepository.findAllForMonthAndYear(properties.month, properties.year)
    val tiers = TierCategory.entries

    PlacementMetricsReportGenerator(timelinessEntities, workingDayCountService)
      .createReport(tiers, properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }

  fun createCas1PlacementApplicationReport(properties: PlacementApplicationReportProperties, outputStream: OutputStream) {
    PlacementApplicationReportGenerator()
      .createReport(placementApplicationEntityReportRowRepository.generatePlacementApplicationEntityReportRowsForCalendarMonth(properties.month, properties.year), properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }

  fun createCas1PlacementMatchingOutcomesReport(properties: Cas1PlacementMatchingOutcomesReportProperties, outputStream: OutputStream) {
    Cas1PlacementMatchingOutcomesReportGenerator()
      .createReport(placementMatchingOutcomesEntityReportRowRepository.generateReportRowsForExpectedArrivalMonth(properties.month, properties.year), properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }

  private fun splitAndRetrievePersonInfo(crns: Set<String>): Map<String, PersonSummaryInfoResult> {
    val deliusUsername = userService.getUserForRequest().deliusUsername

    val crnMap = ListUtils.partition(crns.toList(), numberOfCrn)
      .stream().map { crns ->
        offenderService.getOffenderSummariesByCrns(crns.toSet(), deliusUsername).associateBy { it.crn }
      }.collect(Collectors.toList())

    return crnMap.flatMap { it.toList() }.toMap()
  }
}
