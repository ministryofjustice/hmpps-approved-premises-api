package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
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
  @Lazy private val placementRequestService: PlacementRequestService,
  @Lazy private val bookingService: BookingService,
  @Lazy private val placementApplicationService: PlacementApplicationService,
  @Lazy private val applicationService: ApplicationService,
  @Lazy private val cas1SpaceBookingService: Cas1SpaceBookingService,
) {
  fun treeForApp(application: ApprovedPremisesApplicationEntity, user: UserEntity): WithdrawableTree {
    val children = mutableListOf<WithdrawableTreeNode>()

    placementRequestService.getPlacementRequestForInitialApplicationDates(application.id).forEach {
      children.add(treeForPlacementReq(it, user).rootNode)
    }

    placementApplicationService.getAllSubmittedNonReallocatedApplications(application.id).forEach {
      children.add(treeForPlacementApp(it, user).rootNode)
    }

    bookingService.getAllAdhocOrUnknownForApplication(application).forEach {
      children.add(treeForBooking(it, user).rootNode)
    }

    return WithdrawableTree(
      rootNode = WithdrawableTreeNode(
        applicationId = application.id,
        entityType = WithdrawableEntityType.Application,
        entityId = application.id,
        status = applicationService.getWithdrawableState(application, user),
        dates = emptyList(),
        children = children,
      ),
    )
  }

  fun treeForPlacementApp(placementApplication: PlacementApplicationEntity, user: UserEntity): WithdrawableTree {
    val children = placementApplication.placementRequests.map { treeForPlacementReq(it, user).rootNode }

    return WithdrawableTree(
      WithdrawableTreeNode(
        applicationId = placementApplication.application.id,
        entityType = WithdrawableEntityType.PlacementApplication,
        entityId = placementApplication.id,
        status = placementApplicationService.getWithdrawableState(placementApplication, user),
        dates = placementApplication.placementDates.map { WithdrawableDatePeriod(it.expectedArrival, it.expectedDeparture()) },
        children = children,
      ),
    )
  }

  fun treeForPlacementReq(placementRequest: PlacementRequestEntity, user: UserEntity): WithdrawableTree {
    /*
     * Some legacy adhoc bookings were incorrectly linked to placement requests,
     * and in some cases these placement requests had completely different dates
     * from the bookings. Until these relationships are removed, we should exclude
     * this relationship for any adhoc booking as to avoid unexpected withdrawal
     * cascading.
     *
     * If adhoc is null, we treat it as 'potentially adhoc' and exclude it.
     */
    val booking = placementRequest.booking
    val bookingChildren = if (booking != null && booking.adhoc == false) {
      listOf(treeForBooking(booking, user).rootNode)
    } else {
      emptyList()
    }

    val spaceBookingChildren = placementRequest.spaceBookings.map { treeForSpaceBooking(it, user).rootNode }

    return WithdrawableTree(
      WithdrawableTreeNode(
        applicationId = placementRequest.application.id,
        entityType = WithdrawableEntityType.PlacementRequest,
        entityId = placementRequest.id,
        status = placementRequestService.getWithdrawableState(placementRequest, user),
        dates = listOf(WithdrawableDatePeriod(placementRequest.expectedArrival, placementRequest.expectedDeparture())),
        children = bookingChildren + spaceBookingChildren,
      ),
    )
  }

  fun treeForBooking(booking: BookingEntity, user: UserEntity): WithdrawableTree = WithdrawableTree(
    WithdrawableTreeNode(
      applicationId = booking.application?.id,
      entityType = WithdrawableEntityType.Booking,
      entityId = booking.id,
      status = bookingService.getWithdrawableState(booking, user),
      dates = listOf(WithdrawableDatePeriod(booking.arrivalDate, booking.departureDate)),
      children = emptyList(),
    ),
  )

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

    if (blockedReasons.contains(BlockingReason.ArrivalRecordedInDelius)) {
      notes.add("1 or more placements cannot be withdrawn as they have an arrival recorded in Delius")
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
      "$entityType($abbreviatedId), " +
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
