package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ManagementInfoSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository.Constants.MOVE_ON_CATEGORY_LOCAL_AUTHORITY_RENTED_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonRepository.Companion.NON_ARRIVAL_REASON_CUSTODIAL_DISPOSAL_RIC
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DeliusBookingImportEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime

@Service
class Cas1BookingManagementInfoService(
  private val departureReasonRepository: DepartureReasonRepository,
  private val moveOnCategoryRepository: MoveOnCategoryRepository,
  private val nonArrivalReasonRepository: NonArrivalReasonRepository,
) {

  fun fromBooking(booking: BookingEntity) = ManagementInfo(
    source = ManagementInfoSource.LEGACY_CAS_1,
    deliusId = null,
    arrivedAtDate = booking.arrival?.arrivalDateTime?.toLocalDate(),
    arrivedAtTime = booking.arrival?.arrivalDateTime?.toLocalDateTime()?.toLocalTime(),
    departedAtDate = booking.departure?.dateTime?.toLocalDate(),
    departedAtTime = booking.departure?.dateTime?.toLocalDateTime()?.toLocalTime(),
    keyWorkerStaffCode = null,
    keyWorkerName = null,
    departureReason = booking.departure?.reason,
    departureMoveOnCategory = booking.departure?.moveOnCategory,
    departureNotes = booking.departure?.notes,
    nonArrivalConfirmedAt = booking.nonArrival?.createdAt,
    nonArrivalReason = booking.nonArrival?.reason,
    nonArrivalNotes = booking.nonArrival?.notes,
  )

  fun fromDeliusBookingImport(import: Cas1DeliusBookingImportEntity) = ManagementInfo(
    source = ManagementInfoSource.DELIUS,
    deliusId = import.approvedPremisesReferralId,
    arrivedAtDate = import.arrivalDate,
    arrivedAtTime = null,
    departedAtDate = import.departureDate,
    departedAtTime = null,
    keyWorkerStaffCode = import.keyWorkerStaffCode,
    keyWorkerName = import.keyWorkerStaffCode?.let { "${import.keyWorkerForename} ${import.keyWorkerSurname}" },
    departureReason = import.departureReasonCode?.let { reasonCode ->
      departureReasonRepository
        .findAllByServiceScope(ServiceName.approvedPremises.value)
        .filter { it.legacyDeliusReasonCode == reasonCode }
        .maxByOrNull { it.isActive }
        ?: error("Could not resolve DepartureReason for code $reasonCode")
    },
    departureMoveOnCategory = import.moveOnCategoryCode?.let {
      getMoveOnCategory(it)
    },
    departureNotes = null,
    nonArrivalConfirmedAt = import.nonArrivalContactDatetime,
    nonArrivalReason = import.nonArrivalReasonCode?.let {
      getNonArrivalReason(it)
    },
    nonArrivalNotes = import.nonArrivalNotes,
  )

  fun getMoveOnCategory(code: String): MoveOnCategoryEntity = if (code == "MC02") {
    moveOnCategoryRepository.findByIdOrNull(MOVE_ON_CATEGORY_LOCAL_AUTHORITY_RENTED_ID)!!
  } else {
    moveOnCategoryRepository.findAllByServiceScope(ServiceName.approvedPremises.value)
      .firstOrNull { it.legacyDeliusCategoryCode == code } ?: error("Could not resolve MoveOnCategory for code $code")
  }

  fun getNonArrivalReason(code: String): NonArrivalReasonEntity = if (code == "1H") {
    nonArrivalReasonRepository.findByIdOrNull(NON_ARRIVAL_REASON_CUSTODIAL_DISPOSAL_RIC)!!
  } else {
    nonArrivalReasonRepository.findByLegacyDeliusReasonCode(code) ?: error("Could not resolve NonArrivalReason for code $code")
  }
}

data class ManagementInfo(
  val source: ManagementInfoSource,
  val deliusId: String?,
  val arrivedAtDate: LocalDate?,
  val arrivedAtTime: LocalTime?,
  val departedAtDate: LocalDate?,
  val departedAtTime: LocalTime?,
  val keyWorkerStaffCode: String?,
  val keyWorkerName: String?,
  val departureReason: DepartureReasonEntity?,
  val departureMoveOnCategory: MoveOnCategoryEntity?,
  val departureNotes: String?,
  val nonArrivalConfirmedAt: OffsetDateTime?,
  val nonArrivalReason: NonArrivalReasonEntity?,
  val nonArrivalNotes: String?,
)
