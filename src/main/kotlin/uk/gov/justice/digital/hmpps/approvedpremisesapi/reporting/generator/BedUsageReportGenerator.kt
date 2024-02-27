package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName.temporaryAccommodation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BedUsageReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BedUsageType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUsageReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.toShortBase58
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayCountService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilExclusiveEnd
import java.time.LocalDate

class BedUsageReportGenerator(
  private val bookingTransformer: BookingTransformer,
  private val bookingRepository: BookingRepository,
  private val lostBedsRepository: LostBedsRepository,
  private val workingDayCountService: WorkingDayCountService,
  private val cas3EndDateOverride: Int,
) : ReportGenerator<BedEntity, BedUsageReportRow, BedUsageReportProperties>(BedUsageReportRow::class) {
  override fun filter(properties: BedUsageReportProperties): (BedEntity) -> Boolean = {
    checkServiceType(properties.serviceName, it.room.premises) &&
      (properties.probationRegionId == null || it.room.premises.probationRegion.id == properties.probationRegionId)
  }

  override val convert: BedEntity.(properties: BedUsageReportProperties) -> List<BedUsageReportRow> = { properties ->
    val startOfMonth = LocalDate.of(properties.year, properties.month, 1)

    val endOfMonth = if (properties.serviceName == temporaryAccommodation && cas3EndDateOverride != 0) {
      startOfMonth.plusMonths(cas3EndDateOverride.toLong())
    } else {
      LocalDate.of(properties.year, properties.month, startOfMonth.month.length(startOfMonth.isLeapYear))
    }

    val bookings = bookingRepository.findAllByOverlappingDateForBed(startOfMonth, endOfMonth, this)
    val voids = lostBedsRepository.findAllByOverlappingDateForBed(startOfMonth, endOfMonth, this)

    val premises = this.room.premises

    val resultRows = mutableListOf<BedUsageReportRow>()

    bookings.forEach { booking ->
      resultRows += BedUsageReportRow(
        pdu = if (premises is TemporaryAccommodationPremisesEntity) premises.probationDeliveryUnit?.name else null,
        propertyRef = premises.name,
        addressLine1 = premises.addressLine1,
        bedspaceRef = this.room.name,
        crn = booking.crn,
        type = BedUsageType.Booking,
        startDate = booking.arrivalDate,
        endDate = booking.departureDate,
        durationOfBookingDays = booking.arrivalDate.getDaysUntilExclusiveEnd(booking.departureDate).size,
        bookingStatus = bookingTransformer.determineStatus(booking),
        voidCategory = null,
        voidNotes = null,
        uniquePropertyRef = premises.id.toShortBase58(),
        uniqueBedspaceRef = this.room.id.toShortBase58(),
      )

      val turnaround = booking.turnaround
      if (turnaround != null && turnaround.workingDayCount > 0) {
        val turnaroundStartDate = booking.departureDate.plusDays(1)
        val endDate = workingDayCountService.addWorkingDays(booking.departureDate, turnaround.workingDayCount)

        resultRows += BedUsageReportRow(
          pdu = (premises as? TemporaryAccommodationPremisesEntity)?.probationDeliveryUnit?.name,
          propertyRef = premises.name,
          addressLine1 = premises.addressLine1,
          bedspaceRef = this.room.name,
          crn = null,
          type = BedUsageType.Turnaround,
          startDate = turnaroundStartDate,
          endDate = endDate,
          durationOfBookingDays = turnaroundStartDate.getDaysUntilExclusiveEnd(endDate).size,
          bookingStatus = null,
          voidCategory = null,
          voidNotes = null,
          uniquePropertyRef = premises.id.toShortBase58(),
          uniqueBedspaceRef = this.room.id.toShortBase58(),
        )
      }
    }

    voids.forEach { lostBed ->
      resultRows += BedUsageReportRow(
        pdu = if (premises is TemporaryAccommodationPremisesEntity) premises.probationDeliveryUnit?.name else null,
        propertyRef = premises.name,
        addressLine1 = premises.addressLine1,
        bedspaceRef = this.room.name,
        crn = null,
        type = BedUsageType.Void,
        startDate = lostBed.startDate,
        endDate = lostBed.endDate,
        durationOfBookingDays = lostBed.startDate.getDaysUntilExclusiveEnd(lostBed.endDate).size,
        bookingStatus = null,
        voidCategory = lostBed.reason.name,
        voidNotes = lostBed.notes,
        uniquePropertyRef = premises.id.toShortBase58(),
        uniqueBedspaceRef = this.room.id.toShortBase58(),
      )
    }

    resultRows
  }
}
