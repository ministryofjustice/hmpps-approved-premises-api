package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import java.util.UUID

/**
 * The tree can have the following edges:
 *
 * > Application
 * ---> Request for Placement
 * ------> Match Request
 * ---------> Placement (Booking or Space Booking)
 * ---> Match Request (For initial application dates)
 * ------> Placement (Booking or Space Booking)
 * ---> Placement (For adhoc placements - Booking only)
 *
 * Note that whilst assessments are automatically withdrawn when
 * an application is withdrawn, that is not managed by the tree
 **/
@Component
class Cas1WithdrawableTreeBuilder(
  @Lazy private val placementRequestService: Cas1PlacementRequestService,
  @Lazy private val cas1PlacementApplicationService: Cas1PlacementApplicationService,
  @Lazy private val cas1ApplicationService: Cas1ApplicationService,
  @Lazy private val cas1SpaceBookingService: Cas1SpaceBookingService,
) {
  fun treeForApp(application: ApprovedPremisesApplicationEntity, user: UserEntity): WithdrawableTree {
    val children = mutableListOf<WithdrawableTreeNode>()

    cas1PlacementApplicationService.getAllSubmittedNonReallocatedApplications(application.id).forEach {
      children.add(treeForPlacementApp(it, user).rootNode)
    }

    return WithdrawableTree(
      rootNode = WithdrawableTreeNode(
        applicationId = application.id,
        entityType = WithdrawableEntityType.Application,
        entityId = application.id,
        status = cas1ApplicationService.getWithdrawableState(application, user),
        dates = emptyList(),
        children = children,
      ),
    )
  }

  fun treeForPlacementApp(placementApplication: PlacementApplicationEntity, user: UserEntity): WithdrawableTree {
    val children = listOfNotNull(placementApplication.placementRequest?.let { treeForPlacementReq(it, user).rootNode })

    return WithdrawableTree(
      WithdrawableTreeNode(
        applicationId = placementApplication.application.id,
        entityType = WithdrawableEntityType.PlacementApplication,
        entityId = placementApplication.id,
        status = cas1PlacementApplicationService.getWithdrawableState(placementApplication, user),
        dates = listOfNotNull(
          placementApplication.placementDates()?.let {
            WithdrawableDatePeriod(
              startDate = it.expectedArrival,
              endDate = it.expectedDeparture(),
            )
          },
        ),
        children = children,
        additionalInfo = if (placementApplication.automatic) {
          " automatic"
        } else {
          ""
        },
      ),
    )
  }

  fun treeForPlacementReq(placementRequest: PlacementRequestEntity, user: UserEntity): WithdrawableTree {
    val spaceBookingChildren = placementRequest.spaceBookings.map { treeForSpaceBooking(it, user).rootNode }

    return WithdrawableTree(
      WithdrawableTreeNode(
        applicationId = placementRequest.application.id,
        entityType = WithdrawableEntityType.PlacementRequest,
        entityId = placementRequest.id,
        status = placementRequestService.getWithdrawableState(placementRequest, user),
        dates = listOf(WithdrawableDatePeriod(placementRequest.expectedArrival, placementRequest.expectedDeparture())),
        children = spaceBookingChildren,
      ),
    )
  }

  fun treeForSpaceBooking(spaceBooking: Cas1SpaceBookingEntity, user: UserEntity): WithdrawableTree = WithdrawableTree(
    WithdrawableTreeNode(
      applicationId = spaceBooking.application?.id,
      entityType = WithdrawableEntityType.SpaceBooking,
      entityId = spaceBooking.id,
      status = cas1SpaceBookingService.getWithdrawableState(spaceBooking, user),
      dates = listOf(WithdrawableDatePeriod(spaceBooking.canonicalArrivalDate, spaceBooking.canonicalDepartureDate)),
      children = emptyList(),
    ),
  )
}

data class WithdrawableTree(
  val rootNode: WithdrawableTreeNode,
) {
  fun notes(): List<String> {
    val blockedReasons = rootNode.blockedReasons()
    val notes = mutableListOf<String>()

    if (blockedReasons.contains(BlockingReason.ArrivalRecordedInCas1)) {
      notes.add("1 or more placements cannot be withdrawn as they have an arrival")
    }

    if (blockedReasons.contains(BlockingReason.NonArrivalRecordedInCas1)) {
      notes.add("1 or more placements cannot be withdrawn as they have a non-arrival")
    }

    return notes
  }

  fun render(includeIds: Boolean = true): String = "${rootNode.render(0,includeIds)}\nNotes: ${notes()}\n"

  override fun toString(): String = "\n\n${render()}\n"
}

data class WithdrawableTreeNode(
  val applicationId: UUID?,
  val entityType: WithdrawableEntityType,
  val entityId: UUID,
  val status: WithdrawableState,
  val dates: List<WithdrawableDatePeriod> = emptyList(),
  val children: List<WithdrawableTreeNode> = emptyList(),
  val additionalInfo: String = "",
) {
  fun flatten(): List<WithdrawableTreeNode> = listOf(this) + collectDescendants()

  fun collectDescendants(): List<WithdrawableTreeNode> {
    val result = mutableListOf<WithdrawableTreeNode>()
    children.forEach {
      result.add(it)
      result.addAll(it.collectDescendants())
    }
    return result
  }

  private fun isBlockAncestorWithdrawals() = status.blockingReason != null

  fun isBlocked(): Boolean = isBlockAncestorWithdrawals() || children.any { it.isBlocked() }

  fun blockedReasons(): Set<BlockingReason> = setOfNotNull(status.blockingReason) + children.flatMap { it.blockedReasons() }.toSet()

  override fun toString(): String = "\n\n${render(0)}\n"

  fun simpleDescription(): String = "$entityType $entityId (application $applicationId)"

  @SuppressWarnings("MagicNumber")
  fun render(depth: Int, includeIds: Boolean = true): String {
    val padding = if (depth > 0) {
      "-".repeat(3 * depth) + "> "
    } else {
      ""
    }
    val abbreviatedId = if (includeIds) {
      entityId.toString().substring(0, 3)
    } else {
      ""
    }

    val description = "" +
      "$entityType($abbreviatedId)$additionalInfo, " +
      "withdrawn:${yesOrNo(status.withdrawn)}, " +
      "withdrawable:${yesOrNo(status.withdrawable)}, " +
      "mayDirectlyWithdraw:${yesOrNo(status.userMayDirectlyWithdraw)}" +
      blockingDescription()

    return "$padding$description\n" +
      children.joinToString(separator = "") { it.render(depth + 1, includeIds) }
  }

  private fun yesOrNo(value: Boolean) = if (value) {
    "Y"
  } else {
    "N"
  }

  private fun blockingDescription() = if (isBlockAncestorWithdrawals()) {
    ", BLOCKING - ${status.blockingReason}"
  } else if (isBlocked()) {
    ", BLOCKED"
  } else {
    ""
  }
}
