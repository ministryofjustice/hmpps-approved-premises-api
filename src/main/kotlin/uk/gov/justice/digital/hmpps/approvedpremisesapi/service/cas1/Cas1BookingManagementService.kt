package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssignKeyWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NonArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableCas1SpaceBookingEntityRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository.Constants.MOVE_ON_CATEGORY_NOT_APPLICABLE_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SpringEventPublisher
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.StaffMemberService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService.DepartureInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.ArrivalRecorded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas1BookingManagementService(
  private val cas1PremisesService: Cas1PremisesService,
  private val cas1SpaceBookingRepository: Cas1SpaceBookingRepository,
  private val cas1SpaceBookingManagementDomainEventService: Cas1SpaceBookingManagementDomainEventService,
  private val departureReasonRepository: DepartureReasonRepository,
  private val moveOnCategoryRepository: MoveOnCategoryRepository,
  private val staffMemberService: StaffMemberService,
  private val nonArrivalReasonRepository: NonArrivalReasonRepository,
  private val lockableCas1SpaceBookingEntityRepository: LockableCas1SpaceBookingEntityRepository,
  private val userService: UserService,
  private val cas1ChangeRequestService: Cas1ChangeRequestService,
  private val springEventPublisher: SpringEventPublisher,
) {

  @Transactional
  fun recordArrivalForBooking(
    premisesId: UUID,
    bookingId: UUID,
    arrivalDate: LocalDate,
    arrivalTime: LocalTime,
  ): CasResult<Cas1SpaceBookingEntity> = validatedCasResult {
    if (cas1PremisesService.findPremiseById(premisesId) == null) {
      "$.premisesId" hasValidationError "doesNotExist"
    }

    lockableCas1SpaceBookingEntityRepository.acquirePessimisticLock(bookingId)
    val existingCas1SpaceBooking = cas1SpaceBookingRepository.findByIdOrNull(bookingId)
    if (existingCas1SpaceBooking == null) {
      "$.bookingId" hasValidationError "doesNotExist"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    existingCas1SpaceBooking!!

    if (existingCas1SpaceBooking.isCancelled()) {
      return existingCas1SpaceBooking.id hasConflictError "The booking has already been cancelled"
    }

    if (existingCas1SpaceBooking.hasArrival()) {
      return if (existingCas1SpaceBooking.hasSameActualArrivalDateTime(arrivalDate, arrivalTime)) {
        success(existingCas1SpaceBooking)
      } else {
        existingCas1SpaceBooking.id hasConflictError "An arrival is already recorded for this Space Booking"
      }
    }

    existingCas1SpaceBooking.canonicalArrivalDate = arrivalDate
    existingCas1SpaceBooking.actualArrivalDate = arrivalDate
    existingCas1SpaceBooking.actualArrivalTime = arrivalTime

    val updatedSpaceBooking = cas1SpaceBookingRepository.save(existingCas1SpaceBooking)

    cas1SpaceBookingManagementDomainEventService.arrivalRecorded(
      Cas1SpaceBookingManagementDomainEventService.ArrivalInfo(
        updatedSpaceBooking,
        actualArrivalDate = arrivalDate,
        actualArrivalTime = arrivalTime,
        recordedBy = userService.getUserForRequest(),
      ),
    )

    springEventPublisher.publishEvent(ArrivalRecorded(updatedSpaceBooking))

    success(updatedSpaceBooking)
  }

  @Transactional
  fun recordNonArrivalForBooking(
    premisesId: UUID,
    bookingId: UUID,
    cas1NonArrival: Cas1NonArrival,
    recordedBy: UserEntity,
  ): CasResult<Cas1SpaceBookingEntity> = validatedCasResult {
    if (cas1PremisesService.findPremiseById(premisesId) == null) {
      "$.premisesId" hasValidationError "doesNotExist"
    }

    lockableCas1SpaceBookingEntityRepository.acquirePessimisticLock(bookingId)
    val existingCas1SpaceBooking = cas1SpaceBookingRepository.findByIdOrNull(bookingId)
    if (existingCas1SpaceBooking == null) {
      "$.bookingId" hasValidationError "doesNotExist"
    }

    val reason = nonArrivalReasonRepository.findByIdOrNull(cas1NonArrival.reason)
    if (reason == null) {
      "$.reason" hasValidationError "doesNotExist"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    existingCas1SpaceBooking!!

    if (existingCas1SpaceBooking.isCancelled()) {
      return existingCas1SpaceBooking.id hasConflictError "The booking has already been cancelled"
    }

    if (existingCas1SpaceBooking.nonArrivalConfirmedAt != null) {
      return if (existingCas1SpaceBooking.nonArrivalReason != reason || existingCas1SpaceBooking.nonArrivalNotes != cas1NonArrival.notes) {
        existingCas1SpaceBooking.id hasConflictError "A non-arrival is already recorded for this Space Booking"
      } else {
        success(existingCas1SpaceBooking)
      }
    }

    val cas1NonArrivalNotes = cas1NonArrival.notes
    existingCas1SpaceBooking.nonArrivalConfirmedAt = OffsetDateTime.now().toInstant()
    existingCas1SpaceBooking.nonArrivalReason = reason
    existingCas1SpaceBooking.nonArrivalNotes = cas1NonArrivalNotes

    val result = cas1SpaceBookingRepository.save(existingCas1SpaceBooking)

    cas1ChangeRequestService.spaceBookingMarkedAsNonArrival(existingCas1SpaceBooking)

    cas1SpaceBookingManagementDomainEventService.nonArrivalRecorded(
      recordedBy,
      existingCas1SpaceBooking,
      reason!!,
      cas1NonArrivalNotes,
    )

    success(result)
  }

  @Transactional
  fun recordKeyWorkerAssignedForBooking(
    premisesId: UUID,
    bookingId: UUID,
    keyWorker: Cas1AssignKeyWorker,
  ): CasResult<Cas1SpaceBookingEntity> = validatedCasResult {
    val premises = cas1PremisesService.findPremiseById(premisesId)
    if (premises == null) {
      "$.premisesId" hasValidationError "doesNotExist"
    }

    val existingCas1SpaceBooking = cas1SpaceBookingRepository.findByIdOrNull(bookingId)
    if (existingCas1SpaceBooking == null) {
      "$.bookingId" hasValidationError "doesNotExist"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    existingCas1SpaceBooking!!

    if (existingCas1SpaceBooking.isCancelled()) {
      return existingCas1SpaceBooking.id hasConflictError "The booking has already been cancelled"
    }

    val staffMemberResponse = staffMemberService.getStaffMemberByCodeForPremise(keyWorker.staffCode, premises!!.qCode)
    if (staffMemberResponse !is CasResult.Success) {
      return "$.keyWorker.staffCode" hasSingleValidationError "notFound"
    }
    val assignedKeyWorker = extractEntityFromCasResult(staffMemberResponse)
    val assignedKeyWorkerAsStaffMember = StaffMember(
      staffCode = assignedKeyWorker.code,
      forenames = assignedKeyWorker.name.forenames(),
      surname = assignedKeyWorker.name.surname,
    )
    val assignedKeyWorkerName = "${assignedKeyWorker.name.forenames()} ${assignedKeyWorker.name.surname}"

    val previousKeyWorkerName = existingCas1SpaceBooking.keyWorkerName

    existingCas1SpaceBooking.keyWorkerStaffCode = assignedKeyWorker.code
    existingCas1SpaceBooking.keyWorkerName = assignedKeyWorkerName
    existingCas1SpaceBooking.keyWorkerAssignedAt = OffsetDateTime.now().toInstant()

    val result = cas1SpaceBookingRepository.save(existingCas1SpaceBooking)

    cas1SpaceBookingManagementDomainEventService.keyWorkerAssigned(
      existingCas1SpaceBooking,
      assignedKeyWorkerAsStaffMember,
      assignedKeyWorkerName,
      previousKeyWorkerName,
    )

    success(result)
  }

  @Transactional
  fun recordDepartureForBooking(
    premisesId: UUID,
    bookingId: UUID,
    departureInfo: DepartureInfo,
  ): CasResult<Cas1SpaceBookingEntity> = validatedCasResult {
    if (cas1PremisesService.findPremiseById(premisesId) == null) {
      "$.premisesId" hasValidationError "doesNotExist"
    }

    lockableCas1SpaceBookingEntityRepository.acquirePessimisticLock(bookingId)
    val existingSpaceBooking = cas1SpaceBookingRepository.findByIdOrNull(bookingId)
    if (existingSpaceBooking == null) {
      "$.bookingId" hasValidationError "doesNotExist"
    }

    val departureReason = departureReasonRepository.findByIdOrNull(departureInfo.reasonId)
    if (departureReason == null || !departureReason.isCas1()) {
      "$.cas1NewDeparture.reasonId" hasValidationError "doesNotExist"
    }

    val moveOnCategory = moveOnCategoryRepository.findByIdOrNull(departureInfo.moveOnCategoryId ?: MOVE_ON_CATEGORY_NOT_APPLICABLE_ID)
    if (moveOnCategory == null || !moveOnCategory.isCas1()) {
      "$.cas1NewDeparture.moveOnCategoryId" hasValidationError "doesNotExist"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    existingSpaceBooking!!

    if (existingSpaceBooking.isCancelled()) {
      return existingSpaceBooking.id hasConflictError "The booking has already been cancelled"
    }

    if (!existingSpaceBooking.hasArrival()) {
      return existingSpaceBooking.id hasConflictError "An arrival is not recorded for this Space Booking."
    }

    if (
      existingSpaceBooking.dateTimeIsBeforeArrival(
        departureInfo.departureDate,
        departureInfo.departureTime,
      )
    ) {
      return existingSpaceBooking.id hasConflictError "The departure date time is before the arrival date time."
    }

    if (existingSpaceBooking.hasDeparted()) {
      return if (hasExactDepartureDataAlreadyBeenRecorded(existingSpaceBooking, departureInfo)) {
        success(existingSpaceBooking)
      } else {
        existingSpaceBooking.id hasConflictError "A departure is already recorded for this Space Booking."
      }
    }

    existingSpaceBooking.actualDepartureDate = departureInfo.departureDate
    existingSpaceBooking.actualDepartureTime = departureInfo.departureTime
    existingSpaceBooking.canonicalDepartureDate = departureInfo.departureDate
    existingSpaceBooking.departureReason = departureReason
    existingSpaceBooking.departureMoveOnCategory = moveOnCategory
    existingSpaceBooking.departureNotes = departureInfo.notes

    val result = cas1SpaceBookingRepository.save(existingSpaceBooking)

    cas1ChangeRequestService.spaceBookingMarkedAsDeparted(existingSpaceBooking)

    cas1SpaceBookingManagementDomainEventService.departureRecorded(
      Cas1SpaceBookingManagementDomainEventService.DepartureInfo(
        existingSpaceBooking,
        departureReason!!,
        moveOnCategory!!,
        departureInfo.departureDate,
        departureInfo.departureTime,
        recordedBy = userService.getUserForRequest(),
      ),
    )

    success(result)
  }

  private fun Cas1SpaceBookingEntity.dateTimeIsBeforeArrival(
    date: LocalDate,
    time: LocalTime,
  ): Boolean {
    val arrivalDate = actualArrivalDate
    val arrivalTime = actualArrivalTime
    return date.isBefore(arrivalDate) ||
      (
        arrivalDate == date &&
          arrivalTime != null &&
          arrivalTime.isAfter(time)
        )
  }

  private fun hasExactDepartureDataAlreadyBeenRecorded(
    existingCas1SpaceBooking: Cas1SpaceBookingEntity,
    departureInfo: DepartureInfo,
  ): Boolean = departureInfo.departureDate == existingCas1SpaceBooking.actualDepartureDate &&
    departureInfo.departureTime == existingCas1SpaceBooking.actualDepartureTime &&
    departureInfo.reasonId == existingCas1SpaceBooking.departureReason?.id &&
    departureInfo.moveOnCategoryId == existingCas1SpaceBooking.departureMoveOnCategory?.id

  private fun Cas1SpaceBookingEntity.hasSameActualArrivalDateTime(
    date: LocalDate,
    time: LocalTime,
  ) = actualArrivalDate == date && actualArrivalTime == time
}
