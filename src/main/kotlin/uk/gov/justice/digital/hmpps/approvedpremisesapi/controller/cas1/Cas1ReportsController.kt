package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.ReportsCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generateXlsxStreamingResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotAllowedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.ApplicationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.Cas1PlacementMatchingOutcomesReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.DailyMetricReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.LostBedReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.PlacementApplicationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ReportService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ReportService

private const val MONTH_MAX = 12
private const val MONTH_MIN = 1

@Service
class Cas1ReportsController(
  private val reportService: ReportService,
  private val cas1ReportService: Cas1ReportService,
  private val userService: UserService,
  private val userAccessService: UserAccessService,
) : ReportsCas1Delegate {

  override fun reportsReportNameGet(
    xServiceName: ServiceName,
    reportName: Cas1ReportName,
    year: Int,
    month: Int,
  ): ResponseEntity<StreamingResponseBody> {
    if (xServiceName !== ServiceName.approvedPremises) {
      throw NotAllowedProblem("This endpoint only supports CAS1")
    }

    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }
    validateMonth(month)

    return when (reportName) {
      Cas1ReportName.applications -> generateXlsxStreamingResponse {
          outputStream ->
        reportService.createCas1ApplicationPerformanceReport(
          ApplicationReportProperties(xServiceName, year, month, userService.getUserForRequest().deliusUsername),
          outputStream,
        )
      }
      Cas1ReportName.dailyMetrics -> generateXlsxStreamingResponse {
          outputStream ->
        reportService.createDailyMetricsReport(DailyMetricReportProperties(xServiceName, year, month), outputStream)
      }
      Cas1ReportName.lostBeds -> generateXlsxStreamingResponse {
          outputStream ->
        reportService.createLostBedReport(LostBedReportProperties(xServiceName, null, year, month), outputStream)
      }
      Cas1ReportName.placementApplications -> generateXlsxStreamingResponse {
          outputStream ->
        reportService.createCas1PlacementApplicationReport(PlacementApplicationReportProperties(year, month), outputStream)
      }
      Cas1ReportName.placementMatchingOutcomes -> generateXlsxStreamingResponse {
          outputStream ->
        cas1ReportService.createPlacementMatchingOutcomesReport(Cas1PlacementMatchingOutcomesReportProperties(year, month), outputStream)
      }
    }
  }

  private fun validateMonth(month: Int) {
    if (month < MONTH_MIN || month > MONTH_MAX) {
      throw BadRequestProblem(errorDetail = "month must be between 1 and 12")
    }
  }
}
