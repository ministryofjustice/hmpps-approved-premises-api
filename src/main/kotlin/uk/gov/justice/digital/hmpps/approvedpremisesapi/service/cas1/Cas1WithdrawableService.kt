package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import java.time.LocalDate
import java.util.UUID

@Service
class Cas1WithdrawableService(
  private val applicationService: ApplicationService,
  private val placementRequestService: PlacementRequestService,
  private val cas1PlacementApplicationService: Cas1PlacementApplicationService,
  private val bookingService: BookingService,
  private val cas1SpaceBookingService: Cas1SpaceBookingService,
  private val cas1WithdrawableTreeBuilder: Cas1WithdrawableTreeBuilder,
  private val cas1WithdrawableTreeOperations: Cas1WithdrawableTreeOperations,
) {
  var log: Logger = LoggerFactory.getLogger(this::class.java)

  fun allDirectlyWithdrawables(
    application: ApprovedPremisesApplicationEntity,
    user: UserEntity,
  ): WithdrawableEntitiesWithNotes {
    val tree = cas1WithdrawableTreeBuilder.treeForApp(application, user)

    if (log.isDebugEnabled) {
      log.debug("Withdrawables tree for application ${application.id} is $tree")
    }

    return WithdrawableEntitiesWithNotes(
      notes = tree.notes(),
      withdrawables = toDirectlyWithdrawableEntities(tree.rootNode),
    )
  }

  fun isDirectlyWithdrawable(placementRequest: PlacementRequestEntity, user: UserEntity): Boolean {
    val tree = cas1WithdrawableTreeBuilder.treeForPlacementReq(placementRequest, user)

    if (log.isDebugEnabled) {
      log.debug("Withdrawables tree for placement request ${placementRequest.id} is $tree")
    }

    return toDirectlyWithdrawableEntities(tree.rootNode)
      .any { it.type == WithdrawableEntityType.PlacementRequest && it.id == placementRequest.id }
  }

  fun isDirectlyWithdrawable(placementApplication: PlacementApplicationEntity, user: UserEntity): Boolean {
    val tree = cas1WithdrawableTreeBuilder.treeForPlacementApp(placementApplication, user)

    if (log.isDebugEnabled) {
      log.debug("Withdrawables tree for placement app ${placementApplication.id} is $tree")
    }

    return toDirectlyWithdrawableEntities(tree.rootNode)
      .any { it.type == WithdrawableEntityType.PlacementApplication && it.id == placementApplication.id }
  }

  @Transactional
  fun withdrawApplication(
    applicationId: UUID,
    user: UserEntity,
    withdrawalReason: String,
    otherReason: String?,
  ): CasResult<Unit> {
    val application = applicationService.getApplication(applicationId) ?: return CasResult.NotFound(entityType = "Application", applicationId.toString())
    if (application !is ApprovedPremisesApplicationEntity) {
      return CasResult.GeneralValidationError("Only CAS1 Apps are supported")
    }

    val withdrawalContext = WithdrawalContext(
      withdrawalTriggeredBy = WithdrawalTriggeredByUser(user),
      triggeringEntityType = WithdrawableEntityType.Application,
      triggeringEntityId = applicationId,
    )

    return withdraw(
      cas1WithdrawableTreeBuilder.treeForApp(application, user).rootNode,
      withdrawalContext,
    ) {
      applicationService.withdrawApprovedPremisesApplication(applicationId, user, withdrawalReason, otherReason)
    }
  }

  @Transactional
  fun withdrawPlacementRequest(
    placementRequestId: UUID,
    user: UserEntity,
    userProvidedReason: PlacementRequestWithdrawalReason?,
  ): CasResult<PlacementRequestService.PlacementRequestAndCancellations> {
    val placementRequest = placementRequestService.getPlacementRequestOrNull(placementRequestId)
      ?: return CasResult.NotFound(entityType = "PlacementRequest", id = placementRequestId.toString())

    val withdrawalContext = WithdrawalContext(
      withdrawalTriggeredBy = WithdrawalTriggeredByUser(user),
      triggeringEntityType = WithdrawableEntityType.PlacementRequest,
      triggeringEntityId = placementRequestId,
    )

    return withdraw(
      cas1WithdrawableTreeBuilder.treeForPlacementReq(placementRequest, user).rootNode,
      withdrawalContext,
    ) {
      placementRequestService.withdrawPlacementRequest(placementRequestId, userProvidedReason, withdrawalContext)
    }
  }

  @Transactional
  fun withdrawPlacementApplication(
    placementApplicationId: UUID,
    user: UserEntity,
    userProvidedReason: PlacementApplicationWithdrawalReason?,
  ): CasResult<PlacementApplicationEntity> {
    val placementApplication = cas1PlacementApplicationService.getApplicationOrNull(placementApplicationId)
      ?: return CasResult.NotFound(entityType = "PlacementApplication", id = placementApplicationId.toString())

    val withdrawalContext = WithdrawalContext(
      withdrawalTriggeredBy = WithdrawalTriggeredByUser(user),
      triggeringEntityType = WithdrawableEntityType.PlacementApplication,
      triggeringEntityId = placementApplicationId,
    )

    return withdraw(
      cas1WithdrawableTreeBuilder.treeForPlacementApp(placementApplication, user).rootNode,
      withdrawalContext,
    ) {
      cas1PlacementApplicationService.withdrawPlacementApplication(placementApplicationId, userProvidedReason, withdrawalContext)
    }
  }

  @Transactional
  fun withdrawBooking(
    booking: BookingEntity,
    user: UserEntity,
    cancelledAt: LocalDate,
    userProvidedReason: UUID?,
    notes: String?,
    otherReason: String?,
  ): CasResult<CancellationEntity> {
    val withdrawalContext = WithdrawalContext(
      withdrawalTriggeredBy = WithdrawalTriggeredByUser(user),
      triggeringEntityType = WithdrawableEntityType.Booking,
      triggeringEntityId = booking.id,
    )

    return withdraw(
      cas1WithdrawableTreeBuilder.treeForBooking(booking, user).rootNode,
      withdrawalContext,
    ) {
      bookingService.createCas1Cancellation(
        booking,
        cancelledAt,
        userProvidedReason,
        notes,
        otherReason,
        withdrawalContext,
      )
    }
  }

  @Transactional
  fun withdrawSpaceBooking(
    spaceBooking: Cas1SpaceBookingEntity,
    user: UserEntity,
    cancelledAt: LocalDate,
    userProvidedReason: UUID?,
    otherReason: String?,
    appealChangeRequestId: UUID?,
  ): CasResult<Unit> {
    val withdrawalContext = WithdrawalContext(
      withdrawalTriggeredBy = WithdrawalTriggeredByUser(user),
      triggeringEntityType = WithdrawableEntityType.SpaceBooking,
      triggeringEntityId = spaceBooking.id,
    )

    return withdraw(
      cas1WithdrawableTreeBuilder.treeForSpaceBooking(spaceBooking, user).rootNode,
      withdrawalContext,
    ) {
      cas1SpaceBookingService.withdraw(
        spaceBooking = spaceBooking,
        occurredAt = cancelledAt,
        userProvidedReasonId = userProvidedReason,
        userProvidedReasonNotes = otherReason,
        withdrawalContext = withdrawalContext,
        appealChangeRequestId = appealChangeRequestId,
      )
    }
  }

  private fun <T> withdraw(
    rootNode: WithdrawableTreeNode,
    context: WithdrawalContext,
    withdrawRootEntityFunction: () -> CasResult<T>,
  ): CasResult<T> {
    if (log.isDebugEnabled) {
      log.debug("Tree for withdrawing ${context.triggeringEntityType} with id ${context.triggeringEntityId} is $rootNode")
    }

    if (!rootNode.status.userMayDirectlyWithdraw) {
      return CasResult.Unauthorised()
    }

    if (!rootNode.status.withdrawable) {
      return CasResult.GeneralValidationError("${context.triggeringEntityType.label} is not in a withdrawable state")
    }

    if (rootNode.isBlocked()) {
      return CasResult.GeneralValidationError("${context.triggeringEntityType.label} withdrawal is blocked")
    }

    val withdrawalResult = withdrawRootEntityFunction.invoke()
    if (withdrawalResult is CasResult.Success) {
      cas1WithdrawableTreeOperations.withdrawDescendantsOfRootNode(rootNode, context)
    }
    return withdrawalResult
  }

  private fun toDirectlyWithdrawableEntities(rootNode: WithdrawableTreeNode) = rootNode.flatten()
    .filter { it.status.withdrawable }
    .filter { !it.isBlocked() }
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

data class WithdrawalContext(
  val withdrawalTriggeredBy: WithdrawalTriggeredBy,
  val triggeringEntityType: WithdrawableEntityType,
  val triggeringEntityId: UUID,
)

sealed interface WithdrawalTriggeredBy {
  fun getName(): String
}

data class WithdrawalTriggeredByUser(val user: UserEntity) : WithdrawalTriggeredBy {
  override fun getName() = user.name
}

data object WithdrawalTriggeredBySeedJob : WithdrawalTriggeredBy {
  override fun getName() = "Application Support"
}

data class WithdrawableEntity(
  val type: WithdrawableEntityType,
  val id: UUID,
  val dates: List<WithdrawableDatePeriod>,
)

data class WithdrawableEntitiesWithNotes(
  val notes: List<String>,
  val withdrawables: Set<WithdrawableEntity>,
)

enum class WithdrawableEntityType(val label: String) {
  Application("Application"),

  /**
   * See [PlacementRequestEntity.isForApplicationsArrivalDate] for why we label this as Request for Placement
   */
  PlacementRequest("Request for Placement"),
  PlacementApplication("Request for Placement"),
  Booking("Placement"),
  SpaceBooking("Space Booking"),
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
  /**
   * If set, not only is this entity not withdrawable, but any ancestor
   * of this entity is also not withdrawable. For example, bookings with
   * arrivals will block the withdrawal of any associated request for
   * placement and application
   */
  val blockingReason: BlockingReason? = null,
  val withdrawn: Boolean,
)

data class WithdrawableDatePeriod(
  val startDate: LocalDate,
  val endDate: LocalDate,
)

enum class BlockingReason {
  ArrivalRecordedInCas1,
  ArrivalRecordedInDelius,
  NonArrivalRecordedInCas1,
}
