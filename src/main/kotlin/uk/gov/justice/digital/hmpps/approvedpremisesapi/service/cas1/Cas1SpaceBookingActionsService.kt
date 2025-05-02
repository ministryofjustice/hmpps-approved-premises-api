package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.ActionOutcome.Available
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.ActionOutcome.Unavailable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.SpaceBookingAction.APPEAL_CREATE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.SpaceBookingAction.TRANSFER_CREATE

@Service
class Cas1SpaceBookingActionsService(
  val userAccessService: UserAccessService,
) {
  fun determineActions(spaceBooking: Cas1SpaceBookingEntity): ActionsResult {
    val outcomes = listOf(
      appealCreate(spaceBooking),
      transferCreate(spaceBooking),
    )

    return ActionsResult(outcomes)
  }

  private fun appealCreate(spaceBooking: Cas1SpaceBookingEntity): ActionOutcome {
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
    } else {
      Available(APPEAL_CREATE)
    }
  }

  private fun transferCreate(spaceBooking: Cas1SpaceBookingEntity): ActionOutcome {
    val requiredPermission = UserPermission.CAS1_TRANSFER_CREATE
    fun unavailable(reason: String) = Unavailable(TRANSFER_CREATE, reason)

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
    } else {
      Available(TRANSFER_CREATE)
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
  TRANSFER_CREATE(Cas1SpaceBookingAction.APPEAL_CREATE),
}
