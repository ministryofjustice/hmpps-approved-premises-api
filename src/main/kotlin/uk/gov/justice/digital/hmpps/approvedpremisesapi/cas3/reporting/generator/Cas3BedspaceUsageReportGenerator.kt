package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3v2BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedUsageType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.Cas3BedUsageReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BedUsageReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.util.toShortBase58
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilExclusiveEnd

class Cas3BedspaceUsageReportGenerator(
  private val bookingTransformer: Cas3BookingTransformer,
  private val bookingRepository: Cas3v2BookingRepository,
  private val cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository,
  private val workingDayService: WorkingDayService,
) : ReportGenerator<Cas3BedspacesEntity, Cas3BedUsageReportRow, BedUsageReportProperties>(Cas3BedUsageReportRow::class) {
  override fun filter(properties: BedUsageReportProperties): (Cas3BedspacesEntity) -> Boolean = {
      properties.probationRegionId == null || it.premises.probationDeliveryUnit.probationRegion.id == properties.probationRegionId
  }

  override val convert: Cas3BedspacesEntity.(properties: BedUsageReportProperties) -> List<Cas3BedUsageReportRow> = { properties ->
    val bookings = bookingRepository.findAllByOverlappingDateForBedspace(properties.startDate, properties.endDate, this)
    val voids = cas3VoidBedspacesRepository.findAllByOverlappingDateForBedspace(properties.startDate, properties.endDate, this)

    val premises = this.premises
    val resultRows = mutableListOf<Cas3BedUsageReportRow>()

    bookings.forEach { booking ->
      resultRows += Cas3BedUsageReportRow(
        probationRegion = premises.probationDeliveryUnit.probationRegion.name,
        pdu = premises.probationDeliveryUnit.name,
        localAuthority = premises.localAuthorityArea?.name,
        propertyRef = premises.name,
        addressLine1 = premises.addressLine1,
        town = premises.town,
        postCode = premises.postcode,
        bedspaceRef = this.reference,
        crn = booking.crn,
        type = BedUsageType.Booking,
        startDate = booking.arrivalDate,
        endDate = booking.departureDate,
        durationOfBookingDays = booking.arrivalDate.getDaysUntilExclusiveEnd(booking.departureDate).size,
        bookingStatus = bookingTransformer.determineStatus(booking),
        voidCategory = null,
        voidNotes = null,
        costCentre = null,
        uniquePropertyRef = premises.id.toShortBase58(),
        uniqueBedspaceRef = this.id.toShortBase58(),
      )

      val turnaround = booking.turnaround
      if (turnaround != null && turnaround.workingDayCount > 0) {
        val turnaroundStartDate = booking.departureDate.plusDays(1)
        val endDate = workingDayService.addWorkingDays(booking.departureDate, turnaround.workingDayCount)

        resultRows += Cas3BedUsageReportRow(
          probationRegion = premises.probationDeliveryUnit.probationRegion.name,
          pdu = premises.probationDeliveryUnit.name,
          localAuthority = premises.localAuthorityArea?.name,
          propertyRef = premises.name,
          addressLine1 = premises.addressLine1,
          town = premises.town,
          postCode = premises.postcode,
          bedspaceRef = this.reference,
          crn = null,
          type = BedUsageType.Turnaround,
          startDate = turnaroundStartDate,
          endDate = endDate,
          durationOfBookingDays = turnaroundStartDate.getDaysUntilExclusiveEnd(endDate).size,
          bookingStatus = null,
          voidCategory = null,
          voidNotes = null,
          costCentre = null,
          uniquePropertyRef = premises.id.toShortBase58(),
          uniqueBedspaceRef = this.id.toShortBase58(),
        )
      }
    }

    voids.forEach { lostBed ->
      resultRows += Cas3BedUsageReportRow(
        probationRegion = premises.probationDeliveryUnit.probationRegion.name,
        pdu = premises.probationDeliveryUnit.name,
        localAuthority = premises.localAuthorityArea?.name,
        propertyRef = premises.name,
        addressLine1 = premises.addressLine1,
        town = premises.town,
        postCode = premises.postcode,
        bedspaceRef = this.reference,
        crn = null,
        type = BedUsageType.Void,
        startDate = lostBed.startDate,
        endDate = lostBed.endDate,
        durationOfBookingDays = lostBed.startDate.getDaysUntilExclusiveEnd(lostBed.endDate).size,
        bookingStatus = null,
        voidCategory = lostBed.reason.name,
        voidNotes = lostBed.notes,
        costCentre = lostBed.costCentre,
        uniquePropertyRef = premises.id.toShortBase58(),
        uniqueBedspaceRef = this.id.toShortBase58(),
      )
    }

    resultRows
  }
}
