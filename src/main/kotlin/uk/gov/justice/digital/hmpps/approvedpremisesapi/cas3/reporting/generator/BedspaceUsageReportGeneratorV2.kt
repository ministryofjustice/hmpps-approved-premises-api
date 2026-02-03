package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3OverstayEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceUsageReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.Cas3BedUsageReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.Cas3BedUsageType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BedUsageReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.util.toShortBase58
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.MAX_DAYS_STAY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilExclusiveEnd

class BedspaceUsageReportGeneratorV2(
  private val bookingTransformer: Cas3BookingTransformer,
  private val workingDayService: WorkingDayService,
) : ReportGenerator<BedspaceUsageReportData, Cas3BedUsageReportRow, BedUsageReportProperties>(Cas3BedUsageReportRow::class) {
  private val log = LoggerFactory.getLogger(this::class.java)
  override fun filter(properties: BedUsageReportProperties): (BedspaceUsageReportData) -> Boolean = {
    properties.probationRegionId == null || it.bedspace.premises.probationDeliveryUnit.probationRegion.id == properties.probationRegionId
  }

  override val convert: BedspaceUsageReportData.(properties: BedUsageReportProperties) -> List<Cas3BedUsageReportRow> = { properties ->
    val bedspace = this.bedspace
    val bookings = this.bookings
    val voids = this.voids

    val premises = bedspace.premises
    val resultRows = mutableListOf<Cas3BedUsageReportRow>()

    bookings.forEach { booking ->
      var (overstay, durationOfBookingDays) = getStayData(booking)
      resultRows += Cas3BedUsageReportRow(
        probationRegion = premises.probationDeliveryUnit.probationRegion.name,
        pdu = premises.probationDeliveryUnit.name,
        localAuthority = premises.localAuthorityArea?.name,
        propertyRef = premises.name,
        addressLine1 = premises.addressLine1,
        town = premises.town,
        postCode = premises.postcode,
        bedspaceRef = bedspace.reference,
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
        uniqueBedspaceRef = bedspace.id.toShortBase58(),
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
          bedspaceRef = bedspace.reference,
          crn = null,
          type = Cas3BedUsageType.Turnaround,
          startDate = turnaroundStartDate,
          endDate = endDate,
          durationOfBookingDays = turnaroundStartDate.getDaysUntilExclusiveEnd(endDate).size,
          bookingStatus = null,
          overstay = null,
          authorised = null,
          reason = null,
          voidCategory = null,
          voidNotes = null,
          costCentre = null,
          uniquePropertyRef = premises.id.toShortBase58(),
          uniqueBedspaceRef = bedspace.id.toShortBase58(),
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
        bedspaceRef = bedspace.reference,
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
        uniqueBedspaceRef = bedspace.id.toShortBase58(),
      )
    }

    resultRows
  }

  private fun getStayData(booking: Cas3BookingEntity): Pair<Cas3OverstayEntity?, Int> {
    var overstay: Cas3OverstayEntity? = null
    val durationOfBookingDays = booking.arrivalDate.getDaysUntilExclusiveEnd(booking.departureDate).size

    if (durationOfBookingDays > MAX_DAYS_STAY) {
      overstay = booking.overstays.maxByOrNull { it.createdAt }
      if (overstay == null) {
        log.warn("booking ${booking.id} is over ${MAX_DAYS_STAY} but has no overstay record")
      }
    }
    return Pair(overstay, durationOfBookingDays)
  }
}
