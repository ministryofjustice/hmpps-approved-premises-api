package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractMessageFromCasResult
import java.time.LocalDate

@Service
class Cas1WithdrawableTreeOperations(
  private val placementRequestService: PlacementRequestService,
  private val bookingService: BookingService,
  private val bookingRepository: BookingRepository,
  private val placementApplicationService: PlacementApplicationService,
) {
  var log: Logger = LoggerFactory.getLogger(this::class.java)

  fun withdrawDescendantsOfRootNode(
    rootNode: WithdrawableTreeNode,
    withdrawalContext: WithdrawalContext,
  ) {
    rootNode.collectDescendants().forEach { descendant ->
      if (descendant.status.withdrawable && !descendant.isBlocked()) {
        withdraw(descendant, withdrawalContext)
      }
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
        val result = placementApplicationService.withdrawPlacementApplication(
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
      WithdrawableEntityType.Booking -> {
        val booking = bookingRepository.findByIdOrNull(node.entityId)!!

        val bookingCancellationResult = bookingService.createCas1Cancellation(
          booking = booking,
          cancelledAt = LocalDate.now(),
          userProvidedReason = null,
          notes = "Automatically withdrawn as ${context.triggeringEntityType.label} was withdrawn",
          withdrawalContext = context,
        )

        when (bookingCancellationResult) {
          is CasResult.Success -> Unit
          else -> log.error(
            "Failed to automatically withdraw Booking ${booking.id} " +
              "when withdrawing ${context.triggeringEntityType} ${context.triggeringEntityId} " +
              "with message ${extractMessageFromCasResult(bookingCancellationResult)}",
          )
        }
      }
    }
  }
}
