package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3.ReportsCas3Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3ReportType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3ReportType.bedOccupancy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3ReportType.bedUsage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3ReportType.booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3ReportType.bookingGap
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3ReportType.futureBookings
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3ReportType.futureBookingsCsv
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3ReportType.referral
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.ContentType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generateStreamingResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generateXlsxStreamingResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUsageReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUtilisationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingGapReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.FutureBookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.TransitionalAccommodationReferralReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3ReportService
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

private const val MAXIMUM_REPORT_DURATION_IN_MONTHS = 3
private const val FUTURE_BOOKINGS_REPORT_EXTRA_MONTHS = 6

@Service("Cas3ReportsController")
class ReportsController(
  private val userAccessService: UserAccessService,
  private val cas3ReportService: Cas3ReportService,
) : ReportsCas3Delegate {

  override fun reportsReferralsGet(
    xServiceName: ServiceName,
    year: Int,
    month: Int,
    probationRegionId: UUID?,
  ): ResponseEntity<StreamingResponseBody> {
    if (!userAccessService.currentUserCanViewReport()) {
      throw ForbiddenProblem()
    }
    validateParameters(probationRegionId, month)

    val startDate = LocalDate.of(year, month, 1)
    val endDate = LocalDate.of(year, month, startDate.month.length(startDate.isLeapYear))
    val properties = TransitionalAccommodationReferralReportProperties(probationRegionId, startDate, endDate)

    return when (xServiceName) {
      ServiceName.temporaryAccommodation -> {
        generateXlsxStreamingResponse { outputStream ->
          cas3ReportService.createCas3ApplicationReferralsReport(properties, outputStream)
        }
      }
      else -> throw UnsupportedOperationException("Only supported for CAS3")
    }
  }

  override fun reportsReportNameGet(
    reportName: Cas3ReportType,
    startDate: LocalDate,
    endDate: LocalDate,
    probationRegionId: UUID?,
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
        cas3ReportService.createBedUtilisationReport(
          BedUtilisationReportProperties(
            ServiceName.temporaryAccommodation,
            startDate = startDate,
            endDate = endDate,
            probationRegionId = probationRegionId,
          ),
          outputStream,
        )
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

      bookingGap -> generateStreamingResponse(
        contentType = ContentType.CSV,
      ) { outputStream ->
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

  private fun validateParameters(probationRegionId: UUID?, month: Int) {
    validateUserAccessibility(probationRegionId)

    if (month < 1 || month > 12) {
      throw BadRequestProblem(errorDetail = "month must be between 1 and 12")
    }
  }

  private fun validateRequestParameters(probationRegionId: UUID?, startDate: LocalDate, endDate: LocalDate) {
    validateUserAccessibility(probationRegionId)
    validateRequestedDates(startDate, endDate)
  }

  @SuppressWarnings("ThrowsCount")
  private fun validateRequestedDates(startDate: LocalDate, endDate: LocalDate) {
    when {
      startDate.isAfter(endDate) || startDate.isEqual(endDate) -> throw BadRequestProblem(invalidParams = mapOf("$.startDate" to "afterEndDate"))
      endDate.isAfter(LocalDate.now()) -> throw BadRequestProblem(invalidParams = mapOf("$.endDate" to "inFuture"))
      ChronoUnit.MONTHS.between(startDate, endDate)
        .toInt() >= MAXIMUM_REPORT_DURATION_IN_MONTHS -> throw BadRequestProblem(invalidParams = mapOf("$.endDate" to "rangeTooLarge"))
    }
  }

  private fun validateUserAccessibility(probationRegionId: UUID?) {
    when {
      probationRegionId == null && !userAccessService.currentUserHasAllRegionsAccess() -> throw ForbiddenProblem()
      probationRegionId != null && !userAccessService.currentUserCanAccessRegion(probationRegionId) -> throw ForbiddenProblem()
    }
  }
}
