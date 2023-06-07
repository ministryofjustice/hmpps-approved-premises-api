package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CruService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import java.util.UUID

class PlacementRequestServiceTest {
  private val placementRequestRepository = mockk<PlacementRequestRepository>()
  private val userRepository = mockk<UserRepository>()
  private val bookingNotMadeRepository = mockk<BookingNotMadeRepository>()
  private val domainEventService = mockk<DomainEventService>()
  private val offenderService = mockk<OffenderService>()
  private val communityApiClient = mockk<CommunityApiClient>()
  private val cruService = mockk<CruService>()
  private val placementRequirementsRepository = mockk<PlacementRequirementsRepository>()
  private val placementDateRepository = mockk<PlacementDateRepository>()

  private val placementRequestService = PlacementRequestService(
    placementRequestRepository,
    userRepository,
    bookingNotMadeRepository,
    domainEventService,
    offenderService,
    communityApiClient,
    cruService,
    placementRequirementsRepository,
    placementDateRepository,
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

  private val application = ApprovedPremisesApplicationEntityFactory()
    .withCreatedByUser(
      UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce(),
    )
    .produce()

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

    val assessment = AssessmentEntityFactory()
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

    every { placementRequestRepository.findByApplication_IdAndReallocatedAtNull(application.id) } returns previousPlacementRequest

    val result = placementRequestService.reallocatePlacementRequest(assigneeUser, application)

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

    val assessment = AssessmentEntityFactory()
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

    every { placementRequestRepository.findByApplication_IdAndReallocatedAtNull(application.id) } returns previousPlacementRequest

    val result = placementRequestService.reallocatePlacementRequest(assigneeUser, application)

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

    val assessment = AssessmentEntityFactory()
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

    every { placementRequestRepository.findByApplication_IdAndReallocatedAtNull(application.id) } returns previousPlacementRequest

    every { placementRequestRepository.save(previousPlacementRequest) } answers { it.invocation.args[0] as PlacementRequestEntity }
    every { placementRequestRepository.save(match { it.allocatedToUser == assigneeUser }) } answers { it.invocation.args[0] as PlacementRequestEntity }

    val result = placementRequestService.reallocatePlacementRequest(assigneeUser, application)

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

    assertThat(result is AuthorisableActionResult.NotFound)
  }

  @Test
  fun `getPlacementRequestForUser returns Unauthorised when PlacementRequest not allocated to User and User does not have WORKFLOW_MANAGER role`() {
    val requestingUser = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(requestingUser)
      .produce()

    val assessment = AssessmentEntityFactory()
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

    assertThat(result is AuthorisableActionResult.Unauthorised)
  }

  @Test
  fun `getPlacementRequestForUser returns Success when PlacementRequest is allocated to User`() {
    val requestingUser = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(assigneeUser)
      .produce()

    val assessment = AssessmentEntityFactory()
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

    every { placementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest

    val result = placementRequestService.getPlacementRequestForUser(requestingUser, placementRequest.id)

    assertThat(result is AuthorisableActionResult.Unauthorised)
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

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(assigneeUser)
      .produce()

    val assessment = AssessmentEntityFactory()
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
      .withAllocatedToUser(assigneeUser)
      .produce()

    every { placementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest

    val result = placementRequestService.getPlacementRequestForUser(requestingUser, placementRequest.id)

    assertThat(result is AuthorisableActionResult.Unauthorised)
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
  fun `createBookingNotMade returns Unauthorised when Placement Request isn't allocated to User`() {
    val requestingUser = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val otherUser = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(otherUser)
      .produce()

    val assessment = AssessmentEntityFactory()
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
      .withAllocatedToUser(otherUser)
      .withApplication(application)
      .withAssessment(assessment)
      .produce()

    every { placementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest

    val result = placementRequestService.createBookingNotMade(requestingUser, placementRequest.id, null)
    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
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

    val assessment = AssessmentEntityFactory()
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
}
