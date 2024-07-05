package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.reporting.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedUtilisationBedspaceReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedUtilisationBookingReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedUtilisationLostBedReportData
import java.time.LocalDate

fun convertToCas3BedUtilisationBedspaceReportData(bed: BedEntity): Cas3BedUtilisationBedspaceReportData {
  val room = bed.room
  var probationDeliveryUnitName: String? = null
  val premises = room.premises

  if (room.premises is TemporaryAccommodationPremisesEntity) {
    probationDeliveryUnitName = (room.premises as TemporaryAccommodationPremisesEntity).probationDeliveryUnit?.name
  }

  return Cas3BedUtilisationBedspaceReportData(
    bedId = bed.id.toString(),
    probationRegionName = premises.probationRegion.name,
    probationDeliveryUnitName = probationDeliveryUnitName,
    localAuthorityName = premises.localAuthorityArea?.name,
    premisesName = premises.name,
    addressLine1 = premises.addressLine1,
    town = premises.town,
    postCode = premises.postcode,
    roomName = room.name,
    bedspaceStartDate = bed.createdAt?.toLocalDate(),
    bedspaceEndDate = bed.endDate,
    premisesId = premises.id.toString(),
    roomId = room.id.toString(),
  )
}

fun convertToCas3BedUtilisationBookingReportData(booking: BookingEntity): Cas3BedUtilisationBookingReportData {
  return Cas3BedUtilisationBookingReportData(
    arrivalDate = booking.arrivalDate,
    departureDate = booking.departureDate,
    bedId = booking.bed?.id.toString(),
    cancellationId = booking.cancellation?.id?.toString(),
    arrivalId = booking.arrival?.id?.toString(),
    confirmationId = booking.confirmation?.id?.toString(),
    turnaroundId = booking.turnaround?.id?.toString(),
    workingDayCount = booking.turnaround?.workingDayCount,
  )
}

fun convertToCas3BedUtilisationLostBedReportData(lostBed: LostBedsEntity): Cas3BedUtilisationLostBedReportData {
  return Cas3BedUtilisationLostBedReportData(
    bedId = lostBed.bed.id.toString(),
    startDate = lostBed.startDate,
    endDate = lostBed.endDate,
    cancellationId = lostBed.cancellation?.id?.toString(),
  )
}

@Suppress("LongParameterList")
class Cas3BedUtilisationBedspaceReportData(
  override val bedId: String,
  override val probationRegionName: String?,
  override val probationDeliveryUnitName: String?,
  override val localAuthorityName: String?,
  override val premisesName: String,
  override val addressLine1: String,
  override val town: String?,
  override val postCode: String,
  override val roomName: String,
  override val bedspaceStartDate: LocalDate?,
  override val bedspaceEndDate: LocalDate?,
  override val premisesId: String,
  override val roomId: String,
) : BedUtilisationBedspaceReportData

@Suppress("LongParameterList")
class Cas3BedUtilisationBookingReportData(
  override val arrivalDate: LocalDate,
  override val departureDate: LocalDate,
  override val bedId: String,
  override val cancellationId: String?,
  override val arrivalId: String?,
  override val confirmationId: String?,
  override val turnaroundId: String?,
  override val workingDayCount: Int?,
) : BedUtilisationBookingReportData

class Cas3BedUtilisationLostBedReportData(
  override val bedId: String,
  override val startDate: LocalDate,
  override val endDate: LocalDate,
  override val cancellationId: String?,
) : BedUtilisationLostBedReportData
