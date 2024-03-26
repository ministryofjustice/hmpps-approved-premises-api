package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BedUtilisationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUtilisationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.toShortBase58
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.earliestDateOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilInclusive
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.latestDateOf

class BedUtilisationReportGenerator(
  private val bookingRepository: BookingRepository,
  private val lostBedsRepository: LostBedsRepository,
  private val workingDayService: WorkingDayService,
) : ReportGenerator<BedEntity, BedUtilisationReportRow, BedUtilisationReportProperties>(BedUtilisationReportRow::class) {
  override fun filter(properties: BedUtilisationReportProperties): (BedEntity) -> Boolean = {
    checkServiceType(properties.serviceName, it.room.premises) &&
      (properties.probationRegionId == null || it.room.premises.probationRegion.id == properties.probationRegionId)
  }

  override val convert: BedEntity.(properties: BedUtilisationReportProperties) -> List<BedUtilisationReportRow> = { properties ->
    var bookedDaysActiveAndClosed = 0
    var confirmedDays = 0
    var provisionalDays = 0
    var scheduledTurnaroundDays = 0
    var effectiveTurnaroundDays = 0
    var voidDays = 0

    val nonCancelledBookings = bookingRepository.findAllByOverlappingDateForBed(properties.startDate, properties.endDate, this)
      .filter { it.cancellation == null }

    val nonCancelledVoids = lostBedsRepository.findAllByOverlappingDateForBed(properties.startDate, properties.endDate, this)
      .filter { it.cancellation == null }

    val premises = this.room.premises

    nonCancelledBookings
      .forEach { booking ->
        val daysOfBookingInMonth = latestDateOf(booking.arrivalDate, properties.startDate)
          .getDaysUntilInclusive(earliestDateOf(booking.departureDate, properties.endDate))
          .count()

        when {
          booking.arrival != null -> bookedDaysActiveAndClosed += daysOfBookingInMonth
          booking.confirmation != null && booking.arrival == null -> confirmedDays += daysOfBookingInMonth
          booking.confirmation == null -> provisionalDays += daysOfBookingInMonth
        }

        if (booking.turnaround != null) {
          val turnaroundStartDate = booking.departureDate.plusDays(1)
          val turnaroundEndDate = workingDayService.addWorkingDays(booking.departureDate, booking.turnaround!!.workingDayCount)
          val firstDayOfTurnaroundInMonth = latestDateOf(turnaroundStartDate, properties.startDate)
          val lastDayOfTurnaroundInMonth = earliestDateOf(turnaroundEndDate, properties.endDate)

          scheduledTurnaroundDays += workingDayService.getWorkingDaysCount(firstDayOfTurnaroundInMonth, lastDayOfTurnaroundInMonth)
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
      if (this.createdAt == null) properties.startDate else latestDateOf(this.createdAt!!.toLocalDate(), properties.startDate)

    val bedspaceOnlineDaysEndDate =
      if (this.endDate == null) properties.endDate else earliestDateOf(this.endDate!!, properties.endDate)

    val bedspaceOnlineDays = bedspaceOnlineDaysStartDate
      .getDaysUntilInclusive(bedspaceOnlineDaysEndDate)
      .count()

    val temporaryAccommodationPremisesEntity = premises as? TemporaryAccommodationPremisesEntity
    listOf(
      BedUtilisationReportRow(
        probationRegion = temporaryAccommodationPremisesEntity?.probationRegion?.name,
        pdu = temporaryAccommodationPremisesEntity?.probationDeliveryUnit?.name,
        localAuthority = temporaryAccommodationPremisesEntity?.localAuthorityArea?.name,
        propertyRef = premises.name,
        addressLine1 = premises.addressLine1,
        town = premises.town,
        postCode = premises.postcode,
        bedspaceRef = this.room.name,
        bookedDaysActiveAndClosed = bookedDaysActiveAndClosed,
        confirmedDays = confirmedDays,
        provisionalDays = provisionalDays,
        scheduledTurnaroundDays = scheduledTurnaroundDays,
        effectiveTurnaroundDays = effectiveTurnaroundDays,
        voidDays = voidDays,
        totalBookedDays = totalBookedDays,
        bedspaceStartDate = if (this.createdAt == null) null else this.createdAt!!.toLocalDate(),
        bedspaceEndDate = this.endDate,
        bedspaceOnlineDays = bedspaceOnlineDays,
        occupancyRate = totalBookedDays.toDouble() / bedspaceOnlineDays,
        uniquePropertyRef = premises.id.toShortBase58(),
        uniqueBedspaceRef = this.room.id.toShortBase58(),
      ),
    )
  }
}
