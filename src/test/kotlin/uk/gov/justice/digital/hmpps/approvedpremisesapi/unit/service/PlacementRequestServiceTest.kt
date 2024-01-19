package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.UserAllocator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CruService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addRoleForUnitTest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PaginationConfig
import java.time.LocalDate
import java.util.UUID

class PlacementRequestServiceTest {
  private val placementRequestRepository = mockk<PlacementRequestRepository>()
  private val bookingNotMadeRepository = mockk<BookingNotMadeRepository>()
  private val domainEventService = mockk<DomainEventService>()
  private val offenderService = mockk<OffenderService>()
  private val communityApiClient = mockk<CommunityApiClient>()
  private val cruService = mockk<CruService>()
  private val placementRequirementsRepository = mockk<PlacementRequirementsRepository>()
  private val placementDateRepository = mockk<PlacementDateRepository>()
  private val cancellationRepository = mockk<CancellationRepository>()
  private val userAllocator = mockk<UserAllocator>()

  private val placementRequestService = PlacementRequestService(
    placementRequestRepository,
    bookingNotMadeRepository,
    domainEventService,
    offenderService,
    communityApiClient,
    cruService,
    placementRequirementsRepository,
    placementDateRepository,
    cancellationRepository,
    userAllocator,
    "http://frontend/applications/#id",
  )

  private val previousUser = UserEntityFactory()
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .produce()

  private val assigneeUser = UserEntityFactory()
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .produce()

  @BeforeEach
  fun before() {
    PaginationConfig(defaultPageSize = 10).postInit()
  }

  @Test
  fun `reallocatePlacementRequest returns General Validation Error when request already has an associated booking`() {
    val premisesEntity = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val booking = BookingEntityFactory()
      .withYieldedPremises { premisesEntity }
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(assigneeUser)
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(assigneeUser)
      .produce()

    val previousPlacementRequest = PlacementRequestEntityFactory()
      .withPlacementRequirements(
        PlacementRequirementsEntityFactory()
          .withApplication(application)
          .withAssessment(assessment)
          .produce(),
      )
      .withApplication(application)
      .withBooking(booking)
      .withAssessment(assessment)
      .withAllocatedToUser(previousUser)
      .produce()

    every { placementRequestRepository.findByIdOrNull(previousPlacementRequest.id) } returns previousPlacementRequest

    val result = placementRequestService.reallocatePlacementRequest(assigneeUser, previousPlacementRequest.id)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity

    assertThat(validationResult is ValidatableActionResult.GeneralValidationError).isTrue
    validationResult as ValidatableActionResult.GeneralValidationError
    assertThat(validationResult.message).isEqualTo("This placement request has already been completed")
  }

  @Test
  fun `reallocatePlacementRequest returns Field Validation Error when user to assign to is not a MATCHER`() {
    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(assigneeUser)
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(assigneeUser)
      .produce()

    val previousPlacementRequest = PlacementRequestEntityFactory()
      .withPlacementRequirements(
        PlacementRequirementsEntityFactory()
          .withApplication(application)
          .withAssessment(assessment)
          .produce(),
      )
      .withApplication(application)
      .withAssessment(assessment)
      .withAllocatedToUser(previousUser)
      .produce()

    every { placementRequestRepository.findByIdOrNull(previousPlacementRequest.id) } returns previousPlacementRequest

    val result = placementRequestService.reallocatePlacementRequest(assigneeUser, previousPlacementRequest.id)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity

    assertThat(validationResult is ValidatableActionResult.FieldValidationError).isTrue
    validationResult as ValidatableActionResult.FieldValidationError
    assertThat(validationResult.validationMessages).containsEntry("$.userId", "lackingMatcherRole")
  }

