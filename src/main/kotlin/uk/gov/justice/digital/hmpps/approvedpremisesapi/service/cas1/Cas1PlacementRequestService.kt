package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1PlacementRequestSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockablePlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Suppress("TooGenericExceptionThrown")
class Cas1PlacementRequestService(
  private val placementRequestRepository: PlacementRequestRepository,
  private val bookingNotMadeRepository: BookingNotMadeRepository,
  private val placementRequirementsRepository: PlacementRequirementsRepository,
  private val cancellationRepository: CancellationRepository,
  @Lazy private val applicationService: ApplicationService,
  private val cas1PlacementRequestEmailService: Cas1PlacementRequestEmailService,
  private val cas1PlacementRequestDomainEventService: Cas1PlacementRequestDomainEventService,
  private val cas1BookingDomainEventService: Cas1BookingDomainEventService,
  private val offenderService: OffenderService,
  private val lockablePlacementRequestRepository: LockablePlacementRequestRepository,
  private val clock: Clock,
) {

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  fun getAllCas1Active(
    searchCriteria: AllActiveSearchCriteria,
    pageCriteria: PageCriteria<PlacementRequestSortField>,
  ): Pair<List<Cas1PlacementRequestSummary>, PaginationMetadata?> {
    val pageable = pageCriteria.toPageable(
      when (pageCriteria.sortBy) {
        PlacementRequestSortField.applicationSubmittedAt -> "applicationSubmittedDate"
        PlacementRequestSortField.createdAt -> "created_at"
        PlacementRequestSortField.expectedArrival -> "requestedPlacementArrivalDate"
        PlacementRequestSortField.duration -> "requestedPlacementDuration"
        PlacementRequestSortField.requestType -> "requestType"
        PlacementRequestSortField.personName -> "personName"
        PlacementRequestSortField.personRisksTier -> "personTier"
      },
    )

    val response = placementRequestRepository.allForCas1Dashboard(
      status = searchCriteria.status?.name,
      crnOrName = searchCriteria.crnOrName,
      tier = searchCriteria.tier,
      arrivalDateFrom = searchCriteria.arrivalDateStart,
      arrivalDateTo = searchCriteria.arrivalDateEnd,
      requestType = searchCriteria.requestType?.name,
      cruManagementAreaId = searchCriteria.cruManagementAreaId,
      pageable = pageable,
    )

    return Pair(response.content, getMetadata(response, pageCriteria))
  }

  fun getPlacementRequestOrNull(id: UUID) = placementRequestRepository.findByIdOrNull(id)

  fun getPlacementRequest(
    user: UserEntity,
    id: UUID,
  ): CasResult<PlacementRequestEntity> {
    val placementRequest = placementRequestRepository.findByIdOrNull(id)
      ?: return CasResult.NotFound("PlacementRequest", id.toString())

    if (!offenderService.canAccessOffender(placementRequest.application.crn, user.cas1LaoStrategy())) {
      return CasResult.Unauthorised()
    }

    return CasResult.Success(placementRequest)
  }

  @Deprecated("Use getPlacementRequest instead")
  fun getPlacementRequestForUser(
    user: UserEntity,
    id: UUID,
  ): CasResult<PlacementRequestAndCancellations> {
    val placementRequest = placementRequestRepository.findByIdOrNull(id)
      ?: return CasResult.NotFound("PlacementRequest", id.toString())

    if (!offenderService.canAccessOffender(placementRequest.application.crn, user.cas1LaoStrategy())) {
      return CasResult.Unauthorised()
    }

    return CasResult.Success(toPlacementRequestAndCancellations(placementRequest))
  }

  fun createPlacementRequestsFromPlacementApplication(
    placementApplicationEntity: PlacementApplicationEntity,
    notes: String?,
  ): CasResult<Unit> {
    val placementRequirements = placementRequirementsRepository.findTopByApplicationOrderByCreatedAtDesc(
      placementApplicationEntity.application,
    ) ?: return CasResult.NotFound(
      "Placement Requirements",
      placementApplicationEntity.application.id.toString(),
    )

    val placementDates = PlacementDates(
      expectedArrival = placementApplicationEntity.placementDates()!!.expectedArrival,
      duration = placementApplicationEntity.placementDates()!!.duration,
    )
    val isParole = placementApplicationEntity.placementType == PlacementType.RELEASE_FOLLOWING_DECISION

    this.createPlacementRequest(
      placementRequirements = placementRequirements,
      placementDates = placementDates,
      notes = notes,
      isParole = isParole,
      placementApplicationEntity = placementApplicationEntity,
    )

    return CasResult.Success(Unit)
  }

  @Deprecated(
    """
    Because all new placement requests are now linked to a placement application, we should 
    really be using the createPlacementRequestsFromPlacementApplication function as the entry point
    to create any placement requests. At that point this function will become private
    """,
  )
  fun createPlacementRequest(
    placementRequirements: PlacementRequirementsEntity,
    placementDates: PlacementDates,
    notes: String?,
    isParole: Boolean,
    placementApplicationEntity: PlacementApplicationEntity?,
  ): PlacementRequestEntity {
    val placementRequest = PlacementRequestEntity(
      id = UUID.randomUUID(),
      duration = placementDates.duration,
      expectedArrival = placementDates.expectedArrival,
      placementApplication = placementApplicationEntity,
      placementRequirements = placementRequirements,
      createdAt = OffsetDateTime.now(),
      assessment = placementRequirements.assessment,
      application = placementRequirements.application,
      spaceBookings = mutableListOf(),
      bookingNotMades = mutableListOf(),
      notes = notes,
      isParole = isParole,
      isWithdrawn = false,
      withdrawalReason = null,
    )

    val updatedPlacementRequest = placementRequestRepository.save(placementRequest)

    return updatedPlacementRequest
  }

  @Transactional
  fun createBookingNotMade(
    user: UserEntity,
    placementRequestId: UUID,
    notes: String?,
  ): CasResult<BookingNotMadeEntity> {
    val bookingNotMadeCreatedAt = OffsetDateTime.now(clock)

    val placementRequest = placementRequestRepository.findByIdOrNull(placementRequestId)
      ?: return CasResult.NotFound("PlacementRequest", placementRequestId.toString())

    val bookingNotMade = BookingNotMadeEntity(
      id = UUID.randomUUID(),
      placementRequest = placementRequest,
      createdAt = bookingNotMadeCreatedAt,
      notes = notes,
    )

    cas1BookingDomainEventService.bookingNotMade(user, placementRequest, bookingNotMadeCreatedAt, notes)

    return CasResult.Success(bookingNotMadeRepository.save(bookingNotMade))
  }

  fun getWithdrawableState(placementRequest: PlacementRequestEntity): WithdrawableState = WithdrawableState(
    withdrawable = placementRequest.isInWithdrawableState(),
    withdrawn = placementRequest.isWithdrawn,
    userMayDirectlyWithdraw = false,
  )

  /**
   * This function should not be called directly. Instead, use [Cas1WithdrawableService] that
   * will indirectly invoke this function via cascading down the withdrawal tree. It will also ensure that:
   *
   * 1. The entity is withdrawable, and error if not
   * 2. The user is allowed to withdraw it, and error if not
   * 3. If withdrawn, all descendent entities are withdrawn, where applicable
   */
  @Transactional
  fun withdrawPlacementRequest(
    placementRequestId: UUID,
    userProvidedReason: PlacementRequestWithdrawalReason?,
    withdrawalContext: WithdrawalContext,
  ): CasResult<PlacementRequestAndCancellations> {
    lockablePlacementRequestRepository.acquirePessimisticLock(placementRequestId)

    val placementRequest = placementRequestRepository.findByIdOrNull(placementRequestId)
      ?: return CasResult.NotFound("PlacementRequest", placementRequestId.toString())

    if (placementRequest.isWithdrawn) {
      return CasResult.Success(toPlacementRequestAndCancellations(placementRequest))
    }

    placementRequest.isWithdrawn = true
    placementRequest.withdrawalReason = when (withdrawalContext.triggeringEntityType) {
      WithdrawableEntityType.Application -> PlacementRequestWithdrawalReason.RELATED_APPLICATION_WITHDRAWN
      WithdrawableEntityType.PlacementApplication -> PlacementRequestWithdrawalReason.RELATED_PLACEMENT_APPLICATION_WITHDRAWN
      WithdrawableEntityType.PlacementRequest -> userProvidedReason
      WithdrawableEntityType.SpaceBooking -> throw InternalServerErrorProblem("Withdrawing a Booking should not cascade to PlacementRequests")
    }

    placementRequestRepository.save(placementRequest)

    if (withdrawalContext.withdrawalTriggeredBy is WithdrawalTriggeredByUser) {
      val isUserRequestedWithdrawal = withdrawalContext.triggeringEntityType == WithdrawableEntityType.PlacementRequest
      updateApplicationStatusOnWithdrawal(placementRequest, isUserRequestedWithdrawal)
    }

    cas1PlacementRequestEmailService.placementRequestWithdrawn(placementRequest, withdrawalContext.withdrawalTriggeredBy)
    cas1PlacementRequestDomainEventService.placementRequestWithdrawn(placementRequest, withdrawalContext)

    return CasResult.Success(toPlacementRequestAndCancellations(placementRequest))
  }

  /**
   * Whilst this should always return 0 or 1 requests, due to legacy application
   * data inconsistencies there are a small number of applications that will return multiple
   * placement requests.
   *
   * We know that only one of these placement requests relate to the initial application dates,
   * and the rest relate to placement_applications, but we have no programmatic way of 'fixing'
   * this link for those placement requests.
   *
   * Note: this is only an issue when the placement applications are withdrawn, so any incorrect
   * placement requests returned here will appear as withdrawn to the user
   *
   * See [uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1FixPlacementApplicationLinksJob] for more information.
   */
  fun getPlacementRequestForInitialApplicationDates(applicationId: UUID) = placementRequestRepository.findByApplicationId(applicationId)
    .filter { it.isForLegacyInitialRequestForPlacement() }

  private fun updateApplicationStatusOnWithdrawal(
    placementRequest: PlacementRequestEntity,
    isUserRequestedWithdrawal: Boolean,
  ) {
    if (!isUserRequestedWithdrawal) {
      return
    }

    val application = placementRequest.application
    val placementRequests = placementRequestRepository.findByApplication(application)
    val anyActivePlacementRequests = placementRequests.any { it.isActive() }
    if (!anyActivePlacementRequests) {
      applicationService.updateApprovedPremisesApplicationStatus(
        application.id,
        ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST,
      )
    }
  }

  private fun toPlacementRequestAndCancellations(placementRequest: PlacementRequestEntity): PlacementRequestAndCancellations {
    val cancellations = cancellationRepository.getCancellationsForApplicationId(placementRequest.application.id)
    return PlacementRequestAndCancellations(placementRequest, cancellations)
  }

  data class AllActiveSearchCriteria(
    val status: PlacementRequestStatus? = null,
    val crnOrName: String? = null,
    val tier: String? = null,
    val arrivalDateStart: LocalDate? = null,
    val arrivalDateEnd: LocalDate? = null,
    val requestType: PlacementRequestRequestType? = null,
    val cruManagementAreaId: UUID? = null,
  )

  data class PlacementRequestAndCancellations(
    val placementRequest: PlacementRequestEntity,
    @Deprecated("This is not currently used by the UI and does not cater for SpaceBooking cancellations")
    val cancellations: List<CancellationEntity>,
  )
}
