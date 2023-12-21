package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeBookedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Cru
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AllocatedFilter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
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
  private val userService: UserService,
  private val bookingNotMadeRepository: BookingNotMadeRepository,
  private val domainEventService: DomainEventService,
  private val offenderService: OffenderService,
  private val communityApiClient: CommunityApiClient,
  private val cruService: CruService,
  private val placementRequirementsRepository: PlacementRequirementsRepository,
  private val placementDateRepository: PlacementDateRepository,
  private val cancellationRepository: CancellationRepository,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: String,
) {

  fun getVisiblePlacementRequestsForUser(
    user: UserEntity,
    page: Int?,
    sortDirection: SortDirection?,
  ): Pair<List<PlacementRequestEntity>, PaginationMetadata?> {
    val sortField = "createdAt"
    val pageable = getPageable(sortField, sortDirection, page)
    val response = placementRequestRepository.findAllByAllocatedToUser_IdAndReallocatedAtNullAndIsWithdrawnFalse(
      user.id,
      pageable,
    )
    return Pair(response.content, getMetadata(response, page))
  }

  fun getAllReallocatable(
    page: Int?,
    sortField: TaskSortField,
    sortDirection: SortDirection?,
    allocatedFilter: AllocatedFilter?,
  ): Pair<List<PlacementRequestEntity>, PaginationMetadata?> {
    val pageable = getPageable(
      // Convert to snake_case, because the findAllReallocatable* methods are native SQL queries
      when (sortField) {
        TaskSortField.createdAt -> "created_at"
      },
      sortDirection,
      page,
    )
    val allReallocatable: Page<PlacementRequestEntity>?

    when {
      allocatedFilter == AllocatedFilter.unallocated ->
        allReallocatable =
          placementRequestRepository.findAllReallocatableUnallocated(pageable)
      allocatedFilter == AllocatedFilter.allocated ->
        allReallocatable =
          placementRequestRepository.findAllReallocatableAllocated(pageable)
      else ->
        allReallocatable =
          placementRequestRepository.findAllReallocatable(pageable)
    }

    return Pair(allReallocatable.content, getMetadata(allReallocatable, page))
  }

  fun getAllActive(
    status: PlacementRequestStatus?,
    crn: String?,
    crnOrName: String?,
    tier: String?,
    arrivalDateStart: LocalDate?,
    arrivalDateEnd: LocalDate?,
    page: Int?,
    sortBy: PlacementRequestSortField,
    sortDirection: SortDirection?,
  ): Pair<List<PlacementRequestEntity>, PaginationMetadata?> {
    val sortField = when (sortBy) {
      PlacementRequestSortField.applicationSubmittedAt -> "application.submitted_at"
      else -> sortBy.value
    }

    val pageable = getPageable(sortField, sortDirection, page)
    val response = placementRequestRepository.allForDashboard(
      status,
      crn,
      crnOrName,
      tier,
      arrivalDateStart,
      arrivalDateEnd,
      pageable,
    )

    return Pair(response.content, getMetadata(response, page))
  }

  fun getPlacementRequestForUser(
    user: UserEntity,
    id: UUID,
  ): AuthorisableActionResult<Pair<PlacementRequestEntity, List<CancellationEntity>>> {
    val placementRequest = placementRequestRepository.findByIdOrNull(id)
      ?: return AuthorisableActionResult.NotFound()

    if (placementRequest.allocatedToUser?.id != user.id && !user.hasRole(UserRole.CAS1_WORKFLOW_MANAGER)) {
      return AuthorisableActionResult.Unauthorised()
    }

    val cancellations = cancellationRepository.getCancellationsForApplicationId(placementRequest.application.id)

    return AuthorisableActionResult.Success(Pair(placementRequest, cancellations))
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

    val newPlacementRequest = placementRequestRepository.save(
      currentPlacementRequest.copy(
        id = UUID.randomUUID(),
        reallocatedAt = null,
        allocatedToUser = assigneeUser,
        createdAt = dateTimeNow,
        placementRequirements = currentPlacementRequest.placementRequirements,
      ),
    )

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
      val placementRequest = this.createPlacementRequest(placementRequirements, placementDates, notes, isParole, placementApplicationEntity)

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
    val user = userService

    return placementRequestRepository.save(
      PlacementRequestEntity(
        id = UUID.randomUUID(),
        duration = placementDates.duration,
        expectedArrival = placementDates.expectedArrival,
        placementApplication = placementApplicationEntity,
        placementRequirements = placementRequirements,
        createdAt = OffsetDateTime.now(),
        assessment = placementRequirements.assessment,
        application = placementRequirements.application,
        allocatedToUser = user.getUserForPlacementRequestAllocation(placementRequirements.application.crn),
        booking = null,
        bookingNotMades = mutableListOf(),
        reallocatedAt = null,
        notes = notes,
        isParole = isParole ?: false,
        isWithdrawn = false,
      ),
    )
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

  fun withdrawPlacementRequest(placementRequestId: UUID, user: UserEntity): AuthorisableActionResult<Unit> {
    if (!user.hasRole(UserRole.CAS1_WORKFLOW_MANAGER)) {
      return AuthorisableActionResult.Unauthorised()
    }

    val placementRequest = placementRequestRepository.findByIdOrNull(placementRequestId)
      ?: return AuthorisableActionResult.NotFound("PlacementRequest", placementRequestId.toString())

    placementRequest.isWithdrawn = true

    placementRequestRepository.save(placementRequest)

    return AuthorisableActionResult.Success(Unit)
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
}
