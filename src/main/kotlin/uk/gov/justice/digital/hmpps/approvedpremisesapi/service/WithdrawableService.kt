package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawableType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Withdrawables
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
  @Lazy private val placementApplicationService: PlacementApplicationService,
  @Lazy private val applicationService: ApplicationService,
) {
  var log: Logger = LoggerFactory.getLogger(this::class.java)

  fun allWithdrawables(
    application: ApprovedPremisesApplicationEntity,
    user: UserEntity,
  ): Withdrawables {

    val placementRequests = placementRequestService.getWithdrawablePlacementRequestsForUser(user, application)
    val bookings = bookingService.getCancelleableCas1BookingsForUser(user, application)
    val placementApplications = placementApplicationService.getWithdrawablePlacementApplicationsForUser(user, application)

    return Withdrawables(
      applicationService.isWithdrawableForUser(user, application),
      placementRequests = placementRequests,
      bookings = bookings,
      placementApplications = placementApplications,
    )
  }

  fun allWithdrawablesNew(
    application: ApprovedPremisesApplicationEntity,
    user: UserEntity,
  ): List<Withdrawable> {

    val rootNode = treeForApp(application, user)

    log.info("Tree is $rootNode")

    val allNodes = listOf(rootNode) + rootNode.collectDescendants()

    return allNodes
      .filter { it.status.withdrawable }
      .filter { it.status.userMayDirectlyWithdraw }
      .map {
        Withdrawable(
          it.id,
          toType(it.type),
          it.dates,
        )
      }
  }

  fun toType(entityType: WithdrawableEntityType): WithdrawableType = when(entityType) {
    WithdrawableEntityType.Application -> WithdrawableType.application
    WithdrawableEntityType.PlacementRequest -> WithdrawableType.placementRequest
    WithdrawableEntityType.PlacementApplication -> WithdrawableType.placementApplication
    WithdrawableEntityType.Booking -> WithdrawableType.booking
  }

  fun withdrawAllForApplication(
    application: ApprovedPremisesApplicationEntity,
    user: UserEntity,
  ) {
    val placementRequests = application.placementRequests
    placementRequests.forEach { placementRequest ->
      if (placementRequest.isInWithdrawableState()) {
        val result = placementRequestService.withdrawPlacementRequest(
          placementRequest.id,
          userProvidedReason = null,
          WithdrawalContext(
            user,
            WithdrawableEntityType.Application,
          ),
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
          withdrawalContext = WithdrawalContext(
            user,
            WithdrawableEntityType.Application,
          ),
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
  }

  fun cascadeWithdrawalApplication(application: ApprovedPremisesApplicationEntity,
                                   // TOOD: this is on context but nullable?
                                   user: UserEntity,
                                   context: WithdrawalContext) {
    withdrawDescendants(
      treeForApp(application, user),
      context
    )
  }

  fun cascadePlacementApplication(placementApplication: PlacementApplicationEntity,
                                  context: WithdrawalContext) {
    withdrawDescendants(
      // TODO: need to figure out this nullable context issue. Maybe handle it being null
      // (don't populate withdrawable for user?)
      treeForPlacementApp(placementApplication,context.triggeringUser!!),
      context
    )
  }

  fun cascadePlacementRequest(placementRequest: PlacementRequestEntity,
                                  context: WithdrawalContext) {
    withdrawDescendants(
      // TODO: need to figure out this nullable context issue. Maybe handle it being null
      // (don't populate withdrawable for user?)
      treeForPlacementReq(placementRequest,context.triggeringUser!!),
      context
    )
  }

  private fun withdrawDescendants(rootNode: WithdrawalTreeNode,
                                  withdrawalContext: WithdrawalContext) {
    log.info("Tree is $rootNode")

    rootNode.collectDescendants().forEach {
      if(it.status.withdrawable) {
        withdraw(it, withdrawalContext)
      }
    }
  }

  private fun withdraw(node: WithdrawalTreeNode,
                       context: WithdrawalContext) {

      when(node.type) {
        WithdrawableEntityType.Application -> Unit
        WithdrawableEntityType.PlacementRequest -> {

          val result = placementRequestService.withdrawPlacementRequest(
            placementRequestId = node.id,
            userProvidedReason = null,
            context,
          )

          when (result) {
            is AuthorisableActionResult.Success -> Unit
            else -> log.error(
              "Failed to automatically withdraw placement application ${node.id} " +
                // TODO: for id add trigger entity id into context
                "when withdrawing ${context.triggeringEntityType} with id TODO " +
                "with error type ${result::class}",
            )
          }

        }
        WithdrawableEntityType.PlacementApplication -> {

          val result = placementApplicationService.withdrawPlacementApplication(
            id = node.id,
            userProvidedReason = null,
            context,
          )

          when (result) {
            is AuthorisableActionResult.Success -> Unit
            else -> log.error(
              "Failed to automatically withdraw placement application ${node.id} " +
                // TODO: for id add trigger entity id into context
                "when withdrawing ${context.triggeringEntityType} with id TODO " +
                "with error type ${result::class}",
            )
          }

        }
        WithdrawableEntityType.Booking -> {

          // TODO: hacky and gets person for no reason
          val booking = (bookingService.getBooking(node.id) as AuthorisableActionResult.Success).entity.booking

          val bookingCancellationResult = bookingService.createCas1Cancellation(
            booking = booking,
            cancelledAt = LocalDate.now(),
            userProvidedReason = null,
            // TODO: this isn't correct, could be adhoc booking from app?
            // maybe could put triggering node in?
            notes = "Automatically withdrawn as placement request was withdrawn",
            withdrawalContext = context,
          )

          when (bookingCancellationResult) {
            is ValidatableActionResult.Success -> Unit
            else -> log.error(
              "Failed to automatically withdraw booking ${booking.id} " +
                // TODO: for id add trigger entity id into context
                "when withdrawing ${context.triggeringEntityType} with id TODO " +
                "with message ${extractMessage(bookingCancellationResult)}",
            )
          }


        }
      }

  }

  /*
  A
   -> PA
      -> PR
          -> B
   -> PR
      -> B
   -> B
   */

  fun treeForApp(application: ApprovedPremisesApplicationEntity, user: UserEntity) : WithdrawalTreeNode {
    // TODO: adhoc booking - once merged in other branch

    var children = mutableListOf<WithdrawalTreeNode>()

    val placementRequestForInitialApp = placementRequestService.getPlacementRequestForInitialApplicationDates(application.id)

    placementRequestForInitialApp?.let {
      children.add(treeForPlacementReq(it, user))
    }

    val placementApplications = placementApplicationService.getAllPlacementApplicationEntitiesForApplicationId(application.id)

    children.addAll(
      placementApplications.map {
        treeForPlacementApp(it, user)
      }
    )

    return WithdrawalTreeNode(
      type = WithdrawableEntityType.Application,
      id = application.id,
      dates = emptyList(),
      status = applicationService.getWithdrawableState(application, user),
      children = children
    )
  }

  fun treeForPlacementApp(placementApplication: PlacementApplicationEntity, user: UserEntity): WithdrawalTreeNode {
    val children = placementApplication.placementRequests.map {
      treeForPlacementReq(it, user)
    }

    return WithdrawalTreeNode(
      type = WithdrawableEntityType.PlacementApplication,
      id = placementApplication.id,
      dates = placementApplication.placementDates.map { DatePeriod(it.expectedArrival,it.expectedDeparture()) },
      status = placementApplicationService.getWithdrawableState(placementApplication, user),
      children = children
    )
  }

  fun treeForPlacementReq(placementRequest: PlacementRequestEntity, user: UserEntity): WithdrawalTreeNode {
    val children = listOfNotNull(
      placementRequest.booking?.let {
        treeForBooking(it, user)
      }
    )

    return WithdrawalTreeNode(
      type = WithdrawableEntityType.PlacementRequest,
      id = placementRequest.id,
      dates = listOf(DatePeriod(placementRequest.expectedArrival,placementRequest.expectedDeparture())),
      status = placementRequestService.getWithdrawableState(placementRequest, user),
      children = children
    )
  }

  fun treeForBooking(booking: BookingEntity, user: UserEntity): WithdrawalTreeNode {
    return WithdrawalTreeNode(
      type = WithdrawableEntityType.Booking,
      id = booking.id,
      dates = listOf(DatePeriod(booking.arrivalDate,booking.departureDate)),
      status = bookingService.getWithdrawableState(booking, user),
      children = emptyList()
    )
  }

}

data class WithdrawalContext(
  val triggeringUser: UserEntity?,
  val triggeringEntityType: WithdrawableEntityType,
)

enum class WithdrawableEntityType {
  Application,
  PlacementRequest,
  PlacementApplication,
  Booking,
}

data class WithdrawalTreeNode(
  // TODO: rename entityType?
  val type: WithdrawableEntityType,
  // TODO: rename entityId?
  val id: UUID,
  val dates: List<DatePeriod>,
  val status: WithdrawableState,
  val children: List<WithdrawalTreeNode>,
) {
  fun collectDescendants(): List<WithdrawalTreeNode> {
    val result = mutableListOf<WithdrawalTreeNode>()
    children.forEach {
      result.add(it)
      result.addAll(it.collectDescendants())
    }
    return result
  }

  fun isBlocked(): Boolean =
     status.blocking || children.any { it.isBlocked() }

  override fun toString(): String {
    return "\n\n" + render(0)
  }

  private fun render(depth: Int): String {
    val padding = "".padStart(depth * 3)
    return padding + "Node($type, withdrawable:${status.withdrawable}, blocking:${status.blocking}" +
           if(children.isNotEmpty()) {
             "\n" + children.joinToString(separator = "") { it.render(depth + 1) } + "$padding)\n"
           } else {
             ")\n"
           }
  }
}

data class WithdrawableState (
  val withdrawable: Boolean,
  val userMayDirectlyWithdraw: Boolean,
  val blocking: Boolean = false,
)
