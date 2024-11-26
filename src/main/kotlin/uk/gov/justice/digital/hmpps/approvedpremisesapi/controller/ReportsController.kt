package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.ReportsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUsageReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUtilisationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3ReportService
import java.time.LocalDate
import java.util.UUID

@Service
class ReportsController(
  private val cas3ReportService: Cas3ReportService,
  private val userAccessService: UserAccessService,
) : ReportsApiDelegate {

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
