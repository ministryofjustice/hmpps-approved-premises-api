package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3CostCentre
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedUsageReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedUsageType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BedUsageReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.util.toShortBase58
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BedUsageReportDataDTO
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilExclusiveEnd
import java.util.UUID

class Cas3BedUsageReportGenerator(
  private val bookingTransformer: BookingTransformer,
  private val workingDayService: WorkingDayService,
) : ReportGenerator<BedUsageReportDataDTO, BedUsageReportRow, BedUsageReportProperties>(BedUsageReportRow::class) {
  override fun filter(properties: BedUsageReportProperties): (BedUsageReportDataDTO) -> Boolean = {
    (properties.probationRegionId == null || UUID.fromString(it.probationRegionId) == properties.probationRegionId)
  }

  override val convert: BedUsageReportDataDTO.(properties: BedUsageReportProperties) -> List<BedUsageReportRow> = {
    val bedUsageReportRow = when (BedUsageType.from(this.type)) {
      BedUsageType.Booking -> BedUsageReportRow(
        probationRegion = this.probationRegionName,
        pdu = this.pdu,
        localAuthority = this.localAuthorityArea,
        propertyRef = this.propertyRef,
        addressLine1 = this.addressLine1,
        town = this.town,
        postCode = this.postCode,
        bedspaceRef = this.bedspaceRef,
        crn = this.crn,
        type = BedUsageType.Booking,
        startDate = this.startDate,
        endDate = this.endDate,
        durationOfBookingDays = this.startDate.getDaysUntilExclusiveEnd(this.endDate).size,
        bookingStatus = bookingTransformer.determineTemporaryAccommodationStatus(
          hasTurnaround = this.turnaroundId != null,
          turnaroundWorkingDayCount = this.turnaroundWorkingDayCount,
          departureDate = this.endDate,
          hasCancellation = this.cancellationCount > 0,
          hasDeparture = this.departureCount > 0,
          hasArrival = this.arrivalCount > 0,
          hasNonArrival = this.nonArrivalCount > 0,
          hasConfirmation = this.confirmationCount > 0,
        ),
        voidCategory = null,
        voidNotes = null,
        costCentre = null,
        uniquePropertyRef = UUID.fromString(this.uniquePropertyRef).toShortBase58(),
        uniqueBedspaceRef = UUID.fromString(this.uniqueBedspaceRef).toShortBase58(),
      )
      BedUsageType.Turnaround -> {
        val turnaroundStartDate = this.bookingDepartureDate.plusDays(1)
        val endDate = workingDayService.addWorkingDays(this.bookingDepartureDate, this.turnaroundWorkingDayCount!!)
        BedUsageReportRow(
          probationRegion = this.probationRegionName,
          pdu = this.pdu,
          localAuthority = this.localAuthorityArea,
          propertyRef = this.propertyRef,
          addressLine1 = this.addressLine1,
          town = this.town,
          postCode = this.postCode,
          bedspaceRef = this.bedspaceRef,
          crn = this.crn,
          type = BedUsageType.Turnaround,
          startDate = turnaroundStartDate,
          endDate = endDate,
          durationOfBookingDays = turnaroundStartDate.getDaysUntilExclusiveEnd(endDate).size,
          bookingStatus = null,
          voidCategory = null,
          voidNotes = null,
          costCentre = null,
          uniquePropertyRef = UUID.fromString(this.uniquePropertyRef).toShortBase58(),
          uniqueBedspaceRef = UUID.fromString(this.uniqueBedspaceRef).toShortBase58(),
        )
      }
      BedUsageType.Void -> BedUsageReportRow(
        probationRegion = this.probationRegionName,
        pdu = this.pdu,
        localAuthority = this.localAuthorityArea,
        propertyRef = this.propertyRef,
        addressLine1 = this.addressLine1,
        town = this.town,
        postCode = this.postCode,
        bedspaceRef = this.bedspaceRef,
        crn = this.crn,
        type = BedUsageType.Void,
        startDate = this.startDate,
        endDate = this.endDate,
        durationOfBookingDays = this.startDate.getDaysUntilExclusiveEnd(this.endDate).size,
        bookingStatus = null,
        voidCategory = this.voidCategory,
        voidNotes = this.voidNotes,
        costCentre = if (this.costCentre != null) Cas3CostCentre.from(this.costCentre) else null,
        uniquePropertyRef = UUID.fromString(this.uniquePropertyRef).toShortBase58(),
        uniqueBedspaceRef = UUID.fromString(this.uniqueBedspaceRef).toShortBase58(),
      )
    }
    listOf(bedUsageReportRow)
  }
}
