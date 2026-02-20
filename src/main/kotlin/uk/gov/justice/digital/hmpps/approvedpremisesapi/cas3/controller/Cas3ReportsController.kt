package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3ReportType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3ReportType.bedOccupancy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3ReportType.bedUsage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3ReportType.booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3ReportType.bookingGap
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3ReportType.futureBookings
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3ReportType.futureBookingsCsv
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3ReportType.referral
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BedUsageReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BedUtilisationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BedspaceOccupancyReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BookingGapReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.FutureBookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.TransitionalAccommodationReferralReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3ReportService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.ContentType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generateStreamingResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generateXlsxStreamingResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ParamDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

private const val MAXIMUM_REPORT_DURATION_IN_MONTHS = 6
private const val FUTURE_BOOKINGS_REPORT_EXTRA_MONTHS = 6

@Cas3Controller
class Cas3ReportsController(
  private val userAccessService: UserAccessService,
  private val cas3ReportService: Cas3ReportService,
  private val featureFlagService: FeatureFlagService,
) {

  @GetMapping(
    "/reports/{reportName}",
    produces = ["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"],
  )
  fun reportsReportNameGet(
    @PathVariable reportName: Cas3ReportType,
    @RequestParam startDate: LocalDate,
    @RequestParam endDate: LocalDate,
    @RequestParam probationRegionId: UUID?,
  ): ResponseEntity<StreamingResponseBody> {
    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }

    validateRequestParameters(probationRegionId, startDate, endDate)

    return when (reportName) {
      referral ->
        generateXlsxStreamingResponse { outputStream ->
          cas3ReportService.createCas3ApplicationReferralsReport(
            TransitionalAccommodationReferralReportProperties(
              startDate = startDate,
              endDate = endDate,
              probationRegionId = probationRegionId,
            ),
            outputStream,
          )
        }

      booking ->
        generateXlsxStreamingResponse { outputStream ->
          cas3ReportService.createBookingsReport(
            BookingsReportProperties(
              ServiceName.temporaryAccommodation,
              startDate = startDate,
              endDate = endDate,
              probationRegionId = probationRegionId,
            ),
            outputStream,
          )
        }

      bedUsage ->
        generateXlsxStreamingResponse { outputStream ->
          cas3ReportService.createBedUsageReport(
            BedUsageReportProperties(
              ServiceName.temporaryAccommodation,
              startDate = startDate,
              endDate = endDate,
              probationRegionId = probationRegionId,
            ),
            outputStream,
          )
        }

      bedOccupancy -> generateXlsxStreamingResponse { outputStream ->
        when (featureFlagService.getBooleanFlag("cas3-reports-with-new-bedspace-model-tables-enabled")) {
          true -> cas3ReportService.createBedspaceOccupancyReport(
            BedspaceOccupancyReportProperties(
              startDate = startDate,
              endDate = endDate,
              probationRegionId = probationRegionId,
            ),
            outputStream,
          )
          false -> cas3ReportService.createBedUtilisationReport(
            BedUtilisationReportProperties(
              ServiceName.temporaryAccommodation,
              startDate = startDate,
              endDate = endDate,
              probationRegionId = probationRegionId,
            ),
            outputStream,
          )
        }
      }

      futureBookings -> generateXlsxStreamingResponse { outputStream ->
        cas3ReportService.createFutureBookingReport(
          FutureBookingsReportProperties(
            startDate = startDate,
            endDate = endDate.plusMonths(FUTURE_BOOKINGS_REPORT_EXTRA_MONTHS.toLong()),
            probationRegionId = probationRegionId,
          ),
          outputStream,
        )
      }

      futureBookingsCsv -> generateStreamingResponse(
        contentType = ContentType.CSV,
      ) { outputStream ->
        cas3ReportService.createFutureBookingCsvReport(
          FutureBookingsReportProperties(
            startDate = startDate,
            endDate = endDate.plusMonths(FUTURE_BOOKINGS_REPORT_EXTRA_MONTHS.toLong()),
            probationRegionId = probationRegionId,
          ),
          outputStream,
        )
      }

      bookingGap -> generateXlsxStreamingResponse { outputStream ->
        cas3ReportService.createBookingGapReport(
          BookingGapReportProperties(
            startDate = startDate,
            endDate = endDate,
          ),
          outputStream,
        )
      }
    }
  }

  private fun validateRequestParameters(probationRegionId: UUID?, startDate: LocalDate, endDate: LocalDate) {
    validateUserAccessibility(probationRegionId)
    validateRequestedDates(startDate, endDate)
  }

  @SuppressWarnings("ThrowsCount")
  private fun validateRequestedDates(startDate: LocalDate, endDate: LocalDate) {
    when {
      startDate.isAfter(endDate) || startDate.isEqual(endDate) -> throw BadRequestProblem(invalidParams = mapOf("$.startDate" to ParamDetails("afterEndDate")))
      endDate.isAfter(LocalDate.now()) -> throw BadRequestProblem(invalidParams = mapOf("$.endDate" to ParamDetails("inFuture")))
      ChronoUnit.MONTHS.between(startDate, endDate)
        .toInt() >= MAXIMUM_REPORT_DURATION_IN_MONTHS -> throw BadRequestProblem(invalidParams = mapOf("$.endDate" to ParamDetails("rangeTooLarge")))
    }
  }

  private fun validateUserAccessibility(probationRegionId: UUID?) {
    when {
      probationRegionId == null && !userAccessService.currentUserHasAllRegionsAccess(ServiceName.temporaryAccommodation) -> throw ForbiddenProblem()
      probationRegionId != null && !userAccessService.currentUserCanAccessRegion(ServiceName.temporaryAccommodation, probationRegionId) -> throw ForbiddenProblem()
    }
  }
}
