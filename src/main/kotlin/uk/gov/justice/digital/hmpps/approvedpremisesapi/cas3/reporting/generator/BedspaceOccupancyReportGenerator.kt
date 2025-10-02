package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceOccupancyReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceOccupancyReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BedspaceOccupancyReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.util.toShortBase58
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.earliestDateOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilInclusive
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.latestDateOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate
import java.util.UUID

class BedspaceOccupancyReportGenerator(
  private val workingDayService: WorkingDayService,
) : ReportGenerator<BedspaceOccupancyReportData, BedspaceOccupancyReportRow, BedspaceOccupancyReportProperties>(
  BedspaceOccupancyReportRow::class,
) {
  override fun filter(properties: BedspaceOccupancyReportProperties): (BedspaceOccupancyReportData) -> Boolean = {
    true
  }

  override val convert: BedspaceOccupancyReportData.(properties: BedspaceOccupancyReportProperties) -> List<BedspaceOccupancyReportRow> =
    { properties ->
      var bookedDaysActiveAndClosed = 0
      var confirmedDays = 0
      var provisionalDays = 0
      var scheduledTurnaroundDays = 0
      var effectiveTurnaroundDays = 0
      var voidDays = 0

      val bedspace = this.bedspaceReportData
      val nonCancelledBookings =
        this.bookingsReportData
          .filterNot { bookingCancellationReportData.map { it.bookingId }.contains(it.bookingId) }
          .groupBy { it.bookingId }
          .mapValues { it.value.sortedByDescending { it.arrivalCreatedAt }.take(1) }
          .map { it.value.first() }
      val nonCancelledVoids = this.voidBedspaceReportData.filter { it.cancellationId == null }

      nonCancelledBookings
        .forEach { booking ->
          val daysOfBookingInMonth = latestDateOf(booking.arrivalDate, properties.startDate)
            .getDaysUntilInclusive(earliestDateOf(booking.departureDate, properties.endDate))
            .count()

          when {
            booking.arrivalId != null -> bookedDaysActiveAndClosed += daysOfBookingInMonth
            booking.confirmationId != null && booking.arrivalId == null -> confirmedDays += daysOfBookingInMonth
            booking.confirmationId == null -> provisionalDays += daysOfBookingInMonth
          }

          val bookingTurnaround =
            this.bookingTurnaroundReportData.filter { it.bookingId == booking.bookingId }.maxByOrNull { it.createdAt }
          if (bookingTurnaround != null) {
            val turnaroundStartDate = booking.departureDate.plusDays(1)
            val turnaroundEndDate =
              workingDayService.addWorkingDays(booking.departureDate, bookingTurnaround.workingDayCount)
            val firstDayOfTurnaroundInMonth = latestDateOf(turnaroundStartDate, properties.startDate)
            val lastDayOfTurnaroundInMonth = earliestDateOf(turnaroundEndDate, properties.endDate)

            scheduledTurnaroundDays += workingDayService.getWorkingDaysCount(
              firstDayOfTurnaroundInMonth,
              lastDayOfTurnaroundInMonth,
            )
            effectiveTurnaroundDays += firstDayOfTurnaroundInMonth
              .getDaysUntilInclusive(lastDayOfTurnaroundInMonth)
              .count()
          }
        }

      nonCancelledVoids.forEach { void ->
        val daysOfVoidInMonth = latestDateOf(void.startDate, properties.startDate)
          .getDaysUntilInclusive(earliestDateOf(void.endDate, properties.endDate))
          .count()

        voidDays += daysOfVoidInMonth
      }

      val totalBookedDays = bookedDaysActiveAndClosed
      val bedspaceOnlineDaysStartDate =
        if (bedspace.bedspaceStartDate == null) {
          properties.startDate
        } else {
          latestDateOf(
            bedspace.bedspaceStartDate!!.toLocalDate(),
            properties.startDate,
          )
        }

      val bedspaceOnlineDaysEndDate =
        if (bedspace.bedspaceEndDate == null) {
          properties.endDate
        } else {
          earliestDateOf(
            bedspace.bedspaceEndDate!!,
            properties.endDate,
          )
        }

      val bedspaceOnlineDays = bedspaceOnlineDaysStartDate
        .getDaysUntilInclusive(bedspaceOnlineDaysEndDate)
        .count()

      listOf(
        BedspaceOccupancyReportRow(
          probationRegion = bedspace.probationRegionName,
          pdu = bedspace.probationDeliveryUnitName,
          localAuthority = bedspace.localAuthorityName,
          propertyRef = bedspace.premisesName,
          addressLine1 = bedspace.addressLine1,
          town = bedspace.town,
          postCode = bedspace.postCode,
          bedspaceRef = bedspace.roomName,
          bookedDaysActiveAndClosed = bookedDaysActiveAndClosed,
          confirmedDays = confirmedDays,
          provisionalDays = provisionalDays,
          scheduledTurnaroundDays = scheduledTurnaroundDays,
          effectiveTurnaroundDays = effectiveTurnaroundDays,
          voidDays = voidDays,
          totalBookedDays = totalBookedDays,
          bedspaceStartDate = if (bedspace.bedspaceStartDate == null) null else bedspace.bedspaceStartDate!!.toLocalDate(),
          bedspaceEndDate = bedspace.bedspaceEndDate,
          bedspaceOnlineDays = bedspaceOnlineDays,
          occupancyRate = totalBookedDays.toDouble() / bedspaceOnlineDays,
          uniquePropertyRef = UUID.fromString(bedspace.premisesId).toShortBase58(),
          uniqueBedspaceRef = UUID.fromString(bedspace.bedspaceId).toShortBase58(),
        ),
      )
    }
}
