package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.reporting.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceOccupancyBedspaceReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceOccupancyBookingReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceOccupancyBookingTurnaroundReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceOccupancyVoidBedspaceReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BedUtilisationBedspaceReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BedUtilisationBookingCancellationReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BedUtilisationBookingReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BedUtilisationBookingTurnaroundReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BedUtilisationVoidBedspaceReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import java.time.Instant
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
    bedspaceStartDate = bed.createdAt?.toInstant(),
    bedspaceEndDate = bed.endDate,
    premisesId = premises.id.toString(),
    roomId = room.id.toString(),
  )
}

fun convertToCas3BedspaceOccupancyBedspaceReportData(bedspace: Cas3BedspacesEntity): BedspaceOccupancyBedspaceReportData {
  val probationDeliveryUnitName = bedspace.premises.probationDeliveryUnit.name
  val premises = bedspace.premises
  return Cas3BedspaceOccupancyReportData(
    bedspaceId = bedspace.id.toString(),
    probationRegionName = premises.probationDeliveryUnit.probationRegion.name,
    probationDeliveryUnitName = probationDeliveryUnitName,
    localAuthorityName = premises.localAuthorityArea?.name,
    premisesName = premises.name,
    addressLine1 = premises.addressLine1,
    town = premises.town,
    postCode = premises.postcode,
    roomName = bedspace.reference,
    bedspaceStartDate = bedspace.createdAt?.toInstant(),
    bedspaceEndDate = bedspace.endDate,
    premisesId = premises.id.toString(),
  )
}

fun convertToCas3BedspaceOccupancyBookingReportData(booking: Cas3BookingEntity) = Cas3BedspaceOccupancyBookingReportData(
  bookingId = booking.id.toString(),
  arrivalDate = booking.arrivalDate,
  departureDate = booking.departureDate,
  bedspaceId = booking.bedspace?.id.toString(),
  arrivalId = booking.arrival?.id?.toString(),
  arrivalCreatedAt = booking.arrival?.createdAt?.toInstant(),
  confirmationId = booking.confirmation?.id?.toString(),
)

fun convertToCas3BedUtilisationBookingReportData(booking: BookingEntity): Cas3BedUtilisationBookingReportData = Cas3BedUtilisationBookingReportData(
  bookingId = booking.id.toString(),
  arrivalDate = booking.arrivalDate,
  departureDate = booking.departureDate,
  bedId = booking.bed?.id.toString(),
  arrivalId = booking.arrival?.id?.toString(),
  arrivalCreatedAt = booking.arrival?.createdAt?.toInstant(),
  confirmationId = booking.confirmation?.id?.toString(),
)

fun convertToCas3BedUtilisationBookingCancellationReportData(booking: BookingEntity): Cas3BedUtilisationBookingCancellationReportData = Cas3BedUtilisationBookingCancellationReportData(
  cancellationId = booking.cancellation?.id.toString(),
  bedId = booking.bed?.id.toString(),
  bookingId = booking.id.toString(),
  createdAt = booking.cancellation?.createdAt?.toInstant()!!,
)

fun convertToCas3BedUtilisationBookingTurnaroundReportData(booking: BookingEntity): Cas3BedUtilisationBookingTurnaroundReportData = Cas3BedUtilisationBookingTurnaroundReportData(
  turnaroundId = booking.turnaround?.id.toString(),
  bedId = booking.bed?.id.toString(),
  bookingId = booking.id.toString(),
  workingDayCount = booking.turnaround?.workingDayCount!!,
  createdAt = booking.turnaround?.createdAt?.toInstant()!!,
)

fun convertToCas3BedspaceOccupancyBookingTurnaroundReportData(booking: Cas3BookingEntity) = Cas3BedspaceOccupancyBookingTurnaroundReportData(
  turnaroundId = booking.turnaround?.id.toString(),
  bedspaceId = booking.bedspace?.id.toString(),
  bookingId = booking.id.toString(),
  workingDayCount = booking.turnaround?.workingDayCount!!,
  createdAt = booking.turnaround?.createdAt?.toInstant()!!,
)

