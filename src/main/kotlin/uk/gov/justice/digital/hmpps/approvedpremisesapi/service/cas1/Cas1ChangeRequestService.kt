package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1RejectChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
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
  private val cas1ChangeRequestEmailService: Cas1ChangeRequestEmailService,
  private val cas1ChangeRequestDomainEventService: Cas1ChangeRequestDomainEventService,
  private val userService: UserService,
  private val spaceBookingActionsService: Cas1SpaceBookingActionsService,
) {

  @Transactional
  fun createChangeRequest(placementRequestId: UUID, cas1NewChangeRequest: Cas1NewChangeRequest): CasResult<Unit> = validatedCasResult {
    val placementRequest = placementRequestRepository.findByIdOrNull(placementRequestId)
      ?: return CasResult.NotFound("Placement Request", placementRequestId.toString())

    val requestReason = cas1ChangeRequestReasonRepository.findByIdOrNull(cas1NewChangeRequest.reasonId)
      ?: return CasResult.NotFound("Change Request Reason", cas1NewChangeRequest.reasonId.toString())

    val spaceBooking = cas1SpaceBookingRepository.findByIdOrNull(cas1NewChangeRequest.spaceBookingId)
      ?: return CasResult.NotFound("Space Booking", cas1NewChangeRequest.spaceBookingId.toString())

    if (!placementRequest.spaceBookings.contains(spaceBooking)) return CasResult.NotFound("Placement Request with Space Booking", spaceBooking.id.toString())

    val requiredAction = when (cas1NewChangeRequest.type) {
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
        type = ChangeRequestType.valueOf(cas1NewChangeRequest.type.name),
        requestJson = cas1NewChangeRequest.requestJson.toString(),
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

    when (cas1NewChangeRequest.type) {
      Cas1ChangeRequestType.PLACEMENT_APPEAL -> {
        cas1ChangeRequestEmailService.placementAppealCreated(createdChangeRequest)
        cas1ChangeRequestDomainEventService.placementAppealCreated(
          changeRequest = createdChangeRequest,
          requestingUser = userService.getUserForRequest(),
        )
      }
      Cas1ChangeRequestType.PLACEMENT_EXTENSION -> Unit
      Cas1ChangeRequestType.PLANNED_TRANSFER -> {
        cas1ChangeRequestDomainEventService.plannedTransferRequestCreated(
          changeRequest = createdChangeRequest,
          requestingUser = userService.getUserForRequest(),
        )
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
        Cas1ChangeRequestSortField.LENGTH_OF_STAY_DAYS -> "lengthOfStayDays"
      },
    ),
  )

  @Transactional
  fun approvePlacementAppeal(changeRequestId: UUID, user: UserEntity): CasResult<Unit> = validatedCasResult {
    val changeRequest = findChangeRequest(changeRequestId)

    if (changeRequest == null) {
      return CasResult.NotFound("change request", changeRequestId.toString())
    } else if (changeRequest.resolved) {
      return GeneralValidationError("This change request is already resolved")
    } else {
      approveChangeRequest(changeRequest, user)
    }

    cas1ChangeRequestEmailService.placementAppealAccepted(changeRequest)
    cas1ChangeRequestDomainEventService.placementAppealAccepted(changeRequest)

    return Success(Unit)
  }

  fun approvedPlannedTransfer(
    changeRequest: Cas1ChangeRequestEntity,
    user: UserEntity,
  ) {
    approveChangeRequest(changeRequest, user)

    cas1ChangeRequestDomainEventService.plannedTransferRequestAccepted(changeRequest)
  }

  @Transactional
  fun rejectChangeRequest(
    placementRequestId: UUID,
    changeRequestId: UUID,
    cas1RejectChangeRequest: Cas1RejectChangeRequest,
  ): CasResult<Unit> = validatedCasResult {
    lockableCas1ChangeRequestEntityRepository.acquirePessimisticLock(changeRequestId)

    val changeRequestWithLock = findChangeRequest(changeRequestId)!!

    if (changeRequestWithLock.placementRequest.id != placementRequestId) {
      return GeneralValidationError("The change request does not belong to the specified placement request")
    }

    if (changeRequestWithLock.decision != null) {
      return if (changeRequestWithLock.decision == ChangeRequestDecision.REJECTED) {
        Success(Unit)
      } else {
        GeneralValidationError("A decision has already been made for the change request")
      }
    }

    val changeRequestRejectReason = cas1ChangeRequestRejectionReasonRepository.findByIdAndArchivedIsFalse(cas1RejectChangeRequest.rejectionReasonId)
      ?: return GeneralValidationError("The change request reject reason not found")

    changeRequestWithLock.decision = ChangeRequestDecision.REJECTED
    changeRequestWithLock.rejectionReason = changeRequestRejectReason
    changeRequestWithLock.resolve()
    cas1ChangeRequestRepository.save(changeRequestWithLock)

    when (changeRequestWithLock.type) {
      ChangeRequestType.PLACEMENT_APPEAL -> {
        cas1ChangeRequestEmailService.placementAppealRejected(changeRequestWithLock)
        cas1ChangeRequestDomainEventService.placementAppealRejected(
          changeRequestWithLock,
          userService.getUserForRequest(),
        )
      }
      ChangeRequestType.PLACEMENT_EXTENSION -> Unit
      ChangeRequestType.PLANNED_TRANSFER -> {
        cas1ChangeRequestDomainEventService.plannedTransferRequestRejected(
          changeRequestWithLock,
          userService.getUserForRequest(),
        )
      }
    }

    return Success(Unit)
  }

  fun getChangeRequest(
    placementRequestId: UUID,
    changeRequestId: UUID,
  ): CasResult<Cas1ChangeRequestEntity> {
    val changeRequest = findChangeRequest(changeRequestId) ?: return CasResult.NotFound("Change Request", changeRequestId.toString())

    if (changeRequest.placementRequest.id != placementRequestId) {
      return CasResult.GeneralValidationError("The change request does not belong to the specified placement request")
    }

    return Success(changeRequest)
  }

  @SuppressWarnings("MaxLineLength")
  private fun resolveAllChangeRequestsForSpaceBookingAndType(spaceBooking: Cas1SpaceBookingEntity, changeRequestTypes: List<ChangeRequestType>) = cas1ChangeRequestRepository.findAllBySpaceBookingAndResolvedIsFalseAndTypeIn(spaceBooking, changeRequestTypes).forEach { it.resolve() }

  fun spaceBookingWithdrawn(spaceBooking: Cas1SpaceBookingEntity) = resolveAllChangeRequestsForSpaceBookingAndType(spaceBooking, ChangeRequestType.entries.toList())

  fun spaceBookingHasArrival(spaceBooking: Cas1SpaceBookingEntity) = resolveAllChangeRequestsForSpaceBookingAndType(spaceBooking, listOf(ChangeRequestType.PLACEMENT_APPEAL))

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
