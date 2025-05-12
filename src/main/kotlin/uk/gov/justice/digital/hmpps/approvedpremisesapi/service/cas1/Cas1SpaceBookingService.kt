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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TransferType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.CasResultValidatedScope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult.GeneralValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult.Success
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ifError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingCreateService.CreateBookingDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingUpdateService.UpdateBookingDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingCancelledEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import java.time.Instant
import java.time.LocalDate
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
  private val cas1ApplicationStatusService: Cas1ApplicationStatusService,
  private val cancellationReasonRepository: CancellationReasonRepository,
  private val lockablePlacementRequestRepository: LockablePlacementRequestRepository,
  private val lockableCas1SpaceBookingEntityRepository: LockableCas1SpaceBookingEntityRepository,
  private val cas1ChangeRequestService: Cas1ChangeRequestService,
  private val characteristicService: CharacteristicService,
  private val cas1SpaceBookingActionsService: Cas1SpaceBookingActionsService,
  private val cas1SpaceBookingCreateService: Cas1SpaceBookingCreateService,
  private val cas1SpaceBookingUpdateService: Cas1SpaceBookingUpdateService,
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
    lockablePlacementRequestRepository.acquirePessimisticLock(placementRequestId)

    val validatedCreateBooking = when (
      val result = cas1SpaceBookingCreateService.validate(
        CreateBookingDetails(
          premisesId = premisesId,
          placementRequestId = placementRequestId,
          expectedArrivalDate = arrivalDate,
          expectedDepartureDate = departureDate,
          createdBy = createdBy,
          characteristics = characteristics,
          transferType = null,
          transferredFrom = null,
        ),
      )
    ) {
      is CasResult.Error -> return result.reviseType()
      is Success -> result.value
    }

    val placementRequest = placementRequestService.getPlacementRequestOrNull(placementRequestId)
    placementRequest!!.booking?.let {
      if (it.isActive()) {
        return it.id hasConflictError "A legacy Booking already exists for this premises and placement request"
      }
    }

    if (cas1SpaceBookingRepository.findByPlacementRequestId(placementRequestId).any { it.isActive() }) {
      return placementRequestId hasConflictError "A Space Booking already exists for this placement request"
    }

    success(cas1SpaceBookingCreateService.create(validatedCreateBooking))
  }

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
    appealChangeRequestId: UUID?,
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
    cas1BookingDomainEventService.spaceBookingCancelled(Cas1BookingCancelledEvent(spaceBooking, user, reason, appealChangeRequestId))
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

  @Transactional
  fun updateBooking(
    updateBookingDetails: UpdateBookingDetails,
  ): CasResult<Cas1SpaceBookingEntity> {
    cas1SpaceBookingUpdateService.validate(updateBookingDetails).ifError { return it.reviseType() }

    return Success(cas1SpaceBookingUpdateService.update(updateBookingDetails))
  }

  @Transactional
  fun shortenBooking(
    shortenedBookingDetails: UpdateBookingDetails,
  ): CasResult<Cas1SpaceBookingEntity> = validatedCasResult {
    val existingBooking = cas1SpaceBookingRepository.findByIdOrNull(shortenedBookingDetails.bookingId)

    validateShortenedSpaceBooking(shortenedBookingDetails, existingBooking)
    if (hasErrors()) return errors()

    cas1SpaceBookingActionsService.determineActions(existingBooking!!)
      .unavailableReason(SpaceBookingAction.SHORTEN)?.let {
        return GeneralValidationError(it)
      }

    val updateBookingDetails = UpdateBookingDetails(
      bookingId = shortenedBookingDetails.bookingId,
      premisesId = shortenedBookingDetails.premisesId,
      departureDate = shortenedBookingDetails.departureDate,
      updatedBy = shortenedBookingDetails.updatedBy,
      updateType = UpdateType.SHORTENING,
    )
    cas1SpaceBookingUpdateService.validate(updateBookingDetails).ifError { return it.reviseType() }

    return Success(cas1SpaceBookingUpdateService.update(updateBookingDetails))
  }

  @Transactional
  @Suppress("ReturnCount", "MagicNumber")
  fun createEmergencyTransfer(
    premisesId: UUID,
    bookingId: UUID,
    user: UserEntity,
    requirements: Cas1NewEmergencyTransfer,
  ): CasResult<Cas1SpaceBookingEntity> {
    val arrivalDate = requirements.arrivalDate
    val departureDate = requirements.departureDate

    lockableCas1SpaceBookingEntityRepository.acquirePessimisticLock(bookingId)

    if (arrivalDate.isAfter(LocalDate.now()) || arrivalDate.isBefore(LocalDate.now().minusDays(7))) {
      return GeneralValidationError("The provided arrival date must be today, or within the last 7 days")
    }

    val updateExistingBookingDetails = UpdateBookingDetails(
      bookingId = bookingId,
      premisesId = premisesId,
      departureDate = arrivalDate,
      updatedBy = user,
      updateType = UpdateType.TRANSFER,
    )
    cas1SpaceBookingUpdateService.validate(updateExistingBookingDetails).ifError { return it.reviseType() }

    val existingCas1SpaceBooking = cas1SpaceBookingRepository.findByIdOrNull(bookingId)!!

    cas1SpaceBookingActionsService.determineActions(existingCas1SpaceBooking)
      .unavailableReason(SpaceBookingAction.PLANNED_TRANSFER_REQUEST)?.let {
        return GeneralValidationError(it)
      }

    val validatedCreateBooking = when (
      val result = cas1SpaceBookingCreateService.validate(
        CreateBookingDetails(
          premisesId = requirements.destinationPremisesId,
          placementRequestId = existingCas1SpaceBooking.placementRequest!!.id,
          expectedArrivalDate = arrivalDate,
          expectedDepartureDate = departureDate,
          createdBy = user,
          characteristics = existingCas1SpaceBooking.criteria,
          transferType = TransferType.EMERGENCY,
          transferredFrom = existingCas1SpaceBooking,
        ),
      )
    ) {
      is CasResult.Error -> return result.reviseType()
      is Success -> result.value
    }

    cas1SpaceBookingUpdateService.update(updateExistingBookingDetails)

    val emergencyTransferSpaceBooking = cas1SpaceBookingCreateService.create(validatedCreateBooking)

    return Success(emergencyTransferSpaceBooking)
  }

  @Transactional
  @Suppress("ReturnCount")
  fun createPlannedTransfer(
    bookingId: UUID,
    user: UserEntity,
    cas1NewPlannedTransfer: Cas1NewPlannedTransfer,
  ): CasResult<Unit> {
    val arrivalDate = cas1NewPlannedTransfer.arrivalDate
    val departureDate = cas1NewPlannedTransfer.departureDate

    lockableCas1SpaceBookingEntityRepository.acquirePessimisticLock(bookingId)

    val changeRequest = cas1ChangeRequestService.findChangeRequest(cas1NewPlannedTransfer.changeRequestId)
      ?: return CasResult.NotFound("Change Request", cas1NewPlannedTransfer.changeRequestId.toString())

    if (changeRequest.resolved) {
      return if (changeRequest.decision == ChangeRequestDecision.APPROVED) {
        Success(Unit)
      } else {
        return GeneralValidationError("A decision has already been made for the change request")
      }
    }

    if (!arrivalDate.isAfter(LocalDate.now())) {
      return GeneralValidationError("The provided arrival date ($arrivalDate) must be in the future")
    }

    if (changeRequest.spaceBooking.id != bookingId) {
      return GeneralValidationError("The booking is not associated with the specified change request ${changeRequest.id}")
    }

    val existingCas1SpaceBooking = cas1SpaceBookingRepository.findByIdOrNull(bookingId)
      ?: return CasResult.NotFound("Space Booking", bookingId.toString())

    val placementRequest = existingCas1SpaceBooking.placementRequest!!

    val validatedCreateBooking = when (
      val result = cas1SpaceBookingCreateService.validate(
        CreateBookingDetails(
          premisesId = cas1NewPlannedTransfer.destinationPremisesId,
          placementRequestId = placementRequest.id,
          expectedArrivalDate = arrivalDate,
          expectedDepartureDate = departureDate,
          createdBy = user,
          characteristics = getCharacteristicsEntity(cas1NewPlannedTransfer.characteristics),
          transferType = TransferType.PLANNED,
          transferredFrom = existingCas1SpaceBooking,
        ),
      )
    ) {
      is CasResult.Error -> return result.reviseType()
      is Success -> result.value
    }

    val updateExistingBookingDetails = UpdateBookingDetails(
      bookingId = bookingId,
      premisesId = existingCas1SpaceBooking.premises.id,
      departureDate = arrivalDate,
      updatedBy = user,
      updateType = UpdateType.TRANSFER,
    )
    cas1SpaceBookingUpdateService.validate(updateExistingBookingDetails).ifError { return it.reviseType() }

    cas1SpaceBookingUpdateService.update(updateExistingBookingDetails)

    val newSpaceBooking = cas1SpaceBookingCreateService.create(validatedCreateBooking)

    cas1ChangeRequestService.approvedPlannedTransfer(changeRequest, user, newSpaceBooking)

    return Success(Unit)
  }

  private fun CasResultValidatedScope<Cas1SpaceBookingEntity>.validateShortenedSpaceBooking(
    shortenedBookingDetails: UpdateBookingDetails,
    existingSpaceBooking: Cas1SpaceBookingEntity?,
  ) {
    val newDepartureDate = shortenedBookingDetails.departureDate

    if (newDepartureDate!!.isBefore(LocalDate.now())) {
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

  private fun getCharacteristicsEntity(cas1SpaceCharacteristics: List<Cas1SpaceCharacteristic>?): List<CharacteristicEntity> = characteristicService.getCharacteristicsByPropertyNames(
    cas1SpaceCharacteristics.orEmpty().toSet().map { it.value },
    ServiceName.approvedPremises,
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

  data class SpaceBookingFilterCriteria(
    val residency: Cas1SpaceBookingResidency?,
    val crnOrName: String?,
    val keyWorkerStaffCode: String?,
  )

  enum class UpdateType {
    AMENDMENT,
    SHORTENING,
    TRANSFER,
  }

  data class SearchResultContainer(
    val results: List<Cas1SpaceBookingSearchResult>,
    val paginationMetadata: PaginationMetadata?,
    val premises: ApprovedPremisesEntity,
  )
}
