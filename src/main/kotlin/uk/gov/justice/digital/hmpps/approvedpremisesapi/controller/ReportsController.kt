package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.ReportsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotAllowedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ApTypeCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.TierCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.ApplicationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUsageReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUtilisationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.Cas1PlacementMatchingOutcomesReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.DailyMetricReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.LostBedReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.PlacementApplicationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.PlacementMetricsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.ReferralsMetricsProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ReportService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.io.ByteArrayOutputStream
import java.util.UUID

@Service
class ReportsController(
  private val reportService: ReportService,
  private val userAccessService: UserAccessService,
  private val userService: UserService,
) : ReportsApiDelegate {

  override fun reportsBookingsGet(xServiceName: ServiceName, year: Int, month: Int, probationRegionId: UUID?): ResponseEntity<Resource> {
    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }

    validateParameters(probationRegionId, month)

    val properties = BookingsReportProperties(xServiceName, probationRegionId, year, month)
    val outputStream = ByteArrayOutputStream()

    reportService.createBookingsReport(properties, outputStream)

    return ResponseEntity.ok(InputStreamResource(outputStream.toByteArray().inputStream()))
  }

  override fun reportsBedUsageGet(xServiceName: ServiceName, year: Int, month: Int, probationRegionId: UUID?): ResponseEntity<Resource> {
    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }

    validateParameters(probationRegionId, month)

    val properties = BedUsageReportProperties(xServiceName, probationRegionId, year, month)
    val outputStream = ByteArrayOutputStream()

    reportService.createBedUsageReport(properties, outputStream)

    return ResponseEntity.ok(InputStreamResource(outputStream.toByteArray().inputStream()))
  }

  override fun reportsBedUtilisationGet(xServiceName: ServiceName, year: Int, month: Int, probationRegionId: UUID?): ResponseEntity<Resource> {
    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }

    validateParameters(probationRegionId, month)

    val properties = BedUtilisationReportProperties(xServiceName, probationRegionId, year, month)
    val outputStream = ByteArrayOutputStream()

    reportService.createBedUtilisationReport(properties, outputStream)

    return ResponseEntity.ok(InputStreamResource(outputStream.toByteArray().inputStream()))
  }

  override fun reportsLostBedsGet(xServiceName: ServiceName, year: Int, month: Int, probationRegionId: UUID?): ResponseEntity<Resource> {
    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }

    validateParameters(probationRegionId, month)

    val properties = LostBedReportProperties(xServiceName, probationRegionId, year, month)
    val outputStream = ByteArrayOutputStream()

    reportService.createLostBedReport(properties, outputStream)

    return ResponseEntity.ok(InputStreamResource(outputStream.toByteArray().inputStream()))
  }

  override fun reportsApplicationsGet(xServiceName: ServiceName, year: Int, month: Int): ResponseEntity<Resource> {
    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }

    val user = userService.getUserForRequest()

    validateParameters(null, month)

    val properties = ApplicationReportProperties(xServiceName, year, month, user.deliusUsername)
    val outputStream = ByteArrayOutputStream()

    reportService.createCas1ApplicationPerformanceReport(properties, outputStream)

    return ResponseEntity.ok(InputStreamResource(outputStream.toByteArray().inputStream()))
  }

  override fun reportsReferralsGet(xServiceName: ServiceName, year: Int, month: Int): ResponseEntity<Resource> {
    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }

    val user = userService.getUserForRequest()

    validateParameters(null, month)

    val properties = ApplicationReportProperties(xServiceName, year, month, user.deliusUsername)
    val outputStream = ByteArrayOutputStream()

    reportService.createCas1ApplicationReferralsReport(properties, outputStream)

    return ResponseEntity.ok(InputStreamResource(outputStream.toByteArray().inputStream()))
  }

  override fun reportsDailyMetricsGet(xServiceName: ServiceName, year: Int, month: Int): ResponseEntity<Resource> {
    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }

    if (xServiceName !== ServiceName.approvedPremises) {
      throw NotAllowedProblem("This endpoint only supports CAS1")
    }

    val properties = DailyMetricReportProperties(xServiceName, year, month)
    val outputStream = ByteArrayOutputStream()

    reportService.createDailyMetricsReport(properties, outputStream)

    return ResponseEntity.ok(InputStreamResource(outputStream.toByteArray().inputStream()))
  }

  override fun reportsReferralsByTierGet(
    xServiceName: ServiceName,
    year: Int,
    month: Int,
  ) = getReferralReport(xServiceName, year, month, TierCategory.entries)

  override fun reportsReferralsByApTypeGet(
    xServiceName: ServiceName,
    year: Int,
    month: Int,
  ) = getReferralReport(xServiceName, year, month, ApTypeCategory.entries)

  override fun reportsPlacementMetricsGet(xServiceName: ServiceName, year: Int, month: Int): ResponseEntity<Resource> {
    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }

    if (xServiceName !== ServiceName.approvedPremises) {
      throw NotAllowedProblem("This endpoint only supports CAS1")
    }

    val properties = PlacementMetricsReportProperties(year, month)
    val outputStream = ByteArrayOutputStream()

    reportService.createPlacementMetricsReport(properties, outputStream)

    return ResponseEntity.ok(InputStreamResource(outputStream.toByteArray().inputStream()))
  }

  override fun reportsPlacementApplicationsGet(
    xServiceName: ServiceName,
    year: Int,
    month: Int,
  ): ResponseEntity<Resource> {
    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }

    val user = userService.getUserForRequest()

    validateParameters(null, month)

    val properties = PlacementApplicationReportProperties(year, month)
    val outputStream = ByteArrayOutputStream()

    reportService.createCas1PlacementApplicationReport(properties, outputStream)

    return ResponseEntity.ok(InputStreamResource(outputStream.toByteArray().inputStream()))
  }

  private fun <T : Any> getReferralReport(xServiceName: ServiceName, year: Int, month: Int, categories: List<T>): ResponseEntity<Resource> {
    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }

    if (xServiceName !== ServiceName.approvedPremises) {
      throw NotAllowedProblem("This endpoint only supports CAS1")
    }

    val properties = ReferralsMetricsProperties(year, month)
    val outputStream = ByteArrayOutputStream()

    reportService.createReferralsMetricsReport(properties, outputStream, categories)

    return ResponseEntity.ok(InputStreamResource(outputStream.toByteArray().inputStream()))
  }

  override fun reportsPlacementMatchingOutcomesGet(
    xServiceName: ServiceName,
    year: Int,
    month: Int,
  ): ResponseEntity<Resource> {
    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }

    if (xServiceName !== ServiceName.approvedPremises) {
      throw NotAllowedProblem("This endpoint only supports CAS1")
    }

    val properties = Cas1PlacementMatchingOutcomesReportProperties(year, month)
    val outputStream = ByteArrayOutputStream()

    reportService.createCas1PlacementMatchingOutcomesReport(properties, outputStream)

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
