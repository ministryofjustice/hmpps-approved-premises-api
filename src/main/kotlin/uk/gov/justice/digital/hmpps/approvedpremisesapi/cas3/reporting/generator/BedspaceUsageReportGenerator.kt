package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3OverstayEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3v2BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.Cas3BedUsageReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.Cas3BedUsageType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BedUsageReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.util.toShortBase58
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.MAX_DAYS_STAY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilExclusiveEnd

class BedspaceUsageReportGenerator(
  private val bookingTransformer: Cas3BookingTransformer,
  private val bookingRepository: Cas3v2BookingRepository,
  private val cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository,
  private val workingDayService: WorkingDayService,
) : ReportGenerator<Cas3BedspacesEntity, Cas3BedUsageReportRow, BedUsageReportProperties>(Cas3BedUsageReportRow::class) {
  private val log = LoggerFactory.getLogger(this::class.java)
  override fun filter(properties: BedUsageReportProperties): (Cas3BedspacesEntity) -> Boolean = {
    properties.probationRegionId == null || it.premises.probationDeliveryUnit.probationRegion.id == properties.probationRegionId
  }

  override val convert: Cas3BedspacesEntity.(properties: BedUsageReportProperties) -> List<Cas3BedUsageReportRow> = { properties ->
    val bookings = bookingRepository.findAllByOverlappingDateForBedspace(properties.startDate, properties.endDate, this)
    val voids = cas3VoidBedspacesRepository.findAllByOverlappingDateForBedspace(properties.startDate, properties.endDate, this)

    val premises = this.premises
    val resultRows = mutableListOf<Cas3BedUsageReportRow>()

    bookings.forEach { booking ->
      var overstay: Cas3OverstayEntity? = null
      val durationOfBookingDays = booking.arrivalDate.getDaysUntilExclusiveEnd(booking.departureDate).size

      if (durationOfBookingDays > MAX_DAYS_STAY) {
        overstay = booking.overstays.sortedBy { it.createdAt }.lastOrNull()
        if (overstay == null) {
          log.warn("booking ${booking.id} is over ${MAX_DAYS_STAY} but has no overstay record")
        }
      }

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
        type = Cas3BedUsageType.Booking,
        startDate = booking.arrivalDate,
        endDate = booking.departureDate,
        durationOfBookingDays = durationOfBookingDays,
        bookingStatus = bookingTransformer.determineStatus(booking),
        overstay = overstay?.let { "Y" } ?: "N",
        authorised = overstay?.let { if (it.isAuthorised) "Y" else "N" },
        reason = overstay?.reason,
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
          type = Cas3BedUsageType.Turnaround,
          startDate = turnaroundStartDate,
          endDate = endDate,
          durationOfBookingDays = turnaroundStartDate.getDaysUntilExclusiveEnd(endDate).size,
          overstay = null,
          authorised = null,
          reason = null,
          bookingStatus = null,
          voidCategory = null,
          voidNotes = null,
          costCentre = null,
          uniquePropertyRef = premises.id.toShortBase58(),
          uniqueBedspaceRef = this.id.toShortBase58(),
        )
      }
    }

    voids.forEach { voidBedspace ->
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
        type = Cas3BedUsageType.Void,
        startDate = voidBedspace.startDate,
        endDate = voidBedspace.endDate,
        durationOfBookingDays = voidBedspace.startDate.getDaysUntilExclusiveEnd(voidBedspace.endDate).size,
        bookingStatus = null,
        overstay = null,
        authorised = null,
        reason = null,
        voidCategory = voidBedspace.reason.name,
        voidNotes = voidBedspace.notes,
        costCentre = voidBedspace.costCentre,
        uniquePropertyRef = premises.id.toShortBase58(),
        uniqueBedspaceRef = this.id.toShortBase58(),
      )
    }

    resultRows
  }
}
