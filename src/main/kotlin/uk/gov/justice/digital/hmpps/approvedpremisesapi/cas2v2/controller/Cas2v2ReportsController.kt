package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2v2.ReportsCas2v2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.Cas2v2ReportsService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generateXlsxStreamingResponse

@Service("Cas2v2ReportsController")
class Cas2v2ReportsController(private val cas2v2ReportService: Cas2v2ReportsService) : ReportsCas2v2Delegate {

  override fun reportsReportNameGet(reportName: Cas2v2ReportName): ResponseEntity<StreamingResponseBody> = when (reportName) {
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
