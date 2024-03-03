package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.UserAllocator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeBookedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Cru
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageable
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.transaction.Transactional

@Service
@Suppress("TooGenericExceptionThrown")
class PlacementRequestService(
  private val placementRequestRepository: PlacementRequestRepository,
  private val bookingNotMadeRepository: BookingNotMadeRepository,
  private val domainEventService: DomainEventService,
  private val offenderService: OffenderService,
  private val communityApiClient: CommunityApiClient,
  private val cruService: CruService,
  private val placementRequirementsRepository: PlacementRequirementsRepository,
  private val placementDateRepository: PlacementDateRepository,
  private val cancellationRepository: CancellationRepository,
  private val userAllocator: UserAllocator,
  private val userAccessService: UserAccessService,
  @Lazy private val applicationService: ApplicationService,
  private val cas1PlacementRequestEmailService: Cas1PlacementRequestEmailService,
  private val cas1PlacementRequestDomainEventService: Cas1PlacementRequestDomainEventService,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: String,
  private val taskDeadlineService: TaskDeadlineService,
) {

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  fun getVisiblePlacementRequestsForUser(
    user: UserEntity,
    page: Int? = null,
    sortDirection: SortDirection? = null,
    apAreaId: UUID? = null,
  ): Pair<List<PlacementRequestEntity>, PaginationMetadata?> {
    val sortField = "createdAt"
    val pageable = getPageable(sortField, sortDirection, page)
    val response = placementRequestRepository.findOpenRequestsAssignedToUser(
      userId = user.id,
      apAreaId = apAreaId,
      pageable = pageable,
    )
    return Pair(response.content, getMetadata(response, page))
  }

  fun getAllActive(
    searchCriteria: AllActiveSearchCriteria,
    pageCriteria: PageCriteria<PlacementRequestSortField>,
  ): Pair<List<PlacementRequestEntity>, PaginationMetadata?> {
    val sortField = when (pageCriteria.sortBy) {
      PlacementRequestSortField.applicationSubmittedAt -> "application.submitted_at"
      PlacementRequestSortField.createdAt -> "created_at"
      PlacementRequestSortField.expectedArrival -> "expected_arrival"
      PlacementRequestSortField.duration -> "duration"
      PlacementRequestSortField.requestType -> "request_type"
      PlacementRequestSortField.personName -> "person_name"
      PlacementRequestSortField.personRisksTier -> "person_risks_tier"
    }

    val pageable = getPageable(pageCriteria.withSortBy(sortField))
    val response = placementRequestRepository.allForDashboard(
      status = searchCriteria.status,
      crn = searchCriteria.crn,
      crnOrName = searchCriteria.crnOrName,
      tier = searchCriteria.tier,
      arrivalDateFrom = searchCriteria.arrivalDateStart,
      arrivalDateTo = searchCriteria.arrivalDateEnd,
      requestType = searchCriteria.requestType,
      apAreaId = searchCriteria.apAreaId,
      pageable = pageable,
    )

    return Pair(response.content, getMetadata(response, pageCriteria))
  }

  fun getPlacementRequestOrNull(id: UUID) = placementRequestRepository.findByIdOrNull(id)

  fun getPlacementRequestForUser(
    user: UserEntity,
    id: UUID,
  ): AuthorisableActionResult<PlacementRequestAndCancellations> {
    val placementRequest = placementRequestRepository.findByIdOrNull(id)
      ?: return AuthorisableActionResult.NotFound()

    if (placementRequest.allocatedToUser?.id != user.id && !user.hasRole(UserRole.CAS1_WORKFLOW_MANAGER)) {
      return AuthorisableActionResult.Unauthorised()
    }

    return AuthorisableActionResult.Success(toPlacementRequestAndCancellations(placementRequest))
  }

  fun reallocatePlacementRequest(
    assigneeUser: UserEntity,
    id: UUID,
  ): AuthorisableActionResult<ValidatableActionResult<PlacementRequestEntity>> {
    val currentPlacementRequest = placementRequestRepository.findByIdOrNull(id)
      ?: return AuthorisableActionResult.NotFound()

    if (currentPlacementRequest.booking != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This placement request has already been completed"),
      )
    }

    if (!assigneeUser.hasRole(UserRole.CAS1_MATCHER)) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.FieldValidationError(
          ValidationErrors().apply {
            this["$.userId"] = "lackingMatcherRole"
          },
        ),
      )
    }

    currentPlacementRequest.reallocatedAt = OffsetDateTime.now()
    placementRequestRepository.save(currentPlacementRequest)

    // Make the timestamp precision less precise, so we don't have any issues with microsecond resolution in tests
    val dateTimeNow = OffsetDateTime.now().withNano(0)

    val newPlacementRequest = currentPlacementRequest.copy(
      id = UUID.randomUUID(),
      reallocatedAt = null,
      allocatedToUser = assigneeUser,
      createdAt = dateTimeNow,
      placementRequirements = currentPlacementRequest.placementRequirements,
      dueAt = null,
    )

    newPlacementRequest.dueAt = taskDeadlineService.getDeadline(newPlacementRequest)

    placementRequestRepository.save(newPlacementRequest)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(
        newPlacementRequest,
      ),
    )
  }

  fun createPlacementRequestsFromPlacementApplication(
    placementApplicationEntity: PlacementApplicationEntity,
    notes: String?,
  ): AuthorisableActionResult<List<PlacementRequestEntity>> {
    val placementRequirements = placementRequirementsRepository.findTopByApplicationOrderByCreatedAtDesc(
      placementApplicationEntity.application,
    ) ?: return AuthorisableActionResult.NotFound(
      "Placement Requirements",
      placementApplicationEntity.application.id.toString(),
    )

    val placementDateEntities = placementDateRepository.findAllByPlacementApplication(placementApplicationEntity)

    if (placementDateEntities.isEmpty()) {
      return AuthorisableActionResult.NotFound(
        "Placement Dates for Placement Application",
        placementApplicationEntity.id.toString(),
      )
    }

    val placementRequests = placementDateEntities.map { placementDateEntity ->
      val placementDates = PlacementDates(
        expectedArrival = placementDateEntity.expectedArrival,
        duration = placementDateEntity.duration,
      )
      val isParole = placementApplicationEntity.placementType == PlacementType.RELEASE_FOLLOWING_DECISION
      val placementRequest =
        this.createPlacementRequest(placementRequirements, placementDates, notes, isParole, placementApplicationEntity)

      placementDateRepository.save(
        placementDateEntity.apply {
          placementDateEntity.placementRequest = placementRequest
        },
      )

      placementRequest
    }

    return AuthorisableActionResult.Success(placementRequests)
  }

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
      allocatedToUser = null,
      booking = null,
      bookingNotMades = mutableListOf(),
      reallocatedAt = null,
      notes = notes,
      isParole = isParole,
      isWithdrawn = false,
      withdrawalReason = null,
      dueAt = null,
    )

    val allocatedUser = userAllocator.getUserForPlacementRequestAllocation(placementRequest)

    placementRequest.allocatedToUser = allocatedUser
    placementRequest.dueAt = taskDeadlineService.getDeadline(placementRequest)

    return placementRequestRepository.save(placementRequest)
  }

  @Transactional
  fun createBookingNotMade(
    user: UserEntity,
    placementRequestId: UUID,
    notes: String?,
  ): AuthorisableActionResult<BookingNotMadeEntity> {
    val bookingNotCreatedAt = OffsetDateTime.now()

    val placementRequest = placementRequestRepository.findByIdOrNull(placementRequestId)
      ?: return AuthorisableActionResult.NotFound()

    val bookingNotMade = BookingNotMadeEntity(
      id = UUID.randomUUID(),
      placementRequest = placementRequest,
      createdAt = bookingNotCreatedAt,
      notes = notes,
    )

    saveBookingNotMadeDomainEvent(user, placementRequest, bookingNotCreatedAt, notes)

    return AuthorisableActionResult.Success(
      bookingNotMadeRepository.save(bookingNotMade),
    )
  }

  /**
   * This should return a maximum of one placement request, representing the dates known
   * at the point of application submission.
   *
   * For more information on why we do this, see [PlacementRequestEntity.isForApplicationsArrivalDate]
   */
  fun getWithdrawablePlacementRequestsForUser(
    user: UserEntity,
    application: ApprovedPremisesApplicationEntity,
  ): List<PlacementRequestEntity> =
    placementRequestRepository
      .findByApplication(application)
      .filter {
        it.isInWithdrawableState() &&
          it.isForApplicationsArrivalDate() &&
          userAccessService.userMayWithdrawPlacementRequest(user, it)
      }

  fun getWithdrawableState(placementRequest: PlacementRequestEntity, user: UserEntity): WithdrawableState {
    return WithdrawableState(
      withdrawable = placementRequest.isInWithdrawableState(),
      userMayDirectlyWithdraw = placementRequest.isForApplicationsArrivalDate() && userAccessService.userMayWithdrawPlacementRequest(user, placementRequest),
    )
  }

  /**
   * This function should not be called directly. Instead, use [WithdrawableService.withdrawPlacementRequest] that
   * will indirectly invoke this function. It will also ensure that:
   *
   * 1. The entity is withdrawable, and error if not
   * 2. The user is allowed to withdraw it, and error if not
   * 3. If withdrawn, all descdents entities are withdrawn, where applicable
   */
  @Transactional
  fun withdrawPlacementRequest(
    placementRequestId: UUID,
    userProvidedReason: PlacementRequestWithdrawalReason?,
    withdrawalContext: WithdrawalContext,
  ): CasResult<PlacementRequestAndCancellations> {
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
      WithdrawableEntityType.Booking -> throw InternalServerErrorProblem("Withdrawing a Booking should not cascade to PlacementRequests")
    }

    placementRequestRepository.save(placementRequest)

    val isUserRequestedWithdrawal = withdrawalContext.triggeringEntityType == WithdrawableEntityType.PlacementRequest
    updateApplicationStatusOnWithdrawal(placementRequest, isUserRequestedWithdrawal)

    cas1PlacementRequestEmailService.placementRequestWithdrawn(placementRequest)
    cas1PlacementRequestDomainEventService.placementRequestWithdrawn(placementRequest, withdrawalContext)

    return CasResult.Success(toPlacementRequestAndCancellations(placementRequest))
  }

  fun getPlacementRequestForInitialApplicationDates(applicationId: UUID) =
    placementRequestRepository.findByApplication_id(applicationId)
      .filter { it.isForApplicationsArrivalDate() }
      .firstOrNull { !it.isReallocated() }

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

  private fun saveBookingNotMadeDomainEvent(
    user: UserEntity,
    placementRequest: PlacementRequestEntity,
    bookingNotCreatedAt: OffsetDateTime,
    notes: String?,
  ) {
    val domainEventId = UUID.randomUUID()

    val application = placementRequest.application

    val offenderDetails = when (
      val offenderDetailsResult = offenderService.getOffenderByCrn(
        application.crn,
        user.deliusUsername,
        user.hasQualification(UserQualification.LAO),
      )
    ) {
      is AuthorisableActionResult.Success -> offenderDetailsResult.entity
      is AuthorisableActionResult.Unauthorised -> throw RuntimeException(
        "Unable to get Offender Details when " +
          "creating Booking Not Made Domain Event: Unauthorised",
      )

      is AuthorisableActionResult.NotFound -> throw RuntimeException(
        "Unable to get Offender Details when " +
          "creating Booking Not Made Domain Event: Not Found",
      )
    }

    val staffDetailsResult = communityApiClient.getStaffUserDetails(user.deliusUsername)
    val staffDetails = when (staffDetailsResult) {
      is ClientResult.Success -> staffDetailsResult.body
      is ClientResult.Failure -> staffDetailsResult.throwException()
    }

    domainEventService.saveBookingNotMadeEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        occurredAt = bookingNotCreatedAt.toInstant(),
        data = BookingNotMadeEnvelope(
          id = domainEventId,
          timestamp = bookingNotCreatedAt.toInstant(),
          eventType = "approved-premises.booking.not-made",
          eventDetails = BookingNotMade(
            applicationId = application.id,
            applicationUrl = applicationUrlTemplate.replace("#id", application.id.toString()),
            personReference = PersonReference(
              crn = application.crn,
              noms = offenderDetails.otherIds.nomsNumber ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = application.eventNumber,
            attemptedAt = bookingNotCreatedAt.toInstant(),
            attemptedBy = BookingMadeBookedBy(
              staffMember = StaffMember(
                staffCode = staffDetails.staffCode,
                staffIdentifier = staffDetails.staffIdentifier,
                forenames = staffDetails.staff.forenames,
                surname = staffDetails.staff.surname,
                username = staffDetails.username,
              ),
              cru = Cru(
                name = cruService.cruNameFromProbationAreaCode(staffDetails.probationArea.code),
              ),
            ),
            failureDescription = notes,
          ),
        ),
      ),
    )
  }

  private fun toPlacementRequestAndCancellations(placementRequest: PlacementRequestEntity): PlacementRequestAndCancellations {
    val cancellations = cancellationRepository.getCancellationsForApplicationId(placementRequest.application.id)
    return PlacementRequestAndCancellations(placementRequest, cancellations)
  }

  data class AllActiveSearchCriteria(
    val status: PlacementRequestStatus? = null,
    val crn: String? = null,
    val crnOrName: String? = null,
    val tier: String? = null,
    val arrivalDateStart: LocalDate? = null,
    val arrivalDateEnd: LocalDate? = null,
    val requestType: PlacementRequestRequestType? = null,
    val apAreaId: UUID? = null,
  )

  data class PlacementRequestAndCancellations(
    val placementRequest: PlacementRequestEntity,
    val cancellations: List<CancellationEntity>,
  )
}
