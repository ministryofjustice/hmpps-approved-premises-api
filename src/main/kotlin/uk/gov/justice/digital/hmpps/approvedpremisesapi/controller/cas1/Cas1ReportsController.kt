package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.ReportsCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.ContentType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generateStreamingResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotAllowedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ReportService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ReportService.MonthSpecificReportParams
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ReportService.ReportDateRange
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private const val MONTH_MAX = 12
private const val MONTH_MIN = 1

@Service
class Cas1ReportsController(
  private val cas1ReportService: Cas1ReportService,
  private val userAccessService: UserAccessService,
) : ReportsCas1Delegate {

  companion object {
    val TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuuMMdd_HHmm")
  }

  @SuppressWarnings("CyclomaticComplexMethod")
  override fun reportsReportNameGet(
    xServiceName: ServiceName,
    reportName: Cas1ReportName,
    year: Int?,
    month: Int?,
    startDate: LocalDate?,
    endDate: LocalDate?,
  ): ResponseEntity<StreamingResponseBody> {
    if (xServiceName !== ServiceName.approvedPremises) {
      throw NotAllowedProblem("This endpoint only supports CAS1")
    }

    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_REPORTS_VIEW)

    val reportDateRange = computeReportDateRange(year, month, startDate, endDate)
    val startDate = reportDateRange.start
    val endDate = reportDateRange.end

    // validateMonth(month) TODO()

    val monthSpecificReportParams = MonthSpecificReportParams(
      year = year ?: startDate.year,
      month = month ?: startDate.monthValue,
    )

    return when (reportName) {
      Cas1ReportName.applicationsV2 -> generateStreamingResponse(
        contentType = ContentType.CSV,
        fileName = createCas1ReportName("applications", startDate, endDate, ContentType.CSV),
      ) { outputStream ->
        cas1ReportService.createApplicationReportV2(monthSpecificReportParams, includePii = false, outputStream)
      }
      Cas1ReportName.applicationsV2WithPii -> generateStreamingResponse(
        contentType = ContentType.CSV,
        fileName = createCas1ReportName("applications-with-pii", startDate, endDate, ContentType.CSV),
      ) { outputStream ->
        userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_REPORTS_VIEW_WITH_PII)
        cas1ReportService.createApplicationReportV2(monthSpecificReportParams, includePii = true, outputStream)
      }
      Cas1ReportName.dailyMetrics -> generateStreamingResponse(
        contentType = ContentType.XLSX,
        fileName = createCas1ReportName("daily-metrics", startDate, endDate, ContentType.XLSX),
      ) { outputStream ->
        cas1ReportService.createDailyMetricsReport(reportDateRange, outputStream)
      }
      Cas1ReportName.outOfServiceBeds -> return generateStreamingResponse(
        contentType = ContentType.XLSX,
        fileName = createCas1ReportName("out-of-service-beds", startDate, endDate, ContentType.XLSX),
      ) { outputStream ->
        cas1ReportService.createOutOfServiceBedReport(monthSpecificReportParams, outputStream, includePii = false)
      }
      Cas1ReportName.outOfServiceBedsWithPii -> return generateStreamingResponse(
        contentType = ContentType.XLSX,
        fileName = createCas1ReportName("out-of-service-beds-with-pii", startDate, endDate, ContentType.XLSX),
      ) { outputStream ->
        userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_REPORTS_VIEW_WITH_PII)
        cas1ReportService.createOutOfServiceBedReport(monthSpecificReportParams, outputStream, includePii = true)
      }
      Cas1ReportName.requestsForPlacement -> generateStreamingResponse(
        contentType = ContentType.CSV,
        fileName = createCas1ReportName("requests-for-placement", startDate, endDate, ContentType.CSV),
      ) { outputStream ->
        cas1ReportService.createRequestForPlacementReport(monthSpecificReportParams, includePii = false, outputStream)
      }
      Cas1ReportName.requestsForPlacementWithPii -> generateStreamingResponse(
        contentType = ContentType.CSV,
        fileName = createCas1ReportName("requests-for-placement-with-pii", startDate, endDate, ContentType.CSV),
      ) { outputStream ->
        userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_REPORTS_VIEW_WITH_PII)
        cas1ReportService.createRequestForPlacementReport(monthSpecificReportParams, includePii = true, outputStream)
      }
      Cas1ReportName.placementMatchingOutcomesV2 -> generateStreamingResponse(
        contentType = ContentType.CSV,
        fileName = createCas1ReportName("placement-matching-outcomes", startDate, endDate, ContentType.CSV),
      ) { outputStream ->
        cas1ReportService.createPlacementMatchingOutcomesV2Report(monthSpecificReportParams, includePii = false, outputStream)
      }
      Cas1ReportName.placementMatchingOutcomesV2WithPii -> generateStreamingResponse(
        contentType = ContentType.CSV,
        fileName = createCas1ReportName("placement-matching-outcomes-with-pii", startDate, endDate, ContentType.CSV),
      ) { outputStream ->
        userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_REPORTS_VIEW_WITH_PII)
        cas1ReportService.createPlacementMatchingOutcomesV2Report(monthSpecificReportParams, includePii = true, outputStream)
      }
      Cas1ReportName.placements -> generateStreamingResponse(
        contentType = ContentType.CSV,
        fileName = createCas1ReportName("placements", startDate, endDate, ContentType.CSV),
      ) { outputStream ->
        cas1ReportService.createPlacementReport(monthSpecificReportParams, includePii = false, outputStream)
      }
      Cas1ReportName.placementsWithPii -> generateStreamingResponse(
        contentType = ContentType.CSV,
        fileName = createCas1ReportName("placements-with-pii", startDate, endDate, ContentType.CSV),
      ) { outputStream ->
        userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_REPORTS_VIEW_WITH_PII)
        cas1ReportService.createPlacementReport(monthSpecificReportParams, includePii = true, outputStream)
      }
    }
  }

  fun computeReportDateRange(
    year: Int? = null,
    month: Int? = null,
    startDate: LocalDate? = null,
    endDate: LocalDate? = null,
  ): ReportDateRange = when {
    year != null && month != null -> {
      val start = LocalDate.of(year, month, 1).atStartOfDay()
      val end = YearMonth.of(year, month).atEndOfMonth().atTime(23, 59, 59, 999999999)
      ReportDateRange(start, end)
    }

    startDate != null && endDate != null -> {
      val start = startDate.atStartOfDay()
      val end = endDate.atTime(23, 59, 59, 999999999)
      ReportDateRange(start, end)
    }
    else -> throw IllegalArgumentException("Either year/month or startDate/endDate must be provided")
  }

  private fun validateMonth(month: Int) {
    if (month < MONTH_MIN || month > MONTH_MAX) {
      throw BadRequestProblem(errorDetail = "month must be between 1 and 12")
    }
  }

  private fun createCas1ReportName(
    name: String,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    contentType: ContentType,
  ): String {
    val timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT)
    val startFormatted = startDate.format(DateTimeFormatter.ISO_DATE)
    val endFormatted = endDate.format(DateTimeFormatter.ISO_DATE)

    return "$name-$startFormatted-to-$endFormatted-$timestamp.${contentType.extension}"
  }
}
