package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssignKeyWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewEmergencyTransfer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewPlannedTransfer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NonArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingResidency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummarySortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository.Constants.CAS1_RELATED_APP_WITHDRAWN_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository.Constants.CAS1_RELATED_PLACEMENT_APP_WITHDRAWN_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository.Constants.CAS1_RELATED_PLACEMENT_REQ_WITHDRAWN_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableCas1SpaceBookingEntityRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockablePlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository.Constants.MOVE_ON_CATEGORY_NOT_APPLICABLE_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TransferType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.CasResultValidatedScope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult.ConflictError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult.GeneralValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult.Success
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.StaffMemberService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.collections.orEmpty
import kotlin.collections.toSet

@SuppressWarnings("TooManyFunctions")
@Service
class Cas1SpaceBookingService(
  private val cas1PremisesService: Cas1PremisesService,
  private val placementRequestService: PlacementRequestService,
  private val cas1SpaceBookingRepository: Cas1SpaceBookingRepository,
  private val cas1BookingDomainEventService: Cas1BookingDomainEventService,
  private val cas1BookingEmailService: Cas1BookingEmailService,
  private val cas1SpaceBookingManagementDomainEventService: Cas1SpaceBookingManagementDomainEventService,
  private val departureReasonRepository: DepartureReasonRepository,
  private val moveOnCategoryRepository: MoveOnCategoryRepository,
  private val cas1ApplicationStatusService: Cas1ApplicationStatusService,
  private val staffMemberService: StaffMemberService,
  private val cancellationReasonRepository: CancellationReasonRepository,
  private val nonArrivalReasonRepository: NonArrivalReasonRepository,
  private val lockablePlacementRequestRepository: LockablePlacementRequestRepository,
  private val lockableCas1SpaceBookingEntityRepository: LockableCas1SpaceBookingEntityRepository,
  private val userService: UserService,
  private val cas1ChangeRequestService: Cas1ChangeRequestService,
  private val characteristicService: CharacteristicService,
  private val clock: Clock,
) {
  @Transactional
  fun createNewBooking(
    premisesId: UUID,
    placementRequestId: UUID,
    arrivalDate: LocalDate,
    departureDate: LocalDate,
    createdBy: UserEntity,
    characteristics: List<CharacteristicEntity>,
  ): CasResult<Cas1SpaceBookingEntity> = validatedCasResult {
    val premises = cas1PremisesService.findPremiseById(premisesId)
    if (premises == null) {
      "$.premisesId" hasValidationError "doesNotExist"
    } else if (!premises.supportsSpaceBookings) {
      "$.premisesId" hasValidationError "doesNotSupportSpaceBookings"
    }

    lockablePlacementRequestRepository.acquirePessimisticLock(placementRequestId)
    val placementRequest = placementRequestService.getPlacementRequestOrNull(placementRequestId)
    if (placementRequest == null) {
      "$.placementRequestId" hasValidationError "doesNotExist"
    }

    if (arrivalDate >= departureDate) {
      "$.departureDate" hasValidationError "shouldBeAfterArrivalDate"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    premises!!
    placementRequest!!

    placementRequest.booking?.let {
      if (it.isActive()) {
        return it.id hasConflictError "A legacy Booking already exists for this premises and placement request"
      }
    }

    if (cas1SpaceBookingRepository.findByPlacementRequestId(placementRequestId).any { it.isActive() }) {
      return placementRequestId hasConflictError "A Space Booking already exists for this placement request"
    }

    val application = placementRequest.application

    val spaceBooking = createSpaceBookingEntity(
      premises = premises,
      placementRequest = placementRequest,
      expectedArrivalDate = arrivalDate,
      expectedDepartureDate = departureDate,
      createdBy = createdBy,
      characteristics = characteristics,
      transferType = null,
    )

    cas1ApplicationStatusService.spaceBookingMade(spaceBooking)

    cas1BookingDomainEventService.spaceBookingMade(
      application = application,
      booking = spaceBooking,
      user = createdBy,
      placementRequest = placementRequest,
    )

    cas1BookingEmailService.spaceBookingMade(spaceBooking, application)

    success(spaceBooking)
  }

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

    val result = cas1SpaceBookingRepository.save(existingCas1SpaceBooking)

    cas1ChangeRequestService.spaceBookingHasArrival(existingCas1SpaceBooking)

    cas1SpaceBookingManagementDomainEventService.arrivalRecorded(
      Cas1SpaceBookingManagementDomainEventService.ArrivalInfo(
        existingCas1SpaceBooking,
        actualArrivalDate = arrivalDate,
        actualArrivalTime = arrivalTime,
        recordedBy = userService.getUserForRequest(),
      ),
    )

    success(result)
  }

  private fun Cas1SpaceBookingEntity.hasSameActualArrivalDateTime(
    date: LocalDate,
    time: LocalTime,
  ) = actualArrivalDate == date && actualArrivalTime == time

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

  data class DepartureInfo(
    val departureDate: LocalDate,
    val departureTime: LocalTime,
    val reasonId: UUID,
    val moveOnCategoryId: UUID? = null,
    val notes: String? = null,
  )

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

  fun search(
    premisesId: UUID,
    filterCriteria: SpaceBookingFilterCriteria,
    pageCriteria: PageCriteria<Cas1SpaceBookingSummarySortField>,
  ): CasResult<SearchResultContainer> {
    val premises = cas1PremisesService.findPremiseById(premisesId)
      ?: return CasResult.NotFound("premises", premisesId.toString())

    val page = cas1SpaceBookingRepository.search(
      filterCriteria.residency?.name,
      filterCriteria.crnOrName,
      filterCriteria.keyWorkerStaffCode,
      premisesId,
      pageCriteria.toPageableOrAllPages(
        sortBy = when (pageCriteria.sortBy) {
          Cas1SpaceBookingSummarySortField.personName -> "personName"
          Cas1SpaceBookingSummarySortField.canonicalArrivalDate -> "canonicalArrivalDate"
          Cas1SpaceBookingSummarySortField.canonicalDepartureDate -> "canonicalDepartureDate"
          Cas1SpaceBookingSummarySortField.keyWorkerName -> "keyWorkerName"
          Cas1SpaceBookingSummarySortField.tier -> "tier"
        },
      ),
    )

    return Success(
      SearchResultContainer(
        results = page.toList(),
        paginationMetadata = getMetadata(page, pageCriteria),
        premises = premises,
      ),
    )
  }

  data class SearchResultContainer(
    val results: List<Cas1SpaceBookingSearchResult>,
    val paginationMetadata: PaginationMetadata?,
    val premises: ApprovedPremisesEntity,
  )

  fun getBookingForPremisesAndId(premisesId: UUID, bookingId: UUID): CasResult<Cas1SpaceBookingEntity> {
    if (!cas1PremisesService.premiseExistsById(premisesId)) return CasResult.NotFound("premises", premisesId.toString())

    return getBooking(bookingId)
  }

  fun getBooking(bookingId: UUID): CasResult<Cas1SpaceBookingEntity> {
    val booking = cas1SpaceBookingRepository.findByIdOrNull(bookingId) ?: return CasResult.NotFound("booking", bookingId.toString())

    return Success(booking)
  }

  fun getBookingsForPremisesAndCrn(premisesId: UUID, crn: String) = cas1SpaceBookingRepository.findByPremisesIdAndCrn(
    premisesId = premisesId,
    crn = crn,
  )

  fun getWithdrawableState(spaceBooking: Cas1SpaceBookingEntity, user: UserEntity): WithdrawableState = WithdrawableState(
    withdrawable = !spaceBooking.isCancelled() && !spaceBooking.hasArrival() && !spaceBooking.hasNonArrival(),
    withdrawn = spaceBooking.isCancelled(),
    userMayDirectlyWithdraw = user.hasAtLeastOnePermission(
      UserPermission.CAS1_SPACE_BOOKING_WITHDRAW,
      UserPermission.CAS1_PLACEMENT_APPEAL_ASSESS,
    ),
    blockingReason = if (spaceBooking.hasArrival()) {
      BlockingReason.ArrivalRecordedInCas1
    } else if (spaceBooking.hasNonArrival()) {
      BlockingReason.NonArrivalRecordedInCas1
    } else {
      null
    },
  )

  fun withdraw(
    spaceBooking: Cas1SpaceBookingEntity,
    occurredAt: LocalDate,
    userProvidedReasonId: UUID?,
    userProvidedReasonNotes: String?,
    withdrawalContext: WithdrawalContext,
  ): CasResult<Unit> {
    if (spaceBooking.isCancelled()) {
      return Success(Unit)
    }

    val resolvedReasonId = toCas1CancellationReason(withdrawalContext, userProvidedReasonId)

    val reason = cancellationReasonRepository.findByIdOrNull(resolvedReasonId)
      ?: return CasResult.FieldValidationError(mapOf("$.reason" to "doesNotExist"))

    if (reason.name == "Other" && userProvidedReasonNotes.isNullOrEmpty()) {
      return CasResult.FieldValidationError(mapOf("$.otherReason" to "empty"))
    }

    spaceBooking.cancellationReason = reason
    spaceBooking.cancellationOccurredAt = occurredAt
    spaceBooking.cancellationRecordedAt = Instant.now()
    spaceBooking.cancellationReasonNotes = userProvidedReasonNotes
    cas1SpaceBookingRepository.save(spaceBooking)

    val user = when (withdrawalContext.withdrawalTriggeredBy) {
      is WithdrawalTriggeredByUser -> withdrawalContext.withdrawalTriggeredBy.user
      else -> throw InternalServerErrorProblem("Withdrawal triggered automatically is not supported")
    }

    cas1ChangeRequestService.spaceBookingWithdrawn(spaceBooking)
    cas1BookingDomainEventService.spaceBookingCancelled(spaceBooking, user, reason)
    cas1ApplicationStatusService.spaceBookingCancelled(
      spaceBooking,
      isUserRequestedWithdrawal = withdrawalContext.triggeringEntityType == WithdrawableEntityType.SpaceBooking,
    )

    spaceBooking.application?.let {
      cas1BookingEmailService.spaceBookingWithdrawn(
        spaceBooking = spaceBooking,
        application = it,
        withdrawalTriggeredBy = withdrawalContext.withdrawalTriggeredBy,
      )
    }

    return Success(Unit)
  }

  data class SpaceBookingFilterCriteria(
    val residency: Cas1SpaceBookingResidency?,
    val crnOrName: String?,
    val keyWorkerStaffCode: String?,
  )

  private fun toCas1CancellationReason(
    withdrawalContext: WithdrawalContext,
    userProvidedCancellationReasonId: UUID?,
  ) = when (withdrawalContext.triggeringEntityType) {
    WithdrawableEntityType.Application -> CAS1_RELATED_APP_WITHDRAWN_ID
    WithdrawableEntityType.PlacementApplication -> CAS1_RELATED_PLACEMENT_APP_WITHDRAWN_ID
    WithdrawableEntityType.PlacementRequest -> CAS1_RELATED_PLACEMENT_REQ_WITHDRAWN_ID
    WithdrawableEntityType.Booking -> throw InternalServerErrorProblem("Withdrawing a SpaceBooking should not cascade to Booking")
    WithdrawableEntityType.SpaceBooking -> userProvidedCancellationReasonId!!
  }

  @Transactional
  fun updateSpaceBooking(
    updateSpaceBookingDetails: UpdateSpaceBookingDetails,
  ): CasResult<Cas1SpaceBookingEntity> = validatedCasResult {
    validateUpdateSpaceBooking(updateSpaceBookingDetails)

    if (validationErrors.any()) return fieldValidationError

    val bookingToUpdate = cas1SpaceBookingRepository.findByIdOrNull(updateSpaceBookingDetails.bookingId)!!

    val previousArrivalDate = bookingToUpdate.expectedArrivalDate
    val previousDepartureDate = bookingToUpdate.expectedDepartureDate
    val previousCharacteristics = bookingToUpdate.criteria.toList()

    val updatedBooking = updateExistingSpaceBooking(bookingToUpdate, updateSpaceBookingDetails)

    val previousArrivalDateIfChanged = if (previousArrivalDate != updatedBooking.expectedArrivalDate) previousArrivalDate else null
    val previousDepartureDateIfChanged = if (previousDepartureDate != updatedBooking.expectedDepartureDate) previousDepartureDate else null
    val previousCharacteristicsIfChanged = if (previousCharacteristics.sortedBy { it.id } != updatedBooking.criteria.sortedBy { it.id }) previousCharacteristics else null

    cas1BookingDomainEventService.spaceBookingChanged(
      booking = updatedBooking,
      changedBy = updateSpaceBookingDetails.updatedBy,
      bookingChangedAt = OffsetDateTime.now(),
      previousArrivalDateIfChanged = previousArrivalDateIfChanged,
      previousDepartureDateIfChanged = previousDepartureDateIfChanged,
      previousCharacteristicsIfChanged = previousCharacteristicsIfChanged,
    )

    if (previousArrivalDateIfChanged != null || previousDepartureDateIfChanged != null) {
      updatedBooking.application?.let { application ->
        cas1BookingEmailService.spaceBookingAmended(
          spaceBooking = updatedBooking,
          application = application,
        )
      }
    }

    success(updatedBooking)
  }

  @Transactional
  fun shortenSpaceBooking(
    shortenedBookingDetails: ShortenSpaceBookingDetails,
  ): CasResult<Cas1SpaceBookingEntity> = validatedCasResult {
    val existingBooking = cas1SpaceBookingRepository.findByIdOrNull(shortenedBookingDetails.bookingId)

    validateShortenedSpaceBooking(shortenedBookingDetails, existingBooking)

    val updateSpaceBookingDetails = UpdateSpaceBookingDetails(
      bookingId = shortenedBookingDetails.bookingId,
      premisesId = shortenedBookingDetails.premisesId,
      arrivalDate = null,
      departureDate = shortenedBookingDetails.departureDate,
      characteristics = null,
      updatedBy = shortenedBookingDetails.updatedBy,
    )

    validateUpdateSpaceBooking(updateSpaceBookingDetails)

    if (validationErrors.any()) return fieldValidationError

    val updatedBooking = updateExistingSpaceBooking(existingBooking!!, updateSpaceBookingDetails)

    success(updatedBooking)
  }

  @Transactional
  @Suppress("ReturnCount")
  fun createEmergencyTransfer(
    premisesId: UUID,
    bookingId: UUID,
    user: UserEntity,
    cas1NewEmergencyTransfer: Cas1NewEmergencyTransfer,
  ): CasResult<Cas1SpaceBookingEntity> {
    val destinationPremises = cas1PremisesService.findPremiseById(cas1NewEmergencyTransfer.destinationPremisesId)
      ?: return CasResult.NotFound("Premises", cas1NewEmergencyTransfer.destinationPremisesId.toString())

    if (cas1NewEmergencyTransfer.arrivalDate != LocalDate.now()) {
      return GeneralValidationError("The provided arrival date must be today's date")
    }

    if (cas1NewEmergencyTransfer.arrivalDate >= cas1NewEmergencyTransfer.departureDate) {
      return GeneralValidationError("The provided departure date must be after the arrival date")
    }

    lockableCas1SpaceBookingEntityRepository.acquirePessimisticLock(bookingId)
    val existingCas1SpaceBooking = cas1SpaceBookingRepository.findByIdOrNull(bookingId)
      ?: return CasResult.NotFound("Space Booking", bookingId.toString())

    if (existingCas1SpaceBooking.premises.id != premisesId) {
      return ConflictError(existingCas1SpaceBooking.premises.id, "The booking is not associated with the specified premises $premisesId")
    }

    if (!existingCas1SpaceBooking.isEligibleForEmergencyTransfer()) {
      return ConflictError(existingCas1SpaceBooking.id, "The booking is not eligible for an emergency transfer")
    }

    val placementRequest = existingCas1SpaceBooking.placementRequest!!

    val emergencyTransferSpaceBooking = createSpaceBookingEntity(
      premises = destinationPremises,
      placementRequest = placementRequest,
      expectedArrivalDate = cas1NewEmergencyTransfer.arrivalDate,
      expectedDepartureDate = cas1NewEmergencyTransfer.departureDate,
      createdBy = user,
      characteristics = existingCas1SpaceBooking.criteria,
      transferType = TransferType.EMERGENCY,
    )

    updateTransferredSpaceBooking(existingCas1SpaceBooking, emergencyTransferSpaceBooking, cas1NewEmergencyTransfer.arrivalDate)

    cas1SpaceBookingManagementDomainEventService.emergencyTransferCreated(
      user,
      existingCas1SpaceBooking,
      emergencyTransferSpaceBooking,
    )
    return Success(emergencyTransferSpaceBooking)
  }

  @Transactional
  @Suppress("ReturnCount")
  fun createPlannedTransfer(
    bookingId: UUID,
    user: UserEntity,
    cas1NewPlannedTransfer: Cas1NewPlannedTransfer,
  ): CasResult<Unit> {
    lockableCas1SpaceBookingEntityRepository.acquirePessimisticLock(bookingId)

    val destinationPremises = cas1PremisesService.findPremiseById(cas1NewPlannedTransfer.destinationPremisesId)
      ?: return CasResult.NotFound("Premises", cas1NewPlannedTransfer.destinationPremisesId.toString())

    val changeRequest = cas1ChangeRequestService.findChangeRequest(cas1NewPlannedTransfer.changeRequestId)
      ?: return CasResult.NotFound("Change Request", cas1NewPlannedTransfer.changeRequestId.toString())

    val existingCas1SpaceBooking = cas1SpaceBookingRepository.findByIdOrNull(bookingId)
      ?: return CasResult.NotFound("Space Booking", bookingId.toString())

    if (changeRequest.resolved) {
      return if (changeRequest.decision == ChangeRequestDecision.APPROVED) {
        Success(Unit)
      } else {
        return GeneralValidationError("A decision has already been made for the change request")
      }
    }

    validatePlannedTransferDetails(cas1NewPlannedTransfer, existingCas1SpaceBooking, changeRequest)?.let { return it }

    val placementRequest = existingCas1SpaceBooking.placementRequest!!

    val plannedTransferSpaceBooking = createSpaceBookingEntity(
      premises = destinationPremises,
      placementRequest = placementRequest,
      expectedArrivalDate = cas1NewPlannedTransfer.arrivalDate,
      expectedDepartureDate = cas1NewPlannedTransfer.departureDate,
      createdBy = user,
      characteristics = getCharacteristicsEntity(cas1NewPlannedTransfer.characteristics),
      transferType = TransferType.PLANNED,
    )

    updateTransferredSpaceBooking(existingCas1SpaceBooking, plannedTransferSpaceBooking, cas1NewPlannedTransfer.arrivalDate)

    cas1ChangeRequestService.approvedPlannedTransfer(
      changeRequest = changeRequest,
      user = user,
      from = existingCas1SpaceBooking,
      to = plannedTransferSpaceBooking,
    )

    return Success(Unit)
  }

  private fun updateTransferredSpaceBooking(
    existingSpaceBooking: Cas1SpaceBookingEntity,
    transferredSpaceBooking: Cas1SpaceBookingEntity,
    departureDate: LocalDate,
  ): Cas1SpaceBookingEntity {
    existingSpaceBooking.transferredTo = transferredSpaceBooking
    existingSpaceBooking.expectedDepartureDate = departureDate
    existingSpaceBooking.canonicalDepartureDate = departureDate

    return cas1SpaceBookingRepository.saveAndFlush(existingSpaceBooking)
  }

  @Suppress("ReturnCount")
  private fun validatePlannedTransferDetails(
    plannedTransfer: Cas1NewPlannedTransfer,
    booking: Cas1SpaceBookingEntity,
    changeRequest: Cas1ChangeRequestEntity,
  ): CasResult<Unit>? {
    if (!plannedTransfer.arrivalDate.isAfter(LocalDate.now())) {
      return GeneralValidationError("The provided arrival date (${plannedTransfer.arrivalDate}) must be in the future")
    }

    if (plannedTransfer.arrivalDate >= plannedTransfer.departureDate) {
      return GeneralValidationError("The provided departure date (${plannedTransfer.departureDate}) must be after the arrival date (${plannedTransfer.arrivalDate})")
    }

    if (changeRequest.spaceBooking.id != booking.id) {
      return GeneralValidationError("The booking is not associated with the specified change request ${changeRequest.id}")
    }

    if (!booking.hasArrival()) {
      return GeneralValidationError("Arrival must be recorded for the associated space booking")
    }

    return null
  }

  private fun CasResultValidatedScope<Cas1SpaceBookingEntity>.validateUpdateSpaceBooking(
    updateSpaceBookingDetails: UpdateSpaceBookingDetails,
  ) {
    val premises = cas1PremisesService.findPremiseById(updateSpaceBookingDetails.premisesId)
    if (premises == null) {
      "$.premisesId" hasValidationError "doesNotExist"
      return
    }
    val bookingToUpdate = cas1SpaceBookingRepository.findByIdOrNull(updateSpaceBookingDetails.bookingId)
    if (bookingToUpdate == null) {
      "$.bookingId" hasValidationError "doesNotExist"
      return
    }

    if (bookingToUpdate.isCancelled()) {
      "$.bookingId" hasValidationError "This Booking is cancelled and as such cannot be modified"
    }
    if (bookingToUpdate.hasDeparted() || bookingToUpdate.hasNonArrival()) {
      "$.bookingId" hasValidationError "hasDepartedOrNonArrival"
    }
    if (bookingToUpdate.premises.id != updateSpaceBookingDetails.premisesId) {
      "$.premisesId" hasValidationError "premisesMismatch"
    }

    val effectiveArrivalDate = if (bookingToUpdate.hasArrival()) {
      bookingToUpdate.actualArrivalDate
    } else {
      updateSpaceBookingDetails.arrivalDate ?: bookingToUpdate.expectedArrivalDate
    }

    val effectiveDepartureDate = updateSpaceBookingDetails.departureDate ?: bookingToUpdate.expectedDepartureDate

    if (effectiveDepartureDate.isBefore(effectiveArrivalDate)) {
      "$.departureDate" hasValidationError "The departure date is before the arrival date."
    }
  }

  private fun CasResultValidatedScope<Cas1SpaceBookingEntity>.validateShortenedSpaceBooking(
    shortenedBookingDetails: ShortenSpaceBookingDetails,
    existingSpaceBooking: Cas1SpaceBookingEntity?,
  ) {
    val newDepartureDate = shortenedBookingDetails.departureDate

    if (newDepartureDate.isBefore(LocalDate.now())) {
      "$.departureDate" hasValidationError "The departure date is in the past."
    }

    existingSpaceBooking?.let {
      if (newDepartureDate.isAfter(existingSpaceBooking.expectedDepartureDate)) {
        "$.departureDate" hasValidationError "The departure date is after the current expected departure date."
      }

      if (newDepartureDate.isEqual(existingSpaceBooking.expectedDepartureDate)) {
        "$.departureDate" hasValidationError "The departure date is the same as the current expected departure date."
      }
    }
  }

  private fun updateExistingSpaceBooking(
    bookingToUpdate: Cas1SpaceBookingEntity,
    updateSpaceBookingDetails: UpdateSpaceBookingDetails,
  ): Cas1SpaceBookingEntity {
    if (bookingToUpdate.hasArrival()) {
      bookingToUpdate.updateDepartureDates(updateSpaceBookingDetails)
    } else {
      bookingToUpdate.updateArrivalDates(updateSpaceBookingDetails)
      bookingToUpdate.updateDepartureDates(updateSpaceBookingDetails)
    }

    if (updateSpaceBookingDetails.characteristics != null) {
      updateRoomCharacteristics(bookingToUpdate, updateSpaceBookingDetails.characteristics)
    }

    return cas1SpaceBookingRepository.save(bookingToUpdate)
  }

  private fun updateRoomCharacteristics(
    booking: Cas1SpaceBookingEntity,
    newRoomCharacteristics: List<CharacteristicEntity>,
  ) {
    booking.criteria.apply {
      retainAll { it.isModelScopePremises() }
      addAll(newRoomCharacteristics)
    }
  }

  private fun Cas1SpaceBookingEntity.updateDepartureDates(updateSpaceBookingDetails: UpdateSpaceBookingDetails) {
    if (updateSpaceBookingDetails.departureDate != null) {
      this.expectedDepartureDate = updateSpaceBookingDetails.departureDate
      this.canonicalDepartureDate = updateSpaceBookingDetails.departureDate
    }
  }

  private fun Cas1SpaceBookingEntity.updateArrivalDates(updateSpaceBookingDetails: UpdateSpaceBookingDetails) {
    if (updateSpaceBookingDetails.arrivalDate != null) {
      this.expectedArrivalDate = updateSpaceBookingDetails.arrivalDate
      this.canonicalArrivalDate = updateSpaceBookingDetails.arrivalDate
    }
  }

  private fun getCharacteristicsEntity(cas1SpaceCharacteristics: List<Cas1SpaceCharacteristic>?): List<CharacteristicEntity> = characteristicService.getCharacteristicsByPropertyNames(
    cas1SpaceCharacteristics.orEmpty().toSet().map { it.value },
    ServiceName.approvedPremises,
  )

  private fun createSpaceBookingEntity(
    premises: ApprovedPremisesEntity,
    placementRequest: PlacementRequestEntity,
    expectedArrivalDate: LocalDate,
    expectedDepartureDate: LocalDate,
    createdBy: UserEntity,
    characteristics: List<CharacteristicEntity>,
    transferType: TransferType?,
  ): Cas1SpaceBookingEntity {
    val application = placementRequest.application
    val spaceBooking = cas1SpaceBookingRepository.saveAndFlush(
      Cas1SpaceBookingEntity(
        id = UUID.randomUUID(),
        premises = premises,
        application = application,
        offlineApplication = null,
        placementRequest = placementRequest,
        createdBy = createdBy,
        createdAt = OffsetDateTime.now(clock),
        expectedArrivalDate = expectedArrivalDate,
        expectedDepartureDate = expectedDepartureDate,
        actualArrivalDate = null,
        actualArrivalTime = null,
        actualDepartureDate = null,
        actualDepartureTime = null,
        canonicalArrivalDate = expectedArrivalDate,
        canonicalDepartureDate = expectedDepartureDate,
        crn = placementRequest.application.crn,
        keyWorkerStaffCode = null,
        keyWorkerName = null,
        keyWorkerAssignedAt = null,
        cancellationOccurredAt = null,
        cancellationRecordedAt = null,
        cancellationReason = null,
        cancellationReasonNotes = null,
        departureMoveOnCategory = null,
        departureReason = null,
        departureNotes = null,
        criteria = characteristics.toMutableList(),
        nonArrivalConfirmedAt = null,
        nonArrivalNotes = null,
        nonArrivalReason = null,
        deliusEventNumber = application.eventNumber,
        migratedManagementInfoFrom = null,
        transferredTo = null,
        transferType = transferType,
        deliusId = null,
      ),
    )
    return spaceBooking
  }

  data class UpdateSpaceBookingDetails(
    val bookingId: UUID,
    val premisesId: UUID,
    val arrivalDate: LocalDate?,
    val departureDate: LocalDate?,
    val characteristics: List<CharacteristicEntity>?,
    val updatedBy: UserEntity,
  )

  data class ShortenSpaceBookingDetails(
    val bookingId: UUID,
    val premisesId: UUID,
    val departureDate: LocalDate,
    val reason: String,
    val updatedBy: UserEntity,
  )
}