  @Test
  fun `reallocatePlacementRequest returns Success, deallocates old placementRequest and creates a new one`() {
    val assigneeUser = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_MATCHER)
          .produce()
      }

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(assigneeUser)
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(assigneeUser)
      .produce()

    val previousPlacementRequest = PlacementRequestEntityFactory()
      .withPlacementRequirements(
        PlacementRequirementsEntityFactory()
          .withApplication(application)
          .withAssessment(assessment)
          .produce(),
      )
      .withApplication(application)
      .withAssessment(assessment)
      .withAllocatedToUser(previousUser)
      .produce()

    every { placementRequestRepository.findByIdOrNull(previousPlacementRequest.id) } returns previousPlacementRequest

    every { placementRequestRepository.save(previousPlacementRequest) } answers { it.invocation.args[0] as PlacementRequestEntity }
    every { placementRequestRepository.save(match { it.allocatedToUser == assigneeUser }) } answers { it.invocation.args[0] as PlacementRequestEntity }

    val result = placementRequestService.reallocatePlacementRequest(assigneeUser, previousPlacementRequest.id)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity

    assertThat(validationResult is ValidatableActionResult.Success).isTrue
    validationResult as ValidatableActionResult.Success

    assertThat(previousPlacementRequest.reallocatedAt).isNotNull

    verify { placementRequestRepository.save(match { it.allocatedToUser == assigneeUser }) }

    val newPlacementRequest = validationResult.entity

    assertThat(newPlacementRequest.application).isEqualTo(application)
    assertThat(newPlacementRequest.allocatedToUser).isEqualTo(assigneeUser)
    assertThat(newPlacementRequest.placementRequirements.radius).isEqualTo(previousPlacementRequest.placementRequirements.radius)
    assertThat(newPlacementRequest.placementRequirements.postcodeDistrict).isEqualTo(previousPlacementRequest.placementRequirements.postcodeDistrict)
    assertThat(newPlacementRequest.placementRequirements.gender).isEqualTo(previousPlacementRequest.placementRequirements.gender)
    assertThat(newPlacementRequest.expectedArrival).isEqualTo(previousPlacementRequest.expectedArrival)
    assertThat(newPlacementRequest.placementRequirements.apType).isEqualTo(previousPlacementRequest.placementRequirements.apType)
    assertThat(newPlacementRequest.duration).isEqualTo(previousPlacementRequest.duration)
    assertThat(newPlacementRequest.placementRequirements.desirableCriteria).isEqualTo(previousPlacementRequest.placementRequirements.desirableCriteria)
    assertThat(newPlacementRequest.placementRequirements.essentialCriteria).isEqualTo(previousPlacementRequest.placementRequirements.essentialCriteria)
  }

  @Test
  fun `getPlacementRequestForUser returns NotFound when PlacementRequest doesn't exist`() {
    val placementRequestId = UUID.fromString("72f15a57-8f3a-48bc-abc7-be09fe548fea")

    val requestingUser = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    every { placementRequestRepository.findByIdOrNull(placementRequestId) } returns null

    val result = placementRequestService.getPlacementRequestForUser(requestingUser, placementRequestId)

    assertThat(result is AuthorisableActionResult.NotFound).isTrue()
  }

  @Test
  fun `getPlacementRequestForUser returns Unauthorised when PlacementRequest not allocated to User and User does not have WORKFLOW_MANAGER role`() {
    val requestingUser = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(requestingUser)
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(requestingUser)
      .produce()

    val placementRequest = PlacementRequestEntityFactory()
      .withPlacementRequirements(
        PlacementRequirementsEntityFactory()
          .withApplication(application)
          .withAssessment(assessment)
          .produce(),
      )
      .withApplication(application)
      .withAssessment(assessment)
      .withAllocatedToUser(assigneeUser)
      .produce()

    every { placementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest

    val result = placementRequestService.getPlacementRequestForUser(requestingUser, placementRequest.id)

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue()
  }

  @Test
  fun `getPlacementRequestForUser returns Success when PlacementRequest is allocated to User`() {
    val requestingUser = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(assigneeUser)
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(assigneeUser)
      .produce()

    val placementRequest = PlacementRequestEntityFactory()
      .withPlacementRequirements(
        PlacementRequirementsEntityFactory()
          .withApplication(application)
          .withAssessment(assessment)
          .produce(),
      )
      .withApplication(application)
      .withAssessment(assessment)
      .withAllocatedToUser(requestingUser)
      .produce()

    val mockCancellations = mockk<List<CancellationEntity>>()

    every { placementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
    every { cancellationRepository.getCancellationsForApplicationId(application.id) } returns mockCancellations

    val result = placementRequestService.getPlacementRequestForUser(requestingUser, placementRequest.id)

    assertThat(result is AuthorisableActionResult.Success).isTrue()

    val (expectedPlacementRequest, expectedCancellations) = (result as AuthorisableActionResult.Success).entity

    assertThat(expectedPlacementRequest).isEqualTo(placementRequest)
    assertThat(expectedCancellations).isEqualTo(mockCancellations)
  }

  @Test
  fun `getPlacementRequestForUser returns Success when User has the WORKFLOW_MANAGER role`() {
    val requestingUser = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_WORKFLOW_MANAGER)
          .produce()
      }

    val otherUser = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(assigneeUser)
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(assigneeUser)
      .produce()

    val placementRequest = PlacementRequestEntityFactory()
      .withPlacementRequirements(
        PlacementRequirementsEntityFactory()
          .withApplication(application)
          .withAssessment(assessment)
          .produce(),
      )
      .withApplication(application)
      .withAssessment(assessment)
      .withAllocatedToUser(otherUser)
      .produce()

    val mockCancellations = mockk<List<CancellationEntity>>()

    every { placementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
    every { cancellationRepository.getCancellationsForApplicationId(application.id) } returns mockCancellations

    val result = placementRequestService.getPlacementRequestForUser(requestingUser, placementRequest.id)

    assertThat(result is AuthorisableActionResult.Success).isTrue()

    val (expectedPlacementRequest, expectedCancellations) = (result as AuthorisableActionResult.Success).entity

    assertThat(expectedPlacementRequest).isEqualTo(placementRequest)
    assertThat(expectedCancellations).isEqualTo(mockCancellations)
  }

  @Test
  fun `createBookingNotMade returns Not Found when Placement Request doesn't exist`() {
    val requestingUser = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val placementRequestId = UUID.fromString("25dd65b1-38b5-47bc-a00b-f2df228ed06b")

    every { placementRequestRepository.findByIdOrNull(placementRequestId) } returns null

    val result = placementRequestService.createBookingNotMade(requestingUser, placementRequestId, null)
    assertThat(result is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `createBookingNotMade returns Success, saves Booking Not Made and saves domain event`() {
    val requestingUser = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val otherUser = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(otherUser)
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(otherUser)
      .produce()

    val placementRequest = PlacementRequestEntityFactory()
      .withPlacementRequirements(
        PlacementRequirementsEntityFactory()
          .withApplication(application)
          .withAssessment(assessment)
          .produce(),
      )
      .withAllocatedToUser(requestingUser)
      .withApplication(application)
      .withAssessment(assessment)
      .produce()

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn(application.crn)
      .produce()

    every { offenderService.getOffenderByCrn(application.crn, requestingUser.deliusUsername) } returns AuthorisableActionResult.Success(offenderDetails)

    val staffUserDetails = StaffUserDetailsFactory().produce()

    every { communityApiClient.getStaffUserDetails(requestingUser.deliusUsername) } returns ClientResult.Success(
      HttpStatus.OK,
      staffUserDetails,
    )

    every { domainEventService.saveBookingNotMadeEvent(any()) } just Runs

    every { placementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
    every { bookingNotMadeRepository.save(any()) } answers { it.invocation.args[0] as BookingNotMadeEntity }

    every { cruService.cruNameFromProbationAreaCode(staffUserDetails.probationArea.code) } returns "CRU NAME"

    val result = placementRequestService.createBookingNotMade(requestingUser, placementRequest.id, "some notes")
    assertThat(result is AuthorisableActionResult.Success).isTrue
    val bookingNotMade = (result as AuthorisableActionResult.Success).entity

    assertThat(bookingNotMade.placementRequest).isEqualTo(placementRequest)
    assertThat(bookingNotMade.notes).isEqualTo("some notes")

    verify(exactly = 1) { bookingNotMadeRepository.save(match { it.notes == "some notes" && it.placementRequest == placementRequest }) }

    verify(exactly = 1) {
      domainEventService.saveBookingNotMadeEvent(
        match {
          val data = it.data.eventDetails
          val application = placementRequest.application

          it.applicationId == application.id &&
            it.crn == application.crn &&
            data.applicationId == application.id &&
            data.applicationUrl == "http://frontend/applications/${application.id}" &&
            data.personReference == PersonReference(
            crn = offenderDetails.otherIds.crn,
            noms = offenderDetails.otherIds.nomsNumber!!,
          ) &&
            data.deliusEventNumber == application.eventNumber &&
            data.failureDescription == "some notes"
        },
      )
    }
  }

  @Test
  fun `withdrawPlacementRequest returns Unauthorised if User is not WORKFLOW_MANAGER`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val result = placementRequestService.withdrawPlacementRequest(UUID.randomUUID(), user)

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `withdrawPlacementRequest returns Not Found if no Placement Request with ID exists`() {
    val placementRequestId = UUID.fromString("49f3eef9-4770-4f00-8f31-8e6f4cb4fd9e")

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .addRoleForUnitTest(UserRole.CAS1_WORKFLOW_MANAGER)

    every { placementRequestRepository.findByIdOrNull(placementRequestId) } returns null

    val result = placementRequestService.withdrawPlacementRequest(placementRequestId, user)

    assertThat(result is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `withdrawPlacementRequest returns Success, saves PlacementRequest with isWithdrawn set to true`() {
    val placementRequestId = UUID.fromString("49f3eef9-4770-4f00-8f31-8e6f4cb4fd9e")

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .addRoleForUnitTest(UserRole.CAS1_WORKFLOW_MANAGER)

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(user)
      .produce()

    val placementRequest = PlacementRequestEntityFactory()
      .withId(placementRequestId)
      .withPlacementRequirements(
        PlacementRequirementsEntityFactory()
          .withApplication(application)
          .withAssessment(assessment)
          .produce(),
      )
      .withApplication(application)
      .withAssessment(assessment)
      .withAllocatedToUser(assigneeUser)
      .produce()

    every { placementRequestRepository.findByIdOrNull(placementRequestId) } returns placementRequest
    every { placementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }

    val result = placementRequestService.withdrawPlacementRequest(placementRequestId, user)

    assertThat(result is AuthorisableActionResult.Success).isTrue

    verify {
      placementRequestRepository.save(
        match {
          it.id == placementRequestId &&
            it.isWithdrawn
        },
      )
    }
  }

  @Test
  fun `getAllActive returns results and no metadata when a page number is not provided`() {
    val placementRequests = createPlacementRequests(2)
    val page = mockk<Page<PlacementRequestEntity>>()

    every { page.content } returns placementRequests

    every { placementRequestRepository.allForDashboard(PlacementRequestStatus.matched, null, null, null, null, null, null) } returns page

    val (requests, metadata) = placementRequestService.getAllActive(
      PlacementRequestStatus.matched,
      null,
      null,
      null,
      null,
      null,
      PageCriteria(page = null, sortBy = PlacementRequestSortField.createdAt, sortDirection = SortDirection.asc),
    )

    assertThat(requests).isEqualTo(placementRequests)
    assertThat(metadata).isNull()
  }

  @Test
  fun `getAllActive returns a page and metadata when a page number is provided`() {
    val placementRequests = createPlacementRequests(2)
    val page = mockk<Page<PlacementRequestEntity>>()
    val pageRequest = mockk<PageRequest>()

    mockkStatic(PageRequest::class)

    every { PageRequest.of(0, 10, Sort.by("created_at").ascending()) } returns pageRequest
    every { page.content } returns placementRequests
    every { page.totalPages } returns 10
    every { page.totalElements } returns 100

    every { placementRequestRepository.allForDashboard(PlacementRequestStatus.matched, null, null, null, null, null, pageRequest) } returns page

    val (requests, metadata) = placementRequestService.getAllActive(
      PlacementRequestStatus.matched,
      null,
      null,
      null,
      null,
      null,
      PageCriteria(page = 1, sortBy = PlacementRequestSortField.createdAt, sortDirection = SortDirection.asc),
    )

    assertThat(requests).isEqualTo(placementRequests)
    assertThat(metadata?.currentPage).isEqualTo(1)
    assertThat(metadata?.pageSize).isEqualTo(10)
    assertThat(metadata?.totalPages).isEqualTo(10)
    assertThat(metadata?.totalResults).isEqualTo(100)
  }

  @Test
  fun `getAllActive returns a page and metadata when a page number, sort field and direction is provided`() {
    val placementRequests = createPlacementRequests(2)
    val page = mockk<Page<PlacementRequestEntity>>()
    val pageRequest = mockk<PageRequest>()

    mockkStatic(PageRequest::class)

    every { PageRequest.of(0, 10, Sort.by("expected_arrival").descending()) } returns pageRequest
    every { page.content } returns placementRequests
    every { page.totalPages } returns 10
    every { page.totalElements } returns 100

    every { placementRequestRepository.allForDashboard(PlacementRequestStatus.matched, null, null, null, null, null, pageRequest) } returns page

    val (requests, metadata) = placementRequestService.getAllActive(
      PlacementRequestStatus.matched,
      null,
      null,
      null,
      null,
      null,
      PageCriteria(page = 1, sortBy = PlacementRequestSortField.expectedArrival, sortDirection = SortDirection.desc),
    )

    assertThat(requests).isEqualTo(placementRequests)
    assertThat(metadata?.currentPage).isEqualTo(1)
    assertThat(metadata?.pageSize).isEqualTo(10)
    assertThat(metadata?.totalPages).isEqualTo(10)
    assertThat(metadata?.totalResults).isEqualTo(100)
  }

  @Test
  fun `getAllActive returns only results for CRN when provided`() {
    val crn = "CRN456"
    val placementRequests = createPlacementRequests(2, crn)
    val page = mockk<Page<PlacementRequestEntity>>()
    val pageRequest = mockk<PageRequest>()

    mockkStatic(PageRequest::class)

    every { PageRequest.of(0, 10, Sort.by("expected_arrival").descending()) } returns pageRequest
    every { page.content } returns placementRequests
    every { page.totalPages } returns 10
    every { page.totalElements } returns 100

    every { placementRequestRepository.allForDashboard(null, crn, null, null, null, null, pageRequest) } returns page

    val (requests, metadata) = placementRequestService.getAllActive(
      null,
      crn,
      null,
      null,
      null,
      null,
      PageCriteria(page = 1, sortBy = PlacementRequestSortField.expectedArrival, sortDirection = SortDirection.desc),
    )

    assertThat(requests).isEqualTo(placementRequests)
    assertThat(metadata?.currentPage).isEqualTo(1)
    assertThat(metadata?.pageSize).isEqualTo(10)
    assertThat(metadata?.totalPages).isEqualTo(10)
    assertThat(metadata?.totalResults).isEqualTo(100)
  }

  @Test
  fun `getAllActive returns only results for tier when provided`() {
    val tier = "A2"

    val placementRequests = createPlacementRequests(2, tier = tier)
    val page = mockk<Page<PlacementRequestEntity>>()
    val pageRequest = mockk<PageRequest>()

    mockkStatic(PageRequest::class)

    every { PageRequest.of(0, 10, Sort.by("expected_arrival").descending()) } returns pageRequest
    every { page.content } returns placementRequests
    every { page.totalPages } returns 10
    every { page.totalElements } returns 100

    every { placementRequestRepository.allForDashboard(null, null, null, tier, null, null, pageRequest) } returns page

    val (requests, metadata) = placementRequestService.getAllActive(
      null,
      null,
      null,
      tier,
      null,
      null,
      PageCriteria(page = 1, sortBy = PlacementRequestSortField.expectedArrival, sortDirection = SortDirection.desc),
    )

    assertThat(requests).isEqualTo(placementRequests)
    assertThat(metadata?.currentPage).isEqualTo(1)
    assertThat(metadata?.pageSize).isEqualTo(10)
    assertThat(metadata?.totalPages).isEqualTo(10)
    assertThat(metadata?.totalResults).isEqualTo(100)
  }

  @Test
  fun `getAllActive returns only results with arrival date after or equal to start when provided`() {
    val startDate = LocalDate.parse("2023-08-08")

    val placementRequests = createPlacementRequests(2, arrivalDate = startDate)
    val page = mockk<Page<PlacementRequestEntity>>()
    val pageRequest = mockk<PageRequest>()

    mockkStatic(PageRequest::class)

    every { PageRequest.of(0, 10, Sort.by("expected_arrival").descending()) } returns pageRequest
    every { page.content } returns placementRequests
    every { page.totalPages } returns 10
    every { page.totalElements } returns 100

    every { placementRequestRepository.allForDashboard(null, null, null, null, startDate, null, pageRequest) } returns page

    val (requests, metadata) = placementRequestService.getAllActive(
      null,
      null,
      null,
      null,
      startDate,
      null,
      PageCriteria(page = 1, sortBy = PlacementRequestSortField.expectedArrival, sortDirection = SortDirection.desc),
    )

    assertThat(requests).isEqualTo(placementRequests)
    assertThat(metadata?.currentPage).isEqualTo(1)
    assertThat(metadata?.pageSize).isEqualTo(10)
    assertThat(metadata?.totalPages).isEqualTo(10)
    assertThat(metadata?.totalResults).isEqualTo(100)
  }

  @Test
  fun `getAllActive returns only results with arrival date before or equal to end when provided`() {
    val endDate = LocalDate.parse("2023-08-08")

    val placementRequests = createPlacementRequests(2, arrivalDate = endDate)
    val page = mockk<Page<PlacementRequestEntity>>()
    val pageRequest = mockk<PageRequest>()

    mockkStatic(PageRequest::class)

    every { PageRequest.of(0, 10, Sort.by("expected_arrival").descending()) } returns pageRequest
    every { page.content } returns placementRequests
    every { page.totalPages } returns 10
    every { page.totalElements } returns 100

    every { placementRequestRepository.allForDashboard(null, null, null, null, endDate, null, pageRequest) } returns page

    val (requests, metadata) = placementRequestService.getAllActive(
      null,
      null,
      null,
      null,
      endDate,
      null,
      PageCriteria(page = 1, sortBy = PlacementRequestSortField.expectedArrival, sortDirection = SortDirection.desc),
    )

    assertThat(requests).isEqualTo(placementRequests)
    assertThat(metadata?.currentPage).isEqualTo(1)
    assertThat(metadata?.pageSize).isEqualTo(10)
    assertThat(metadata?.totalPages).isEqualTo(10)
    assertThat(metadata?.totalResults).isEqualTo(100)
  }

  private fun createPlacementRequests(num: Int, crn: String? = null, tier: String? = null, arrivalDate: LocalDate? = null): List<PlacementRequestEntity> {
    return List(num) {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .apply {
          if (crn != null) this.withCrn(crn)

          if (tier != null) {
            this.withRiskRatings(
              PersonRisksFactory()
                .withTier(
                  RiskWithStatus(
                    RiskTier(
                      level = tier,
                      lastUpdated = LocalDate.now(),
                    ),
                  ),
                )
                .produce(),
            )
          }
        }
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .produce()

      PlacementRequestEntityFactory()
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withApplication(application)
        .withAssessment(assessment)
        .withAllocatedToUser(assigneeUser)
        .apply {
          if (arrivalDate != null) {
            this.withExpectedArrival(arrivalDate)
          }
        }
        .produce()
    }
  }
}
