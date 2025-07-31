package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.ActionOutcome.Available
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.ActionOutcome.Unavailable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.SpaceBookingAction.APPEAL_CREATE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.SpaceBookingAction.EMERGENCY_TRANSFER_CREATE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.SpaceBookingAction.PLANNED_TRANSFER_REQUEST
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.SpaceBookingAction.SHORTEN

@Service
class Cas1SpaceBookingActionsService(
  val userAccessService: Cas1UserAccessService,
  val spaceBookingRepository: Cas1SpaceBookingRepository,
  val changeRequestRepository: Cas1ChangeRequestRepository,
) {
  fun determineActions(spaceBooking: Cas1SpaceBookingEntity): ActionsResult {
    val openChangeRequests = changeRequestRepository.findAllBySpaceBookingAndResolvedIsFalse(spaceBooking)

    val outcomes = listOf(
      appealCreate(spaceBooking, openChangeRequests),
      plannedTransferRequest(spaceBooking, openChangeRequests),
      emergencyTransferCreate(spaceBooking),
      shortenBooking(spaceBooking),
    )

    return ActionsResult(outcomes)
  }

  private fun appealCreate(
    spaceBooking: Cas1SpaceBookingEntity,
    openChangeRequests: List<Cas1ChangeRequestEntity>,
  ): ActionOutcome {
    val requiredPermission = UserPermission.CAS1_PLACEMENT_APPEAL_CREATE
    fun unavailable(reason: String) = Unavailable(APPEAL_CREATE, reason)

    return if (!userAccessService.currentUserHasPermission(requiredPermission)) {
      unavailable("User must have permission '$requiredPermission'")
    } else if (spaceBooking.hasArrival()) {
      unavailable("Space booking has been marked as arrived")
    } else if (spaceBooking.hasNonArrival()) {
      unavailable("Space booking has been marked as non arrived")
    } else if (spaceBooking.isCancelled()) {
      unavailable("Space booking has been cancelled")
    } else if (openChangeRequests.any { it.type == ChangeRequestType.PLACEMENT_APPEAL }) {
      unavailable("There is an existing open change request of this type")
    } else {
      Available(APPEAL_CREATE)
    }
  }

  private fun emergencyTransferCreate(spaceBooking: Cas1SpaceBookingEntity) = commonTransferChecks(spaceBooking, EMERGENCY_TRANSFER_CREATE)

  private fun shortenBooking(spaceBooking: Cas1SpaceBookingEntity): ActionOutcome {
    val requiredPermission = UserPermission.CAS1_SPACE_BOOKING_SHORTEN
    fun unavailable(reason: String) = Unavailable(SHORTEN, reason)

    return if (!userAccessService.currentUserHasPermission(requiredPermission)) {
      unavailable("User must have permission '$requiredPermission'")
    } else if (spaceBooking.hasNonArrival()) {
      unavailable("Space booking has been marked as non arrived")
    } else if (spaceBooking.hasDeparted()) {
      unavailable("Space booking has been marked as departed")
    } else if (spaceBooking.isCancelled()) {
      unavailable("Space booking has been cancelled")
    } else if (spaceBookingRepository.hasNonCancelledTransfer(spaceBooking.id)) {
      unavailable("Space booking has already been transferred")
    } else {
      Available(SHORTEN)
    }
  }

  private fun plannedTransferRequest(
    spaceBooking: Cas1SpaceBookingEntity,
    openChangeRequests: List<Cas1ChangeRequestEntity>,
  ): ActionOutcome {
    if (openChangeRequests.any { it.type == ChangeRequestType.PLANNED_TRANSFER }) {
      return Unavailable(PLANNED_TRANSFER_REQUEST, "There is an existing open change request of this type")
    }

    return commonTransferChecks(spaceBooking, PLANNED_TRANSFER_REQUEST)
  }

  private fun commonTransferChecks(spaceBooking: Cas1SpaceBookingEntity, action: SpaceBookingAction): ActionOutcome {
    val requiredPermission = UserPermission.CAS1_TRANSFER_CREATE
    fun unavailable(reason: String) = Unavailable(action, reason)
    return if (!userAccessService.currentUserHasPermission(requiredPermission)) {
      unavailable("User must have permission '$requiredPermission'")
    } else if (!spaceBooking.hasArrival()) {
      unavailable("Space booking has not been marked as arrived")
    } else if (spaceBooking.hasNonArrival()) {
      unavailable("Space booking has been marked as non arrived")
    } else if (spaceBooking.hasDeparted()) {
      unavailable("Space booking has been marked as departed")
    } else if (spaceBooking.isCancelled()) {
      unavailable("Space booking has been cancelled")
    } else if (spaceBookingRepository.hasNonCancelledTransfer(spaceBooking.id)) {
      unavailable("Space booking has already been transferred")
    } else {
      Available(action)
    }
  }
}

class ActionsResult(
  private val outcomes: List<ActionOutcome>,
) {
  companion object {
    fun forAllowedAction(action: SpaceBookingAction) = ActionsResult(listOf(Available(action)))
    fun forUnavailableAction(action: SpaceBookingAction, message: String) = ActionsResult(listOf(Unavailable(action, message)))
  }

  fun available() = outcomes.filterIsInstance<Available>().map { it.action }

  fun unavailable() = outcomes.filterIsInstance<Unavailable>()

  fun unavailableReason(action: SpaceBookingAction) = unavailable().firstOrNull { it.action == action }?.reason
}

sealed interface ActionOutcome {
  val action: SpaceBookingAction

  data class Available(override val action: SpaceBookingAction) : ActionOutcome
  data class Unavailable(override val action: SpaceBookingAction, val reason: String) : ActionOutcome
}

enum class SpaceBookingAction(
  val apiType: Cas1SpaceBookingAction,
) {
  APPEAL_CREATE(Cas1SpaceBookingAction.APPEAL_CREATE),
  PLANNED_TRANSFER_REQUEST(Cas1SpaceBookingAction.PLANNED_TRANSFER_REQUEST),
  EMERGENCY_TRANSFER_CREATE(Cas1SpaceBookingAction.EMERGENCY_TRANSFER_CREATE),
  SHORTEN(Cas1SpaceBookingAction.SHORTEN),
}
