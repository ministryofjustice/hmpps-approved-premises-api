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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ReportService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ReportService.ReportDateRange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserAccessService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val MONTH_MAX = 12
private const val MONTH_MIN = 1

@Service
class Cas1ReportsController(
  private val cas1ReportService: Cas1ReportService,
  private val userAccessService: Cas1UserAccessService,
) : ReportsCas1Delegate {

  companion object {
    val TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuuMMdd_HHmm")
  }

  @SuppressWarnings("CyclomaticComplexMethod")
  override fun getReportByName(
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

    validateDateInputs(year, month, startDate, endDate)

    val reportDateRange = computeReportDateRange(year, month, startDate, endDate)

    return when (reportName) {
      Cas1ReportName.applicationsV2 -> generateStreamingResponse(
        contentType = ContentType.CSV,
        fileName = createCas1ReportName("applications", reportDateRange),
      ) { outputStream ->
        cas1ReportService.createApplicationReportV2(reportDateRange, includePii = false, outputStream)
      }
      Cas1ReportName.applicationsV2WithPii -> generateStreamingResponse(
        contentType = ContentType.CSV,
        fileName = createCas1ReportName("applications-with-pii", reportDateRange),
      ) { outputStream ->
        userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_REPORTS_VIEW_WITH_PII)
        cas1ReportService.createApplicationReportV2(reportDateRange, includePii = true, outputStream)
      }
      Cas1ReportName.dailyMetrics -> generateStreamingResponse(
        contentType = ContentType.CSV,
        fileName = createCas1ReportName("daily-metrics", reportDateRange),
      ) { outputStream ->
        cas1ReportService.createDailyMetricsReport(reportDateRange, outputStream)
      }
      Cas1ReportName.outOfServiceBeds -> return generateStreamingResponse(
        contentType = ContentType.XLSX,
        fileName = createCas1ReportName("out-of-service-beds", reportDateRange, ContentType.XLSX),
      ) { outputStream ->
        cas1ReportService.createOutOfServiceBedReport(reportDateRange, outputStream, includePii = false)
      }
      Cas1ReportName.outOfServiceBedsWithPii -> return generateStreamingResponse(
        contentType = ContentType.XLSX,
        fileName = createCas1ReportName("out-of-service-beds-with-pii", reportDateRange, ContentType.XLSX),
      ) { outputStream ->
        userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_REPORTS_VIEW_WITH_PII)
        cas1ReportService.createOutOfServiceBedReport(reportDateRange, outputStream, includePii = true)
      }
      Cas1ReportName.requestsForPlacement -> generateStreamingResponse(
        contentType = ContentType.CSV,
        fileName = createCas1ReportName("requests-for-placement", reportDateRange),
      ) { outputStream ->
        cas1ReportService.createRequestForPlacementReport(reportDateRange, includePii = false, outputStream)
      }
      Cas1ReportName.requestsForPlacementWithPii -> generateStreamingResponse(
        contentType = ContentType.CSV,
        fileName = createCas1ReportName("requests-for-placement-with-pii", reportDateRange),
      ) { outputStream ->
        userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_REPORTS_VIEW_WITH_PII)
        cas1ReportService.createRequestForPlacementReport(reportDateRange, includePii = true, outputStream)
      }
      Cas1ReportName.placementMatchingOutcomesV2 -> generateStreamingResponse(
        contentType = ContentType.CSV,
        fileName = createCas1ReportName("placement-matching-outcomes", reportDateRange),
      ) { outputStream ->
        cas1ReportService.createPlacementMatchingOutcomesV2Report(reportDateRange, includePii = false, outputStream)
      }
      Cas1ReportName.placementMatchingOutcomesV2WithPii -> generateStreamingResponse(
        contentType = ContentType.CSV,
        fileName = createCas1ReportName("placement-matching-outcomes-with-pii", reportDateRange),
      ) { outputStream ->
        userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_REPORTS_VIEW_WITH_PII)
        cas1ReportService.createPlacementMatchingOutcomesV2Report(reportDateRange, includePii = true, outputStream)
      }
      Cas1ReportName.placements -> generateStreamingResponse(
        contentType = ContentType.CSV,
        fileName = createCas1ReportName("placements", reportDateRange),
      ) { outputStream ->
        cas1ReportService.createPlacementReport(reportDateRange, includePii = false, outputStream)
      }
      Cas1ReportName.placementsWithPii -> generateStreamingResponse(
        contentType = ContentType.CSV,
        fileName = createCas1ReportName("placements-with-pii", reportDateRange),
      ) { outputStream ->
        userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_REPORTS_VIEW_WITH_PII)
        cas1ReportService.createPlacementReport(reportDateRange, includePii = true, outputStream)
      }

      Cas1ReportName.overduePlacements -> generateStreamingResponse(
        contentType = ContentType.CSV,
        fileName = createCas1ReportName("overdue-placements", reportDateRange),
      ) { outputStream ->
        cas1ReportService.createOverduePlacementsReport(reportDateRange, outputStream)
      }
    }
  }

  @SuppressWarnings("MagicNumber")
  fun computeReportDateRange(
    year: Int? = null,
    month: Int? = null,
    startDate: LocalDate? = null,
    endDate: LocalDate? = null,
  ): ReportDateRange = when {
    year != null && month != null -> {
      val start = LocalDate.of(year, month, 1).atStartOfDay()
      val end = YearMonth.of(year, month).atEndOfMonth().atTime(23, 59, 59)
      ReportDateRange(start, end)
    }

    startDate != null && endDate != null -> {
      val start = startDate.atStartOfDay()
      val end = endDate.atTime(23, 59, 59)
      ReportDateRange(start, end)
    }
    else -> throw IllegalArgumentException("Either year/month or startDate/endDate must be provided")
  }

  private fun createCas1ReportName(
    name: String,
    reportDateRange: ReportDateRange,
    fileType: ContentType = ContentType.CSV,
  ): String {
    val timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT)
    val startFormatted = reportDateRange.start.format(DateTimeFormatter.ISO_DATE)
    val endFormatted = reportDateRange.end.format(DateTimeFormatter.ISO_DATE)

    return "$name-$startFormatted-to-$endFormatted-$timestamp.${fileType.extension}"
  }

  @SuppressWarnings("ThrowsCount")
  fun validateDateInputs(
    year: Int? = null,
    month: Int? = null,
    startDate: LocalDate? = null,
    endDate: LocalDate? = null,
  ) {
    when {
      areMixedInputs(year, month, startDate, endDate) ->
        throw BadRequestProblem(errorDetail = "Provide either year/month or startDate/endDate, but not both")

      isYearMonthMode(year, month) -> validateYearAndMonth(year, month)

      isDateRangeMode(startDate, endDate) -> {
        if (startDate == null) {
          throw BadRequestProblem(errorDetail = "Start date must be provided")
        }
        if (endDate == null) {
          throw BadRequestProblem(errorDetail = "End date must be provided")
        }

        validateDateRange(startDate, endDate)
      }

      else ->
        throw BadRequestProblem(errorDetail = "Either year/month or startDate/endDate must be provided")
    }
  }

  private fun areMixedInputs(year: Int?, month: Int?, startDate: LocalDate?, endDate: LocalDate?) = (year != null || month != null) && (startDate != null || endDate != null)

  private fun isYearMonthMode(year: Int?, month: Int?) = year != null || month != null

  private fun isDateRangeMode(startDate: LocalDate?, endDate: LocalDate?) = startDate != null || endDate != null

  @SuppressWarnings("MagicNumber")
  private fun validateYearAndMonth(year: Int?, month: Int?) {
    if (year == null || month == null) {
      throw BadRequestProblem(errorDetail = "Both year and month must be provided")
    }
    if (month !in MONTH_MIN..MONTH_MAX) {
      throw BadRequestProblem(errorDetail = "Month must be between 1 and 12")
    }
  }

  @SuppressWarnings("MagicNumber")
  private fun validateDateRange(startDate: LocalDate, endDate: LocalDate) {
    if (startDate.isAfter(endDate)) {
      throw BadRequestProblem(errorDetail = "Start date cannot be after end date")
    }
    if (ChronoUnit.DAYS.between(startDate, endDate) > 366) {
      throw BadRequestProblem(errorDetail = "The date range must not exceed one year")
    }
  }
}
