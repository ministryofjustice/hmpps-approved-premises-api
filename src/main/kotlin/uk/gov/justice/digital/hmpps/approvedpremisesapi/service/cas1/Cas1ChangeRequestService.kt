package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import jakarta.transaction.Transactional
import org.springframework.context.event.EventListener
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1RejectChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockablePlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRejectionReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRepository.FindOpenChangeRequestResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.LockableCas1ChangeRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult.GeneralValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult.Success
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SpringEventPublisher
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.ArrivalRecorded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlacementAppealAccepted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlacementAppealCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlacementAppealRejected
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlannedTransferRequestAccepted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlannedTransferRequestCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.PlannedTransferRequestRejected
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas1ChangeRequestService(
  private val cas1ChangeRequestRepository: Cas1ChangeRequestRepository,
  private val placementRequestRepository: PlacementRequestRepository,
  private val cas1ChangeRequestReasonRepository: Cas1ChangeRequestReasonRepository,
  private val cas1SpaceBookingRepository: Cas1SpaceBookingRepository,
  private val lockableCas1ChangeRequestEntityRepository: LockableCas1ChangeRequestRepository,
  private val cas1ChangeRequestRejectionReasonRepository: Cas1ChangeRequestRejectionReasonRepository,
  private val userService: UserService,
  private val spaceBookingActionsService: Cas1SpaceBookingActionsService,
  private val lockablePlacementRequestRepository: LockablePlacementRequestRepository,
  private val springEventPublisher: SpringEventPublisher,
) {

  @SuppressWarnings("CyclomaticComplexMethod", "ComplexCondition")
  @Transactional
  fun createChangeRequest(placementRequestId: UUID, requirements: Cas1NewChangeRequest): CasResult<Unit> = validatedCasResult {
    val placementRequest = placementRequestRepository.findByIdOrNull(placementRequestId)
      ?: return CasResult.NotFound("Placement Request", placementRequestId.toString())

    val requestReason = cas1ChangeRequestReasonRepository.findByIdOrNull(requirements.reasonId)
      ?: return CasResult.NotFound("Change Request Reason", requirements.reasonId.toString())

    val spaceBooking = cas1SpaceBookingRepository.findByIdOrNull(requirements.spaceBookingId)
      ?: return CasResult.NotFound("Space Booking", requirements.spaceBookingId.toString())

    if (!placementRequest.spaceBookings.contains(spaceBooking)) return CasResult.NotFound("Placement Request with Space Booking", spaceBooking.id.toString())

    lockablePlacementRequestRepository.acquirePessimisticLock(placementRequestId)

    val type = ChangeRequestType.valueOf(requirements.type.name)
    val requestJson = requirements.requestJson.toString()

    if (cas1ChangeRequestRepository.findAllByPlacementRequestAndResolvedIsFalse(placementRequest).any {
        it.type == type &&
          it.requestReason == requestReason &&
          it.requestJson == requestJson &&
          it.spaceBooking == spaceBooking
      }
    ) {
      return Success(Unit)
    }

    val requiredAction = when (requirements.type) {
      Cas1ChangeRequestType.PLACEMENT_APPEAL -> SpaceBookingAction.APPEAL_CREATE
      Cas1ChangeRequestType.PLACEMENT_EXTENSION -> error("to be implemented")
      Cas1ChangeRequestType.PLANNED_TRANSFER -> SpaceBookingAction.PLANNED_TRANSFER_REQUEST
    }

    spaceBookingActionsService.determineActions(spaceBooking).unavailableReason(requiredAction)?.let {
      return GeneralValidationError(it)
    }

    val now = OffsetDateTime.now()

    val createdChangeRequest = cas1ChangeRequestRepository.save(
      Cas1ChangeRequestEntity(
        id = UUID.randomUUID(),
        placementRequest = placementRequest,
        spaceBooking = spaceBooking,
        type = type,
        requestJson = requirements.requestJson.toString(),
        requestReason = requestReason,
        decisionJson = null,
        decision = null,
        rejectionReason = null,
        decisionMadeByUser = null,
        resolved = false,
        resolvedAt = null,
        createdAt = now,
        updatedAt = now,
      ),
    )

    when (type) {
      ChangeRequestType.PLACEMENT_APPEAL -> {
        springEventPublisher.publishEvent(PlacementAppealCreated(createdChangeRequest, userService.getUserForRequest()))
      }
      ChangeRequestType.PLACEMENT_EXTENSION -> error("to be implemented")
      ChangeRequestType.PLANNED_TRANSFER -> {
        springEventPublisher.publishEvent(PlannedTransferRequestCreated(createdChangeRequest, userService.getUserForRequest()))
      }
    }

    return success(Unit)
  }

  fun findOpen(
    cruManagementAreaId: UUID?,
    pageCriteria: PageCriteria<Cas1ChangeRequestSortField>,
  ): List<FindOpenChangeRequestResult> = cas1ChangeRequestRepository.findOpen(
    cruManagementAreaId,
    pageCriteria.toPageableOrAllPages(
      sortBy = when (pageCriteria.sortBy) {
        Cas1ChangeRequestSortField.NAME -> "name"
        Cas1ChangeRequestSortField.TIER -> "tier"
        Cas1ChangeRequestSortField.CANONICAL_ARRIVAL_DATE -> "canonicalArrivalDate"
      },
    ),
  )

  @Transactional
  fun approvePlacementAppeal(changeRequestId: UUID, user: UserEntity): CasResult<Unit> = validatedCasResult {
    val changeRequest = findChangeRequest(changeRequestId)
      ?: return CasResult.NotFound("change request", changeRequestId.toString())

    if (changeRequest.resolved) {
      return GeneralValidationError("This change request is already resolved")
    }

    approveChangeRequest(changeRequest, user)

    springEventPublisher.publishEvent(PlacementAppealAccepted(changeRequest))

    return Success(Unit)
  }

  @Transactional
  fun approvedPlannedTransfer(
    changeRequest: Cas1ChangeRequestEntity,
    user: UserEntity,
  ) {
    approveChangeRequest(changeRequest, user)

    springEventPublisher.publishEvent(PlannedTransferRequestAccepted(changeRequest))
  }

  @Transactional
  fun rejectChangeRequest(
    placementRequestId: UUID,
    changeRequestId: UUID,
    cas1RejectChangeRequest: Cas1RejectChangeRequest,
  ): CasResult<Unit> = validatedCasResult {
    val changeRequest = findChangeRequest(changeRequestId)
      ?: return CasResult.NotFound("change request", changeRequestId.toString())

    if (changeRequest.placementRequest.id != placementRequestId) {
      return GeneralValidationError("The change request does not belong to the specified placement request")
    }

    lockableCas1ChangeRequestEntityRepository.acquirePessimisticLock(changeRequestId)

    if (changeRequest.decision != null) {
      return if (changeRequest.decision == ChangeRequestDecision.REJECTED) {
        Success(Unit)
      } else {
        GeneralValidationError("A decision has already been made for the change request")
      }
    }

    val changeRequestRejectReason = cas1ChangeRequestRejectionReasonRepository.findByIdAndArchivedIsFalse(cas1RejectChangeRequest.rejectionReasonId)
      ?: return GeneralValidationError("The change request reject reason not found")

    changeRequest.decision = ChangeRequestDecision.REJECTED
    changeRequest.rejectionReason = changeRequestRejectReason
    changeRequest.resolve()
    cas1ChangeRequestRepository.save(changeRequest)

    when (changeRequest.type) {
      ChangeRequestType.PLACEMENT_APPEAL -> {
        springEventPublisher.publishEvent(PlacementAppealRejected(changeRequest, userService.getUserForRequest()))
      }
      ChangeRequestType.PLACEMENT_EXTENSION -> Unit
      ChangeRequestType.PLANNED_TRANSFER -> {
        springEventPublisher.publishEvent(PlannedTransferRequestRejected(changeRequest, userService.getUserForRequest()))
      }
    }

    return Success(Unit)
  }

  fun getChangeRequestForPlacementId(
    placementRequestId: UUID,
    changeRequestId: UUID,
  ): CasResult<Cas1ChangeRequestEntity> {
    val changeRequest = findChangeRequest(changeRequestId) ?: return CasResult.NotFound("Change Request", changeRequestId.toString())

    if (changeRequest.placementRequest.id != placementRequestId) {
      return GeneralValidationError("The change request does not belong to the specified placement request")
    }

    return Success(changeRequest)
  }

  @SuppressWarnings("MaxLineLength")
  private fun resolveAllChangeRequestsForSpaceBookingAndType(spaceBooking: Cas1SpaceBookingEntity, changeRequestTypes: List<ChangeRequestType>) = cas1ChangeRequestRepository.findAllBySpaceBookingAndResolvedIsFalseAndTypeIn(spaceBooking, changeRequestTypes).forEach { it.resolve() }

  fun spaceBookingWithdrawn(spaceBooking: Cas1SpaceBookingEntity) = resolveAllChangeRequestsForSpaceBookingAndType(spaceBooking, ChangeRequestType.entries.toList())

  @EventListener
  fun spaceBookingHasArrival(arrivalRecorded: ArrivalRecorded) = resolveAllChangeRequestsForSpaceBookingAndType(arrivalRecorded.spaceBooking, listOf(ChangeRequestType.PLACEMENT_APPEAL))

  fun spaceBookingMarkedAsNonArrival(spaceBooking: Cas1SpaceBookingEntity) = resolveAllChangeRequestsForSpaceBookingAndType(spaceBooking, listOf(ChangeRequestType.PLACEMENT_APPEAL))

  fun spaceBookingMarkedAsDeparted(spaceBooking: Cas1SpaceBookingEntity) = resolveAllChangeRequestsForSpaceBookingAndType(spaceBooking, listOf(ChangeRequestType.PLACEMENT_EXTENSION, ChangeRequestType.PLANNED_TRANSFER))

  fun findChangeRequest(changeRequestId: UUID): Cas1ChangeRequestEntity? = cas1ChangeRequestRepository.findByIdOrNull(changeRequestId)

  private fun approveChangeRequest(changeRequestEntity: Cas1ChangeRequestEntity, user: UserEntity): Cas1ChangeRequestEntity {
    changeRequestEntity.decision = ChangeRequestDecision.APPROVED
    changeRequestEntity.resolve()
    changeRequestEntity.decisionMadeByUser = user
    return cas1ChangeRequestRepository.saveAndFlush(changeRequestEntity)
  }

  private fun Cas1ChangeRequestEntity.resolve() {
    this.resolved = true
    this.resolvedAt = OffsetDateTime.now()
  }
}
