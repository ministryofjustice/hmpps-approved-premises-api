package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableTreeOperations.Constants.MAX_WITHDRAWAL_COUNT
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractMessageFromCasResult
import java.time.LocalDate

@Service
class Cas1WithdrawableTreeOperations(
  private val placementRequestService: Cas1PlacementRequestService,
  private val cas1SpaceBookingService: Cas1SpaceBookingService,
  private val cas1SpaceBookingRepository: Cas1SpaceBookingRepository,
  private val cas1PlacementApplicationService: Cas1PlacementApplicationService,
) {
  var log: Logger = LoggerFactory.getLogger(this::class.java)

  object Constants {
    const val MAX_WITHDRAWAL_COUNT = 100
  }

  fun withdrawDescendantsOfRootNode(
    rootNode: WithdrawableTreeNode,
    withdrawalContext: WithdrawalContext,
  ) {
    val withdrawableDescendants = rootNode
      .collectDescendants()
      .filter { it.status.withdrawable }
      .filter { !it.isBlocked() }

    if (withdrawableDescendants.size > MAX_WITHDRAWAL_COUNT) {
      throw IllegalStateException(
        "Cascade withdrawal for root node ${rootNode.simpleDescription()} will lead to" +
          " an unexpectedly high number of withdrawals (${withdrawableDescendants.size})",
      )
    }

    val withdrawableDescendantsForOtherApps = withdrawableDescendants
      .filter { it.applicationId != rootNode.applicationId }

    if (withdrawableDescendantsForOtherApps.isNotEmpty()) {
      val entityList = withdrawableDescendantsForOtherApps.map { it.simpleDescription() }

      throw IllegalStateException(
        "Cascade withdrawal for root node ${rootNode.simpleDescription()} " +
          "would remove the following nodes belonging to other applications $entityList",
      )
    }

    withdrawableDescendants.forEach {
      withdraw(it, withdrawalContext)
    }
  }

  private fun withdraw(
    node: WithdrawableTreeNode,
    context: WithdrawalContext,
  ) {
    when (node.entityType) {
      WithdrawableEntityType.Application -> Unit
      WithdrawableEntityType.PlacementRequest -> {
        val result = placementRequestService.withdrawPlacementRequest(
          placementRequestId = node.entityId,
          userProvidedReason = null,
          context,
        )

        when (result) {
          is CasResult.Success -> Unit
          else -> log.error(
            "Failed to automatically withdraw PlacementRequest ${node.entityId} " +
              "when withdrawing ${context.triggeringEntityType} ${context.triggeringEntityId} " +
              "with error type ${result::class}",
          )
        }
      }
      WithdrawableEntityType.PlacementApplication -> {
        val result = cas1PlacementApplicationService.withdrawPlacementApplication(
          id = node.entityId,
          userProvidedReason = null,
          context,
        )

        when (result) {
          is CasResult.Success -> Unit
          else -> log.error(
            "Failed to automatically withdraw PlacementApplication ${node.entityId} " +
              "when withdrawing ${context.triggeringEntityType} ${context.triggeringEntityId} " +
              "with error type ${result::class}",
          )
        }
      }
      WithdrawableEntityType.SpaceBooking -> {
        val spaceBooking = cas1SpaceBookingRepository.findByIdOrNull(node.entityId)!!

        val withdrawalResult = cas1SpaceBookingService.withdraw(
          spaceBooking = spaceBooking,
          occurredAt = LocalDate.now(),
          userProvidedReasonId = null,
          userProvidedReasonNotes = null,
          appealChangeRequestId = null,
          withdrawalContext = context,
        )

        when (withdrawalResult) {
          is CasResult.Success -> Unit
          else -> log.error(
            "Failed to automatically withdraw Space Booking ${spaceBooking.id} " +
              "when withdrawing ${context.triggeringEntityType} ${context.triggeringEntityId} " +
              "with message ${extractMessageFromCasResult(withdrawalResult)}",
          )
        }
      }
    }
  }
}