fun convertToCas3BedUtilisationLostBedReportData(voidBedspace: Cas3VoidBedspaceEntity): Cas3BedUtilisationVoidBedspaceUtilisationVoidBedspaceReportData = Cas3BedUtilisationVoidBedspaceUtilisationVoidBedspaceReportData(
  bedId = voidBedspace.bed!!.id.toString(),
  startDate = voidBedspace.startDate,
  endDate = voidBedspace.endDate,
  cancellationId = voidBedspace.cancellation?.id?.toString(),
)

fun convertToCas3BedspaceOccupancyVoidBedspaceReportData(voidBedspace: Cas3VoidBedspaceEntity) = Cas3BedspaceOccupancyVoidBedspaceReportData(
  bedspaceId = voidBedspace.bedspace!!.id.toString(),
  startDate = voidBedspace.startDate,
  endDate = voidBedspace.endDate,
  cancellationId = voidBedspace.cancellation?.id?.toString(),
)

fun convertToCas3BedspaceOccupancyVoidBedpaceReportData(voidBedspace: Cas3VoidBedspaceEntity) = Cas3BedspaceOccupancyVoidBedspaceReportData(
  bedspaceId = voidBedspace.bedspace!!.id.toString(),
  startDate = voidBedspace.startDate,
  endDate = voidBedspace.endDate,
  cancellationId = null,
)

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
  override val bedspaceStartDate: Instant?,
  override val bedspaceEndDate: LocalDate?,
  override val premisesId: String,
  override val roomId: String,
) : BedUtilisationBedspaceReportData

@Suppress("LongParameterList")
class Cas3BedUtilisationBookingReportData(
  override val bookingId: String,
  override val arrivalDate: LocalDate,
  override val departureDate: LocalDate,
  override val bedId: String,
  override val arrivalId: String?,
  override val arrivalCreatedAt: Instant?,
  override val confirmationId: String?,
) : BedUtilisationBookingReportData

class Cas3BedUtilisationBookingCancellationReportData(
  override val cancellationId: String,
  override val bedId: String,
  override val bookingId: String,
  override val createdAt: Instant,
) : BedUtilisationBookingCancellationReportData

class Cas3BedUtilisationBookingTurnaroundReportData(
  override val turnaroundId: String,
  override val bedId: String,
  override val bookingId: String,
  override val createdAt: Instant,
  override val workingDayCount: Int,
) : BedUtilisationBookingTurnaroundReportData

class Cas3BedUtilisationVoidBedspaceUtilisationVoidBedspaceReportData(
  override val bedId: String,
  override val startDate: LocalDate,
  override val endDate: LocalDate,
  override val cancellationId: String?,
) : BedUtilisationVoidBedspaceReportData

@Suppress("LongParameterList")
class Cas3BedspaceOccupancyReportData(
  override val bedspaceId: String,
  override val probationRegionName: String?,
  override val probationDeliveryUnitName: String?,
  override val localAuthorityName: String?,
  override val premisesName: String,
  override val addressLine1: String,
  override val town: String?,
  override val postCode: String,
  override val roomName: String,
  override val bedspaceStartDate: Instant?,
  override val bedspaceEndDate: LocalDate?,
  override val premisesId: String,
) : BedspaceOccupancyBedspaceReportData

@Suppress("LongParameterList")
class Cas3BedspaceOccupancyBookingReportData(
  override val bookingId: String,
  override val arrivalDate: LocalDate,
  override val departureDate: LocalDate,
  override val bedspaceId: String,
  override val arrivalId: String?,
  override val arrivalCreatedAt: Instant?,
  override val confirmationId: String?,
) : BedspaceOccupancyBookingReportData

class Cas3BedspaceOccupancyVoidBedspaceReportData(
  override val bedspaceId: String,
  override val startDate: LocalDate,
  override val endDate: LocalDate,
  override val cancellationId: String?,
) : BedspaceOccupancyVoidBedspaceReportData

class Cas3BedspaceOccupancyBookingTurnaroundReportData(
  override val turnaroundId: String,
  override val bedspaceId: String,
  override val bookingId: String,
  override val createdAt: Instant,
  override val workingDayCount: Int,
) : BedspaceOccupancyBookingTurnaroundReportData
