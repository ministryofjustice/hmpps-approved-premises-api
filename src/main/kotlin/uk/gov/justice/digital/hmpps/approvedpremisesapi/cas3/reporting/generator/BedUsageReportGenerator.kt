package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator

import org.jetbrains.kotlinx.dataframe.DataFrame
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedUsageReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedUsageReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedUsageType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BedUsageReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.util.toShortBase58
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilExclusiveEnd

class BedUsageReportGenerator(
  private val bookingTransformer: BookingTransformer,
  private val workingDayService: WorkingDayService,
) : ReportGenerator<BedUsageReportData, BedUsageReportRow, BedUsageReportProperties>(BedUsageReportRow::class) {

  override fun filter(properties: BedUsageReportProperties): (BedUsageReportData) -> Boolean = {
    checkServiceType(properties.serviceName, it.bed.room.premises) &&
      (properties.probationRegionId == null || it.bed.room.premises.probationRegion.id == properties.probationRegionId)
  }

  override val convert: BedUsageReportData.(properties: BedUsageReportProperties) -> List<BedUsageReportRow> = { properties ->
    val bed = this.bed
    val bookings = this.bookings
    val voids = this.voids

    val premises = bed.room.premises
    val temporaryAccommodationPremisesEntity = premises as? TemporaryAccommodationPremisesEntity
    val resultRows = mutableListOf<BedUsageReportRow>()

    bookings.forEach { booking ->
      resultRows += BedUsageReportRow(
        probationRegion = temporaryAccommodationPremisesEntity?.probationRegion?.name,
        pdu = temporaryAccommodationPremisesEntity?.probationDeliveryUnit?.name,
        localAuthority = temporaryAccommodationPremisesEntity?.localAuthorityArea?.name,
        propertyRef = premises.name,
        addressLine1 = premises.addressLine1,
        town = premises.town,
        postCode = premises.postcode,
        bedspaceRef = bed.room.name,
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
        uniqueBedspaceRef = bed.room.id.toShortBase58(),
      )

      val turnaround = booking.turnaround
      if (turnaround != null && turnaround.workingDayCount > 0) {
        val turnaroundStartDate = booking.departureDate.plusDays(1)
        val endDate = workingDayService.addWorkingDays(booking.departureDate, turnaround.workingDayCount)

        resultRows += BedUsageReportRow(
          probationRegion = temporaryAccommodationPremisesEntity?.probationRegion?.name,
          pdu = temporaryAccommodationPremisesEntity?.probationDeliveryUnit?.name,
          localAuthority = temporaryAccommodationPremisesEntity?.localAuthorityArea?.name,
          propertyRef = premises.name,
          addressLine1 = premises.addressLine1,
          town = premises.town,
          postCode = premises.postcode,
          bedspaceRef = bed.room.name,
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
          uniqueBedspaceRef = bed.room.id.toShortBase58(),
        )
      }
    }

    voids.forEach { voidBedspace ->
      resultRows += BedUsageReportRow(
        probationRegion = temporaryAccommodationPremisesEntity?.probationRegion?.name,
        pdu = temporaryAccommodationPremisesEntity?.probationDeliveryUnit?.name,
        localAuthority = temporaryAccommodationPremisesEntity?.localAuthorityArea?.name,
        propertyRef = premises.name,
        addressLine1 = premises.addressLine1,
        town = premises.town,
        postCode = premises.postcode,
        bedspaceRef = bed.room.name,
        crn = null,
        type = BedUsageType.Void,
        startDate = voidBedspace.startDate,
        endDate = voidBedspace.endDate,
        durationOfBookingDays = voidBedspace.startDate.getDaysUntilExclusiveEnd(voidBedspace.endDate).size,
        bookingStatus = null,
        voidCategory = voidBedspace.reason.name,
        voidNotes = voidBedspace.notes,
        costCentre = voidBedspace.costCentre,
        uniquePropertyRef = premises.id.toShortBase58(),
        uniqueBedspaceRef = bed.room.id.toShortBase58(),
      )
    }

    resultRows
  }
}
