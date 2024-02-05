package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3

import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3.ReportsCas3Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.TransitionalAccommodationReferralReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.ReportService
import java.io.ByteArrayOutputStream
import java.util.UUID

@Service("Cas3ReportsController")
class ReportsController(
  private val userAccessService: UserAccessService,
  private val reportService: ReportService,
) : ReportsCas3Delegate {

  override fun reportsReferralsGet(
    xServiceName: ServiceName,
    year: Int,
    month: Int,
    probationRegionId: UUID?,
  ): ResponseEntity<Resource> {
    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }
    validateParameters(probationRegionId, month)

    val properties = TransitionalAccommodationReferralReportProperties(xServiceName, probationRegionId, year, month)
    val outputStream = ByteArrayOutputStream()

    when (xServiceName) {
      ServiceName.temporaryAccommodation -> {
        reportService.createCas3ApplicationReferralsReport(properties, outputStream)
      }
      else -> throw UnsupportedOperationException("Only supported for CAS3")
    }

    return ResponseEntity.ok(InputStreamResource(outputStream.toByteArray().inputStream()))
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
