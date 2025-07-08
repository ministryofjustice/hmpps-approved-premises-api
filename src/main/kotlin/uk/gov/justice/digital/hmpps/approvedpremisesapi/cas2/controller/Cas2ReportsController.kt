package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2.ReportsCas2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2ReportsService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generateXlsxStreamingResponse

@Service("Cas2ReportsController")
class Cas2ReportsController(private val reportService: Cas2ReportsService) : ReportsCas2Delegate {

  override fun reportsReportNameGet(reportName: Cas2ReportName): ResponseEntity<StreamingResponseBody> = when (reportName) {
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
