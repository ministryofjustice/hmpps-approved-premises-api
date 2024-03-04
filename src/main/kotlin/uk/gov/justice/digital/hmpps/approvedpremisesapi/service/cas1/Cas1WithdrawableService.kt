package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import java.time.LocalDate
import java.util.UUID
import javax.transaction.Transactional

@Service
class WithdrawableService(
  private val applicationService: ApplicationService,
  private val placementRequestService: PlacementRequestService,
  private val placementApplicationService: PlacementApplicationService,
  private val bookingService: BookingService,
  private val withdrawableTreeBuilder: WithdrawableTreeBuilder,
  private val cas1WithdrawableTreeOperations: Cas1WithdrawableTreeOperations,
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

  @Transactional
  fun withdrawApplication(
    applicationId: UUID,
    user: UserEntity,
    withdrawalReason: String,
    otherReason: String?,
  ): CasResult<Unit> {
    val application = applicationService.getApplication(applicationId) ?: return CasResult.NotFound()
    if (application !is ApprovedPremisesApplicationEntity) {
      return CasResult.GeneralValidationError("Only CAS1 Apps are supported")
    }

    val withdrawalContext = WithdrawalContext(
      triggeringUser = user,
      triggeringEntityType = WithdrawableEntityType.Application,
      triggeringEntityId = applicationId,
    )

    return withdraw(
      withdrawableTreeBuilder.treeForApp(application, user),
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
      ?: return CasResult.NotFound()

    val withdrawalContext = WithdrawalContext(
      triggeringUser = user,
      triggeringEntityType = WithdrawableEntityType.PlacementRequest,
      triggeringEntityId = placementRequestId,
    )

    return withdraw(
      withdrawableTreeBuilder.treeForPlacementReq(placementRequest, user),
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
    val placementApplication = placementApplicationService.getApplicationOrNull(placementApplicationId)
      ?: return CasResult.NotFound()

    val withdrawalContext = WithdrawalContext(
      triggeringUser = user,
      triggeringEntityType = WithdrawableEntityType.PlacementApplication,
      triggeringEntityId = placementApplicationId,
    )

    return withdraw(
      withdrawableTreeBuilder.treeForPlacementApp(placementApplication, user),
      withdrawalContext,
    ) {
      placementApplicationService.withdrawPlacementApplication(placementApplicationId, userProvidedReason, withdrawalContext)
    }
  }

  @Transactional
  fun withdrawBooking(
    booking: BookingEntity,
    user: UserEntity,
    cancelledAt: LocalDate,
    userProvidedReason: UUID?,
    notes: String?,
  ): CasResult<CancellationEntity> {
    val withdrawalContext = WithdrawalContext(
      triggeringUser = user,
      triggeringEntityType = WithdrawableEntityType.Booking,
      triggeringEntityId = booking.id,
    )

    return withdraw(
      withdrawableTreeBuilder.treeForBooking(booking, user),
      withdrawalContext,
    ) {
      bookingService.createCas1Cancellation(
        booking,
        cancelledAt,
        userProvidedReason,
        notes,
        withdrawalContext,
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

    val withdrawalResult = withdrawRootEntityFunction.invoke()
    if (withdrawalResult is CasResult.Success) {
      cas1WithdrawableTreeOperations.withdrawDescendantsOfRootNode(rootNode, context)
    }
    return withdrawalResult
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
