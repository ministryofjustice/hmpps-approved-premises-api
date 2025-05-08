package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewEmergencyTransfer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewPlannedTransfer
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableCas1SpaceBookingEntityRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockablePlacementRequestRepository
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingCancelledEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingChangedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingCreatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.TransferInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
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
  private val cas1ApplicationStatusService: Cas1ApplicationStatusService,
  private val cancellationReasonRepository: CancellationReasonRepository,
  private val lockablePlacementRequestRepository: LockablePlacementRequestRepository,
  private val lockableCas1SpaceBookingEntityRepository: LockableCas1SpaceBookingEntityRepository,
  private val cas1ChangeRequestService: Cas1ChangeRequestService,
  private val characteristicService: CharacteristicService,
  private val cas1SpaceBookingActionsService: Cas1SpaceBookingActionsService,
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

    val spaceBooking = createSpaceBooking(
      premises = premises,
      placementRequest = placementRequest,
      expectedArrivalDate = arrivalDate,
      expectedDepartureDate = departureDate,
      createdBy = createdBy,
      characteristics = characteristics,
    )

    success(spaceBooking)
  }

  data class DepartureInfo(
    val departureDate: LocalDate,
    val departureTime: LocalTime,
    val reasonId: UUID,
    val moveOnCategoryId: UUID? = null,
    val notes: String? = null,
  )

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
    cas1BookingDomainEventService.spaceBookingCancelled(Cas1BookingCancelledEvent(spaceBooking, user, reason))
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
      Cas1BookingChangedEvent(
        booking = updatedBooking,
        changedBy = updateSpaceBookingDetails.updatedBy,
        bookingChangedAt = OffsetDateTime.now(clock),
        previousArrivalDateIfChanged = previousArrivalDateIfChanged,
        previousDepartureDateIfChanged = previousDepartureDateIfChanged,
        previousCharacteristicsIfChanged = previousCharacteristicsIfChanged,
      ),
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

    val previousDepartureDate = existingBooking!!.expectedDepartureDate

    val updatedBooking = updateExistingSpaceBooking(existingBooking!!, updateSpaceBookingDetails)

    updatedBooking.application?.let { application ->
      cas1BookingEmailService.spaceBookingShortened(
        spaceBooking = updatedBooking,
        application = application,
      )
    }

    success(updatedBooking)
  }

  @Transactional
  @Suppress("ReturnCount", "MagicNumber")
  fun createEmergencyTransfer(
    premisesId: UUID,
    bookingId: UUID,
    user: UserEntity,
    requirements: Cas1NewEmergencyTransfer,
  ): CasResult<Cas1SpaceBookingEntity> {
    val destinationPremises = cas1PremisesService.findPremiseById(requirements.destinationPremisesId)
      ?: return CasResult.NotFound("Premises", requirements.destinationPremisesId.toString())

    val arrivalDate = requirements.arrivalDate
    val departureDate = requirements.departureDate

    if (arrivalDate.isAfter(LocalDate.now()) || arrivalDate.isBefore(LocalDate.now().minusDays(7))) {
      return GeneralValidationError("The provided arrival date must be today, or within the last 7 days")
    }

    if (arrivalDate >= departureDate) {
      return GeneralValidationError("The provided departure date must be after the arrival date")
    }

    lockableCas1SpaceBookingEntityRepository.acquirePessimisticLock(bookingId)
    val existingCas1SpaceBooking = cas1SpaceBookingRepository.findByIdOrNull(bookingId)
      ?: return CasResult.NotFound("Space Booking", bookingId.toString())

    if (existingCas1SpaceBooking.premises.id != premisesId) {
      return ConflictError(existingCas1SpaceBooking.premises.id, "The booking is not associated with the specified premises $premisesId")
    }

    cas1SpaceBookingActionsService.determineActions(existingCas1SpaceBooking)
      .unavailableReason(SpaceBookingAction.PLANNED_TRANSFER_REQUEST)?.let {
        return GeneralValidationError(it)
      }

    val placementRequest = existingCas1SpaceBooking.placementRequest!!

    val emergencyTransferSpaceBooking = createSpaceBooking(
      premises = destinationPremises,
      placementRequest = placementRequest,
      expectedArrivalDate = arrivalDate,
      expectedDepartureDate = departureDate,
      createdBy = user,
      characteristics = existingCas1SpaceBooking.criteria,
      transferInfo = BookingTransferInfo(
        type = TransferType.EMERGENCY,
        booking = existingCas1SpaceBooking,
        changeRequest = null,
      ),
      beforeRaisingBookingMadeDomainEvent = { createdSpaceBooking ->
        cas1SpaceBookingManagementDomainEventService.emergencyTransferCreated(
          user,
          existingCas1SpaceBooking,
          createdSpaceBooking,
        )
      },
    )

    updateTransferredSpaceBooking(existingCas1SpaceBooking, emergencyTransferSpaceBooking, arrivalDate)

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

    val plannedTransferSpaceBooking = createSpaceBooking(
      premises = destinationPremises,
      placementRequest = placementRequest,
      expectedArrivalDate = cas1NewPlannedTransfer.arrivalDate,
      expectedDepartureDate = cas1NewPlannedTransfer.departureDate,
      createdBy = user,
      characteristics = getCharacteristicsEntity(cas1NewPlannedTransfer.characteristics),
      transferInfo = BookingTransferInfo(
        type = TransferType.PLANNED,
        booking = existingCas1SpaceBooking,
        changeRequest = changeRequest,
      ),
      beforeRaisingBookingMadeDomainEvent = { createdSpaceBooking ->
        cas1ChangeRequestService.approvedPlannedTransfer(
          changeRequest = changeRequest,
          user = user,
        )
      },
    )

    updateTransferredSpaceBooking(existingCas1SpaceBooking, plannedTransferSpaceBooking, cas1NewPlannedTransfer.arrivalDate)

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

  private fun createSpaceBooking(
    premises: ApprovedPremisesEntity,
    placementRequest: PlacementRequestEntity,
    expectedArrivalDate: LocalDate,
    expectedDepartureDate: LocalDate,
    createdBy: UserEntity,
    characteristics: List<CharacteristicEntity>,
    transferInfo: BookingTransferInfo? = null,
    beforeRaisingBookingMadeDomainEvent: (Cas1SpaceBookingEntity) -> Unit = {},
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
        // this is populated on retrieval from the origin space booking
        transferredTo = null,
        transferType = transferInfo?.type,
        deliusId = null,
      ),
    )

    cas1ApplicationStatusService.spaceBookingMade(spaceBooking)

    beforeRaisingBookingMadeDomainEvent(spaceBooking)

    cas1BookingDomainEventService.spaceBookingMade(
      Cas1BookingCreatedEvent(
        booking = spaceBooking,
        createdBy = createdBy,
        transferInfo = transferInfo?.let {
          TransferInfo(
            type = it.type,
            changeRequestId = it.changeRequest?.id,
            booking = it.booking,
          )
        },
      ),
    )

    cas1BookingEmailService.spaceBookingMade(spaceBooking, application)

    return spaceBooking
  }

  private data class BookingTransferInfo(
    val type: TransferType,
    val booking: Cas1SpaceBookingEntity,
    val changeRequest: Cas1ChangeRequestEntity? = null,
  )

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
