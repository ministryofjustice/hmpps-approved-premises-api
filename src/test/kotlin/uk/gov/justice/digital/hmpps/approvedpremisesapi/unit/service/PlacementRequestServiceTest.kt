package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostcodeDistrictRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import java.util.UUID

class PlacementRequestServiceTest {
  private val placementRequestRepository = mockk<PlacementRequestRepository>()
  private val postcodeDistrictRepository = mockk<PostcodeDistrictRepository>()
  private val characteristicRepository = mockk<CharacteristicRepository>()
  private val userRepository = mockk<UserRepository>()

  private val placementRequestService = PlacementRequestService(
    placementRequestRepository,
    postcodeDistrictRepository,
    characteristicRepository,
    userRepository
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
        .produce()
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

    val previousPlacementRequest = PlacementRequestEntityFactory()
      .withApplication(application)
      .withBooking(booking)
      .withAssessment(
        AssessmentEntityFactory()
          .withApplication(application)
          .withAllocatedToUser(assigneeUser)
          .produce()
      )
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

    val previousPlacementRequest = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(
        AssessmentEntityFactory()
          .withApplication(application)
          .withAllocatedToUser(assigneeUser)
          .produce()
      )
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
          .withRole(UserRole.MATCHER)
          .produce()
      }

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(assigneeUser)
      .produce()

    val previousPlacementRequest = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(
        AssessmentEntityFactory()
          .withApplication(application)
          .withAllocatedToUser(assigneeUser)
          .produce()
      )
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
    assertThat(newPlacementRequest.radius).isEqualTo(previousPlacementRequest.radius)
    assertThat(newPlacementRequest.postcodeDistrict).isEqualTo(previousPlacementRequest.postcodeDistrict)
    assertThat(newPlacementRequest.gender).isEqualTo(previousPlacementRequest.gender)
    assertThat(newPlacementRequest.expectedArrival).isEqualTo(previousPlacementRequest.expectedArrival)
    assertThat(newPlacementRequest.mentalHealthSupport).isEqualTo(previousPlacementRequest.mentalHealthSupport)
    assertThat(newPlacementRequest.apType).isEqualTo(previousPlacementRequest.apType)
    assertThat(newPlacementRequest.duration).isEqualTo(previousPlacementRequest.duration)
    assertThat(newPlacementRequest.desirableCriteria).isEqualTo(previousPlacementRequest.desirableCriteria)
    assertThat(newPlacementRequest.essentialCriteria).isEqualTo(previousPlacementRequest.essentialCriteria)
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

    val placementRequest = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(
        AssessmentEntityFactory()
          .withApplication(application)
          .withAllocatedToUser(requestingUser)
          .produce()
      )
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

    val placementRequest = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(
        AssessmentEntityFactory()
          .withApplication(application)
          .withAllocatedToUser(assigneeUser)
          .produce()
      )
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
          .withRole(UserRole.WORKFLOW_MANAGER)
          .produce()
      }

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(assigneeUser)
      .produce()

    val placementRequest = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(
        AssessmentEntityFactory()
          .withApplication(application)
          .withAllocatedToUser(assigneeUser)
          .produce()
      )
      .withAllocatedToUser(assigneeUser)
      .produce()

    every { placementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest

    val result = placementRequestService.getPlacementRequestForUser(requestingUser, placementRequest.id)

    assertThat(result is AuthorisableActionResult.Unauthorised)
  }
}
