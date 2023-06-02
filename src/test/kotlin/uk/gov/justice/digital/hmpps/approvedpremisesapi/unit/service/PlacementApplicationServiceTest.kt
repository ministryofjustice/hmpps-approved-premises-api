package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService

class PlacementApplicationServiceTest {
  private val placementApplicationRepository = mockk<PlacementApplicationRepository>()
  private val jsonSchemaService = mockk<JsonSchemaService>()
  private val userService = mockk<UserService>()
  private val placementDateRepository = mockk<PlacementDateRepository>()

  private val placementApplicationService = PlacementApplicationService(
    placementApplicationRepository,
    jsonSchemaService,
    userService,
    placementDateRepository,
  )

  @Nested
  inner class ReallocateApplicationTest {
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

    private val previousPlacementApplication = PlacementApplicationEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(assigneeUser)
      .withDecision(null)
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
    fun `Reallocating an application returns successfully`() {
      assigneeUser.apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_ASSESSOR)
          .produce()
      }

      every { placementApplicationRepository.findByApplication_IdAndReallocatedAtNull(application.id) } returns previousPlacementApplication

      every { placementApplicationRepository.save(previousPlacementApplication) } answers { it.invocation.args[0] as PlacementApplicationEntity }
      every { placementApplicationRepository.save(match { it.allocatedToUser == assigneeUser }) } answers { it.invocation.args[0] as PlacementApplicationEntity }

      val result = placementApplicationService.reallocateApplication(assigneeUser, application)

      assertThat(result is AuthorisableActionResult.Success).isTrue
      val validationResult = (result as AuthorisableActionResult.Success).entity

      assertThat(validationResult is ValidatableActionResult.Success).isTrue
      validationResult as ValidatableActionResult.Success

      assertThat(previousPlacementApplication.reallocatedAt).isNotNull

      verify { placementApplicationRepository.save(match { it.allocatedToUser == assigneeUser }) }

      val newPlacementApplication = validationResult.entity

      assertThat(newPlacementApplication.application).isEqualTo(application)
      assertThat(newPlacementApplication.allocatedToUser).isEqualTo(assigneeUser)
      assertThat(newPlacementApplication.createdByUser).isEqualTo(newPlacementApplication.createdByUser)
      assertThat(newPlacementApplication.data).isEqualTo(newPlacementApplication.data)
      assertThat(newPlacementApplication.document).isEqualTo(newPlacementApplication.document)
      assertThat(newPlacementApplication.schemaVersion).isEqualTo(newPlacementApplication.schemaVersion)
    }

    @Test
    fun `Reallocating a placement application with a decision returns a General Validation Error`() {
      previousPlacementApplication.apply {
        decision = PlacementApplicationDecision.ACCEPTED
      }

      every { placementApplicationRepository.findByApplication_IdAndReallocatedAtNull(application.id) } returns previousPlacementApplication

      val result = placementApplicationService.reallocateApplication(assigneeUser, application)

      assertThat(result is AuthorisableActionResult.Success).isTrue
      val validationResult = (result as AuthorisableActionResult.Success).entity

      assertThat(validationResult is ValidatableActionResult.GeneralValidationError).isTrue
      validationResult as ValidatableActionResult.GeneralValidationError
      assertThat(validationResult.message).isEqualTo("This placement application has already been completed")
    }

    @Test
    fun `Reallocating a placement application when user to assign to is not an ASSESSOR returns a field validation error`() {
      every { placementApplicationRepository.findByApplication_IdAndReallocatedAtNull(application.id) } returns previousPlacementApplication

      val result = placementApplicationService.reallocateApplication(assigneeUser, application)

      assertThat(result is AuthorisableActionResult.Success).isTrue
      val validationResult = (result as AuthorisableActionResult.Success).entity

      assertThat(validationResult is ValidatableActionResult.FieldValidationError).isTrue
      validationResult as ValidatableActionResult.FieldValidationError
      assertThat(validationResult.validationMessages).containsEntry("$.userId", "lackingAssessorRole")
    }
  }
}
