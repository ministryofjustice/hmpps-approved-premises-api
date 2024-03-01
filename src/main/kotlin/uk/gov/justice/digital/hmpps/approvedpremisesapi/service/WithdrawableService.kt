package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.extractMessage
import java.time.LocalDate
import java.util.UUID

@Service
class WithdrawableService(
  // Added Lazy annotations here to prevent circular dependency issues
  @Lazy private val placementRequestService: PlacementRequestService,
  @Lazy private val bookingService: BookingService,
  private val bookingRepository: BookingRepository,
  @Lazy private val placementApplicationService: PlacementApplicationService,
  private val withdrawableTreeBuilder: WithdrawableTreeBuilder,
) {
  var log: Logger = LoggerFactory.getLogger(this::class.java)

  fun allWithdrawables(
    application: ApprovedPremisesApplicationEntity,
    user: UserEntity,
  ): Set<WithdrawableEntity> {
    val rootNode = withdrawableTreeBuilder.treeForApp(application, user)

    if (log.isDebugEnabled) {
      log.debug("Withdrawables tree is $rootNode")
    }

    return rootNode.flatten()
      .filter { it.status.withdrawable }
      .filter { it.status.userMayDirectlyWithdraw }
      .map {
        WithdrawableEntity(
          it.entityType,
          it.entityId,
          it.dates,
        )
      }
      .toSet()
  }

  fun withdrawPlacementApplicationDescendants(
    placementApplication: PlacementApplicationEntity,
    context: WithdrawalContext,
  ) {
    withdrawDescendantsOfRootNode(
      withdrawableTreeBuilder.treeForPlacementApp(placementApplication, context.triggeringUser!!),
      context,
    )
  }

  fun withdrawPlacementRequestDescendants(
    placementRequest: PlacementRequestEntity,
    context: WithdrawalContext,
  ) {
    withdrawDescendantsOfRootNode(
      withdrawableTreeBuilder.treeForPlacementReq(placementRequest, context.triggeringUser!!),
      context,
    )
  }

  private fun withdrawDescendantsOfRootNode(
    rootNode: WithdrawableTreeNode,
    withdrawalContext: WithdrawalContext,
  ) {
    if (log.isDebugEnabled) {
      log.debug("Tree for withdrawing descendants is $rootNode")
    }

    rootNode.collectDescendants().forEach {
      if (it.status.withdrawable) {
        withdraw(it, withdrawalContext)
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
          is AuthorisableActionResult.Success -> Unit
          else -> log.error(
            "Failed to automatically withdraw PlacementRequest ${node.entityId} " +
              "when withdrawing ${context.triggeringEntityType} ${context.triggeringEntityId} " +
              "with error type ${result::class}",
          )
        }
      }
      WithdrawableEntityType.PlacementApplication -> Unit
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
          is ValidatableActionResult.Success -> Unit
          else -> log.error(
            "Failed to automatically withdraw Booking ${booking.id} " +
              "when withdrawing ${context.triggeringEntityType} ${context.triggeringEntityId} " +
              "with message ${extractMessage(bookingCancellationResult)}",
          )
        }
      }
    }
  }

  fun withdrawAllForApplication(
    application: ApprovedPremisesApplicationEntity,
    user: UserEntity,
  ) {
    val withdrawalContext = WithdrawalContext(
      user,
      WithdrawableEntityType.Application,
      application.id,
    )

    val placementRequests = application.placementRequests
    placementRequests.forEach { placementRequest ->
      if (placementRequest.isInWithdrawableState()) {
        val result = placementRequestService.withdrawPlacementRequest(
          placementRequest.id,
          userProvidedReason = null,
          withdrawalContext,
        )

        when (result) {
          is AuthorisableActionResult.Success -> Unit
          else -> log.error(
            "Failed to automatically withdraw placement request ${placementRequest.id} " +
              "when withdrawing application ${application.id} " +
              "with error type ${result::class}",
          )
        }
      }
    }

    val placementApplications = placementApplicationService.getAllPlacementApplicationEntitiesForApplicationId(
      application.id,
    )
    placementApplications.forEach { placementApplication ->
      if (placementApplication.isInWithdrawableState()) {
        val result = placementApplicationService.withdrawPlacementApplication(
          id = placementApplication.id,
          userProvidedReason = null,
          withdrawalContext,
        )

        when (result) {
          is AuthorisableActionResult.Success -> Unit
          else -> log.error(
            "Failed to automatically withdraw placement application ${placementApplication.id} " +
              "when withdrawing application ${application.id} " +
              "with error type ${result::class}",
          )
        }
      }
    }

    val now = LocalDate.now()
    val bookings = bookingService.getAllForApplication(application)
    bookings.forEach { booking ->
      if (booking.isInCancellableStateCas1()) {
        val bookingCancellationResult = bookingService.createCas1Cancellation(
          booking = booking,
          cancelledAt = now,
          userProvidedReason = null,
          notes = "Automatically withdrawn as placement request was withdrawn",
          withdrawalContext = withdrawalContext,
        )

        when (bookingCancellationResult) {
          is ValidatableActionResult.Success -> Unit
          else -> log.error(
            "Failed to automatically withdraw booking ${booking.id} " +
              "when withdrawing application ${application.id} " +
              "with message ${extractMessage(bookingCancellationResult)}",
          )
        }
      }
    }
  }
}

data class WithdrawalContext(
  /**
   * Ideally this would not be nullable, but we have seed jobs that cancel bookings
   * that don't have an associated user. This would be an issue if seed jobs cancel
   * any elements that can cascade withdrawals, as we assume the user is provided
   * in these cases
   */
  val triggeringUser: UserEntity?,
  val triggeringEntityType: WithdrawableEntityType,
  val triggeringEntityId: UUID,
)

data class WithdrawableEntity(
  val type: WithdrawableEntityType,
  val id: UUID,
  val dates: List<WithdrawableDatePeriod>,
)

enum class WithdrawableEntityType(val label: String) {
  Application("Application"),

  /**
   * See [PlacementRequestEntity.isForApplicationsArrivalDate] for why we label this as Request for Placement
   */
  PlacementRequest("Request for Placement"),
  PlacementApplication("Request for Placement"),
  Booking("Placement"),
}

data class WithdrawableState(
  /**
   * If the entity is in a withdrawable state.
   * This doesn't consider if the given user can directly withdraw it.
   */
  val withdrawable: Boolean,
  /**
   * If the user can directly withdraw this entity (as apposed to cascading).
   * This doesn't consider if the entity itself is in a withdrawable state.
   */
  val userMayDirectlyWithdraw: Boolean,
)

data class WithdrawableDatePeriod(
  val startDate: LocalDate,
  val endDate: LocalDate,
)
