package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
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

    val previousPlacementRequest = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(previousUser)
      .withBooking(booking)
      .produce()

    every { placementRequestRepository.findByApplication_IdAndReallocatedAtNull(application.id) } returns previousPlacementRequest

    val result = placementRequestService.reallocatePlacementRequest(assigneeUser, application)

    Assertions.assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity

    Assertions.assertThat(validationResult is ValidatableActionResult.GeneralValidationError).isTrue
    validationResult as ValidatableActionResult.GeneralValidationError
    Assertions.assertThat(validationResult.message).isEqualTo("This placement request has already been completed")
  }

  @Test
  fun `reallocatePlacementRequest returns Field Validation Error when user to assign to is not a MATCHER`() {
    val previousPlacementRequest = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(previousUser)
      .produce()

    every { placementRequestRepository.findByApplication_IdAndReallocatedAtNull(application.id) } returns previousPlacementRequest

    val result = placementRequestService.reallocatePlacementRequest(assigneeUser, application)

    Assertions.assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity

    Assertions.assertThat(validationResult is ValidatableActionResult.FieldValidationError).isTrue
    validationResult as ValidatableActionResult.FieldValidationError
    Assertions.assertThat(validationResult.validationMessages).containsEntry("$.userId", "lackingMatcherRole")
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

    val previousPlacementRequest = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(previousUser)
      .produce()

    every { placementRequestRepository.findByApplication_IdAndReallocatedAtNull(application.id) } returns previousPlacementRequest

    every { placementRequestRepository.save(previousPlacementRequest) } answers { it.invocation.args[0] as PlacementRequestEntity }
    every { placementRequestRepository.save(match { it.allocatedToUser == assigneeUser }) } answers { it.invocation.args[0] as PlacementRequestEntity }

    val result = placementRequestService.reallocatePlacementRequest(assigneeUser, application)

    Assertions.assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity

    Assertions.assertThat(validationResult is ValidatableActionResult.Success).isTrue
    validationResult as ValidatableActionResult.Success

    Assertions.assertThat(previousPlacementRequest.reallocatedAt).isNotNull

    verify { placementRequestRepository.save(match { it.allocatedToUser == assigneeUser }) }

    val newPlacementRequest = validationResult.entity

    Assertions.assertThat(newPlacementRequest.application).isEqualTo(application)
    Assertions.assertThat(newPlacementRequest.allocatedToUser).isEqualTo(assigneeUser)
    Assertions.assertThat(newPlacementRequest.radius).isEqualTo(previousPlacementRequest.radius)
    Assertions.assertThat(newPlacementRequest.postcodeDistrict).isEqualTo(previousPlacementRequest.postcodeDistrict)
    Assertions.assertThat(newPlacementRequest.gender).isEqualTo(previousPlacementRequest.gender)
    Assertions.assertThat(newPlacementRequest.expectedArrival).isEqualTo(previousPlacementRequest.expectedArrival)
    Assertions.assertThat(newPlacementRequest.mentalHealthSupport).isEqualTo(previousPlacementRequest.mentalHealthSupport)
    Assertions.assertThat(newPlacementRequest.apType).isEqualTo(previousPlacementRequest.apType)
    Assertions.assertThat(newPlacementRequest.duration).isEqualTo(previousPlacementRequest.duration)
    Assertions.assertThat(newPlacementRequest.desirableCriteria).isEqualTo(previousPlacementRequest.desirableCriteria)
    Assertions.assertThat(newPlacementRequest.essentialCriteria).isEqualTo(previousPlacementRequest.essentialCriteria)
  }
}
