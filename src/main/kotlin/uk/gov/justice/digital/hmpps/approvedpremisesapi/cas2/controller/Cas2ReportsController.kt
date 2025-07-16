package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2ReportsService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generateXlsxStreamingResponse

@RestController
@RequestMapping(
  "\${api.base-path:}/cas2",
  produces = ["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"],
)
class Cas2ReportsController(private val reportService: Cas2ReportsService) {

  @GetMapping("/reports/{reportName}")
  fun reportsReportNameGet(@PathVariable reportName: Cas2ReportName): ResponseEntity<StreamingResponseBody> = when (reportName) {
    Cas2ReportName.submittedMinusApplications -> generateXlsxStreamingResponse { outputStream ->
      reportService.createSubmittedApplicationsReport(outputStream)
    }

    Cas2ReportName.applicationMinusStatusMinusUpdates -> generateXlsxStreamingResponse { outputStream ->
      reportService.createApplicationStatusUpdatesReport(outputStream)
    }

    Cas2ReportName.unsubmittedMinusApplications -> generateXlsxStreamingResponse { outputStream ->
      reportService.createUnsubmittedApplicationsReport(outputStream)
    }
  }
}
