package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2.ReportsCas2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generateXlsxStreamingResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.ReportsService

@Service("Cas2ReportsController")
class ReportsController(private val reportService: ReportsService) : ReportsCas2Delegate {

  override fun reportsReportNameGet(reportName: Cas2ReportName): ResponseEntity<StreamingResponseBody> {
    return when (reportName) {
      Cas2ReportName.SUBMITTED_MINUS_APPLICATIONS -> generateXlsxStreamingResponse {
          outputStream ->
        reportService.createSubmittedApplicationsReport(outputStream)
      }
      Cas2ReportName.APPLICATION_MINUS_STATUS_MINUS_UPDATES -> generateXlsxStreamingResponse {
          outputStream ->
        reportService.createApplicationStatusUpdatesReport(outputStream)
      }
      Cas2ReportName.UNSUBMITTED_MINUS_APPLICATIONS -> generateXlsxStreamingResponse {
          outputStream ->
        reportService.createUnsubmittedApplicationsReport(outputStream)
      }
    }
  }
}
