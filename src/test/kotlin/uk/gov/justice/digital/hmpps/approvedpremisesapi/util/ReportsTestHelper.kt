package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedUtilisationBedspaceReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedUtilisationBookingReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedUtilisationLostBedReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BookingsReportData
import java.sql.Timestamp
import java.time.LocalDate

fun List<BookingEntity>.toBookingsReportData(): List<BookingsReportData> = this
  .map {
    val application = it.application as? TemporaryAccommodationApplicationEntity
    val temporaryAccommodationPremises = it.premises as? TemporaryAccommodationPremisesEntity
    object : BookingsReportData {
      override val bookingId: String
        get() = it.id.toString()
      override val referralId: String?
        get() = application?.id?.toString()
      override val referralDate: LocalDate?
        get() = application?.submittedAt?.toLocalDate()
      override val riskOfSeriousHarm: String?
        get() = application?.riskRatings?.roshRisks?.value?.overallRisk
      override val registeredSexOffender: Boolean?
        get() = application?.isRegisteredSexOffender
      override val historyOfSexualOffence: Boolean?
        get() = application?.isHistoryOfSexualOffence
      override val concerningSexualBehaviour: Boolean?
        get() = application?.isConcerningSexualBehaviour
      override val needForAccessibleProperty: Boolean?
        get() = application?.needsAccessibleProperty
      override val historyOfArsonOffence: Boolean?
        get() = application?.hasHistoryOfArson
      override val concerningArsonBehaviour: Boolean?
        get() = application?.isConcerningArsonBehaviour
      override val dutyToReferMade: Boolean?
        get() = application?.isDutyToReferSubmitted
      override val dateDutyToReferMade: LocalDate?
        get() = application?.dutyToReferSubmissionDate
      override val referralEligibleForCas3: Boolean?
        get() = application?.isEligible
      override val referralEligibilityReason: String?
        get() = application?.eligibilityReason
      override val probationRegionName: String
        get() = it.premises.probationRegion.name
      override val localAuthorityAreaName: String?
        get() = it.premises.localAuthorityArea?.name
      override val crn: String
        get() = it.crn
      override val confirmationId: String?
        get() = it.confirmation?.id?.toString()
      override val cancellationId: String?
        get() = it.cancellation?.id?.toString()
      override val cancellationReason: String?
        get() = it.cancellation?.reason?.name
      override val startDate: LocalDate?
        get() = it.arrival?.arrivalDate
      override val endDate: LocalDate?
        get() = it.arrival?.expectedDepartureDate
      override val actualEndDate: Timestamp?
        get() = it.departure?.dateTime?.let { time -> Timestamp.from(time.toInstant()) }
      override val accommodationOutcome: String?
        get() = it.departure?.moveOnCategory?.name
      override val dutyToReferLocalAuthorityAreaName: String?
        get() = application?.dutyToReferLocalAuthorityAreaName
      override val pdu: String?
        get() = temporaryAccommodationPremises?.probationDeliveryUnit?.name
      override val town: String?
        get() = it.premises.town
      override val postCode: String?
        get() = it.premises.postcode
    }
  }
  .sortedBy { it.bookingId }

fun List<BookingEntity>.toBookingsReportDataAndPersonInfo(): List<BookingsReportDataAndPersonInfo> =
  this.toBookingsReportDataAndPersonInfo { PersonSummaryInfoResult.Unknown(it) }

fun List<BookingEntity>.toBookingsReportDataAndPersonInfo(configuration: (crn: String) -> PersonSummaryInfoResult): List<BookingsReportDataAndPersonInfo> =
  this.toBookingsReportData().map { BookingsReportDataAndPersonInfo(it, configuration(it.crn)) }

fun createTestBedUtilisationBedspaceReportData(bed: BedEntity): TestBedUtilisationBedspaceReportData {
  val room = bed.room
  var probationDeliveryUnitName: String? = null
  val premises = room.premises

  if (room.premises is TemporaryAccommodationPremisesEntity) {
    probationDeliveryUnitName = (room.premises as TemporaryAccommodationPremisesEntity).probationDeliveryUnit?.name
  }

  return TestBedUtilisationBedspaceReportData(
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

fun createTestBedUtilisationBookingReportData(booking: BookingEntity): TestBedUtilisationBookingReportData {
  return TestBedUtilisationBookingReportData(
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

fun createTestBedUtilisationLostBedReportData(lostBed: LostBedsEntity): TestBedUtilisationLostBedReportData {
  return TestBedUtilisationLostBedReportData(
    bedId = lostBed.bed.id.toString(),
    startDate = lostBed.startDate,
    endDate = lostBed.endDate,
    cancellationId = lostBed.cancellation?.id?.toString(),
  )
}

@Suppress("LongParameterList")
class TestBedUtilisationBedspaceReportData(
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
class TestBedUtilisationBookingReportData(
  override val arrivalDate: LocalDate,
  override val departureDate: LocalDate,
  override val bedId: String,
  override val cancellationId: String?,
  override val arrivalId: String?,
  override val confirmationId: String?,
  override val turnaroundId: String?,
  override val workingDayCount: Int?,
) : BedUtilisationBookingReportData

class TestBedUtilisationLostBedReportData(
  override val bedId: String,
  override val startDate: LocalDate,
  override val endDate: LocalDate,
  override val cancellationId: String?,
) : BedUtilisationLostBedReportData
