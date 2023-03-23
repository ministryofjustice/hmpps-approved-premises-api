package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.TaskService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.time.OffsetDateTime
import java.util.UUID

class TaskServiceTest {
  private val assessmentServiceMock = mockk<AssessmentService>()
  private val applicationRepositoryMock = mockk<ApplicationRepository>()
  private val userServiceMock = mockk<UserService>()

  private val taskService = TaskService(
    assessmentServiceMock,
    applicationRepositoryMock,
    userServiceMock
  )

  private val requestUserWithPermission = UserEntityFactory()
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .produce()
    .apply {
      roles += UserRoleAssignmentEntityFactory()
        .withRole(UserRole.WORKFLOW_MANAGER)
        .withUser(this)
        .produce()
    }

  @Test
  fun `reallocateTask returns Unauthorised when requestUser does not have WORKFLOW_MANAGER role`() {
    val requestUser = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    val result = taskService.reallocateTask(requestUser, TaskType.assessment, UUID.randomUUID(), UUID.randomUUID())

    Assertions.assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `reallocateTask returns Not Found when assignee user does not exist`() {
    val assigneeUserId = UUID.fromString("55aa66be-0819-494e-955b-90b9aaa4f0c6")

    every { userServiceMock.updateUserFromCommunityApiById(assigneeUserId) } returns AuthorisableActionResult.NotFound()

    val result = taskService.reallocateTask(requestUserWithPermission, TaskType.assessment, assigneeUserId, UUID.randomUUID())

    Assertions.assertThat(result is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `reallocateTask returns Not Found when application does not exist`() {
    val assigneeUser = generateAndStubAssigneeUser()

    val applicationId = UUID.fromString("95c7175f-451a-47e0-af16-6bf9175b5581")

    every { applicationRepositoryMock.findByIdOrNull(applicationId) } returns null

    val result = taskService.reallocateTask(requestUserWithPermission, TaskType.assessment, assigneeUser.id, applicationId)

    Assertions.assertThat(result is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `reallocateTask reallocates an assessment`() {
    val assigneeUser = generateAndStubAssigneeUser()
    val application = generateAndStubApplication()

    val assessment = AssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(
        UserEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .produce()
      )
      .produce()

    every { assessmentServiceMock.reallocateAssessment(assigneeUser, application) } returns AuthorisableActionResult.Success(
      ValidatableActionResult.Success(
        assessment
      )
    )

    val result = taskService.reallocateTask(requestUserWithPermission, TaskType.assessment, assigneeUser.id, application.id)

    Assertions.assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity

    Assertions.assertThat(validationResult is ValidatableActionResult.Success).isTrue
    validationResult as ValidatableActionResult.Success

    Assertions.assertThat(validationResult.entity).isEqualTo(assessment)
  }

  private fun generateAndStubAssigneeUser(): UserEntity {
    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    every { userServiceMock.updateUserFromCommunityApiById(user.id) } returns AuthorisableActionResult.Success(user)

    return user
  }

  private fun generateAndStubApplication(): ApprovedPremisesApplicationEntity {
    val application = ApprovedPremisesApplicationEntityFactory()
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

    every { applicationRepositoryMock.findByIdOrNull(application.id) } returns application

    return application
  }
}
