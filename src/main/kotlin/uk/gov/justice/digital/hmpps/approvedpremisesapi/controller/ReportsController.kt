package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.ReportsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotAllowedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.ApplicationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUsageReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUtilisationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.Cas1PlacementMatchingOutcomesReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.DailyMetricReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.LostBedReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.PlacementApplicationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ReportService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3ReportService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class ReportsController(
  private val cas1ReportService: Cas1ReportService,
  private val cas3ReportService: Cas3ReportService,
  private val userAccessService: UserAccessService,
  private val userService: UserService,
) : ReportsApiDelegate {

  companion object {
    val TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuuMMdd_HHmm")
  }

  override fun reportsBookingsGet(xServiceName: ServiceName, year: Int, month: Int, probationRegionId: UUID?): ResponseEntity<StreamingResponseBody> {
    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }

    validateParameters(probationRegionId, month)

    val startDate = LocalDate.of(year, month, 1)
    val endDate = LocalDate.of(year, month, startDate.month.length(startDate.isLeapYear))
    val properties = BookingsReportProperties(xServiceName, probationRegionId, startDate, endDate)

    return generateXlsxStreamingResponse { outputStream ->
      cas3ReportService.createBookingsReport(properties, outputStream)
    }
  }

  override fun reportsBedUsageGet(xServiceName: ServiceName, year: Int, month: Int, probationRegionId: UUID?): ResponseEntity<StreamingResponseBody> {
    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }

    validateParameters(probationRegionId, month)
    val startDate = LocalDate.of(year, month, 1)
    val endDate = LocalDate.of(year, month, startDate.month.length(startDate.isLeapYear))
    val properties = BedUsageReportProperties(xServiceName, probationRegionId, startDate, endDate)

    return generateXlsxStreamingResponse { outputStream ->
      cas3ReportService.createBedUsageReport(properties, outputStream)
    }
  }

  override fun reportsBedUtilisationGet(xServiceName: ServiceName, year: Int, month: Int, probationRegionId: UUID?): ResponseEntity<StreamingResponseBody> {
    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }

    validateParameters(probationRegionId, month)
    val startDate = LocalDate.of(year, month, 1)
    val endDate = LocalDate.of(year, month, startDate.month.length(startDate.isLeapYear))
    val properties = BedUtilisationReportProperties(xServiceName, probationRegionId, startDate, endDate)

    return generateXlsxStreamingResponse { outputStream ->
      cas3ReportService.createBedUtilisationReport(properties, outputStream)
    }
  }

  @Deprecated("Use Cas1ReportsController")
  override fun reportsLostBedsGet(xServiceName: ServiceName, year: Int, month: Int, probationRegionId: UUID?): ResponseEntity<StreamingResponseBody> {
    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }

    validateParameters(probationRegionId, month)

    val properties = LostBedReportProperties(xServiceName, probationRegionId, year, month)

    return generateStreamingResponse(
      contentType = ContentType.XLSX,
      fileName = createCas1ReportName("lost-beds", year, month, ContentType.XLSX),
    ) { outputStream ->
      cas1ReportService.createLostBedReport(properties, outputStream)
    }
  }

  @Deprecated("Use Cas1ReportsController")
  override fun reportsApplicationsGet(xServiceName: ServiceName, year: Int, month: Int): ResponseEntity<StreamingResponseBody> {
    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }

    val user = userService.getUserForRequest()

    validateParameters(null, month)

    val properties = ApplicationReportProperties(xServiceName, year, month, user.deliusUsername)

    return generateStreamingResponse(
      contentType = ContentType.XLSX,
      fileName = createCas1ReportName("applications", year, month, ContentType.XLSX),
    ) { outputStream ->
      cas1ReportService.createApplicationReport(properties, outputStream)
    }
  }

  @Deprecated("Use Cas1ReportsController")
  override fun reportsDailyMetricsGet(xServiceName: ServiceName, year: Int, month: Int): ResponseEntity<StreamingResponseBody> {
    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }

    if (xServiceName !== ServiceName.approvedPremises) {
      throw NotAllowedProblem("This endpoint only supports CAS1")
    }

    val properties = DailyMetricReportProperties(xServiceName, year, month)

    return generateStreamingResponse(
      contentType = ContentType.XLSX,
      fileName = createCas1ReportName("daily-metrics", year, month, ContentType.XLSX),
    ) { outputStream ->
      cas1ReportService.createDailyMetricsReport(properties, outputStream)
    }
  }

  @Deprecated("Use Cas1ReportsController")
  override fun reportsPlacementApplicationsGet(
    xServiceName: ServiceName,
    year: Int,
    month: Int,
  ): ResponseEntity<StreamingResponseBody> {
    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }

    validateParameters(null, month)

    val properties = PlacementApplicationReportProperties(year, month)

    return generateStreamingResponse(
      contentType = ContentType.XLSX,
      fileName = createCas1ReportName("placement-applications", year, month, ContentType.XLSX),
    ) { outputStream ->
      cas1ReportService.createPlacementApplicationReport(properties, outputStream)
    }
  }

  @Deprecated("Use Cas1ReportsController")
  override fun reportsPlacementMatchingOutcomesGet(
    xServiceName: ServiceName,
    year: Int,
    month: Int,
  ): ResponseEntity<StreamingResponseBody> {
    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }

    if (xServiceName !== ServiceName.approvedPremises) {
      throw NotAllowedProblem("This endpoint only supports CAS1")
    }

    val properties = Cas1PlacementMatchingOutcomesReportProperties(year, month)

    return generateStreamingResponse(
      contentType = ContentType.XLSX,
      fileName = createCas1ReportName("placement-matching-outcomes", year, month, ContentType.XLSX),
    ) { outputStream ->
      cas1ReportService.createPlacementMatchingOutcomesReport(properties, outputStream)
    }
  }

  private fun createCas1ReportName(name: String, year: Int, month: Int, contentType: ContentType) =
    "$name-$year-${month.toString().padStart(2, '0')}-${LocalDateTime.now().format(TIMESTAMP_FORMAT)}.${contentType.extension}"

  private fun validateParameters(probationRegionId: UUID?, month: Int) {
    when {
      probationRegionId == null && !userAccessService.currentUserHasAllRegionsAccess() -> throw ForbiddenProblem()
      probationRegionId != null && !userAccessService.currentUserCanAccessRegion(probationRegionId) -> throw ForbiddenProblem()
    }

    if (month < 1 || month > 12) {
      throw BadRequestProblem(errorDetail = "month must be between 1 and 12")
    }
  }
}
