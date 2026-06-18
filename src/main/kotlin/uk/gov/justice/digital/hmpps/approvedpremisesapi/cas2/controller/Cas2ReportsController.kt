package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2v2ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2ReportsService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.controller.generateXlsxStreamingResponse

@Cas2Controller
class Cas2ReportsController(private val cas2v2ReportService: Cas2ReportsService) {

  @GetMapping("/reports/{reportName}")
  fun reportsReportNameGet(
    @PathVariable reportName: Cas2v2ReportName,
  ): ResponseEntity<StreamingResponseBody> = when (reportName) {
    Cas2v2ReportName.submittedMinusApplications -> generateXlsxStreamingResponse { outputStream ->
      cas2v2ReportService.createSubmittedApplicationsReport(outputStream)
    }

    Cas2v2ReportName.applicationMinusStatusMinusUpdates -> generateXlsxStreamingResponse { outputStream ->
      cas2v2ReportService.createApplicationStatusUpdatesReport(outputStream)
    }

    Cas2v2ReportName.unsubmittedMinusApplications -> generateXlsxStreamingResponse { outputStream ->
      cas2v2ReportService.createUnsubmittedApplicationsReport(outputStream)
    }
  }
}
