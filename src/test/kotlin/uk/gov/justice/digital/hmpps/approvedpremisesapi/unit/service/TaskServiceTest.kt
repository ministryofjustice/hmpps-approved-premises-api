package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.TaskService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import java.util.UUID

class TaskServiceTest {
  private val assessmentServiceMock = mockk<AssessmentService>()
  private val applicationRepositoryMock = mockk<ApplicationRepository>()
  private val userServiceMock = mockk<UserService>()
  private val placementRequestServiceMock = mockk<PlacementRequestService>()
  private val userTransformerMock = mockk<UserTransformer>()
  private val placementApplicationServiceMock = mockk<PlacementApplicationService>()

  private val taskService = TaskService(
    assessmentServiceMock,
    applicationRepositoryMock,
    userServiceMock,
    placementRequestServiceMock,
    userTransformerMock,
    placementApplicationServiceMock,
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
        .withRole(UserRole.CAS1_WORKFLOW_MANAGER)
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
      .withAllocatedToUser(assigneeUser)
      .produce()

    every { assessmentServiceMock.reallocateAssessment(assigneeUser, application) } returns AuthorisableActionResult.Success(
      ValidatableActionResult.Success(
        assessment,
      ),
    )

    val transformedUser = mockk<ApprovedPremisesUser>()

    every { userTransformerMock.transformJpaToApi(assigneeUser, ServiceName.approvedPremises) } returns transformedUser

    val reallocation = Reallocation(
      taskType = TaskType.assessment,
      user = transformedUser,
    )

    val result = taskService.reallocateTask(requestUserWithPermission, TaskType.assessment, assigneeUser.id, application.id)

    Assertions.assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity

    Assertions.assertThat(validationResult is ValidatableActionResult.Success).isTrue
    validationResult as ValidatableActionResult.Success

    Assertions.assertThat(validationResult.entity).isEqualTo(reallocation)
  }

  @Test
  fun `reallocateTask reallocates a placementRequest`() {
    val assigneeUser = generateAndStubAssigneeUser()
    val application = generateAndStubApplication()
    val assessment = AssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(assigneeUser)
      .produce()

    val placementRequest = PlacementRequestEntityFactory()
      .withPlacementRequirements(
        PlacementRequirementsEntityFactory()
          .withApplication(assessment.application as ApprovedPremisesApplicationEntity)
          .withAssessment(assessment)
          .produce(),
      )
      .withApplication(application)
      .withAssessment(assessment)
      .withAllocatedToUser(assigneeUser)
      .produce()

    every { placementRequestServiceMock.reallocatePlacementRequest(assigneeUser, application) } returns AuthorisableActionResult.Success(
      ValidatableActionResult.Success(
        placementRequest,
      ),
    )

    val transformedUser = mockk<ApprovedPremisesUser>()

    every { userTransformerMock.transformJpaToApi(assigneeUser, ServiceName.approvedPremises) } returns transformedUser

    val reallocation = Reallocation(
      taskType = TaskType.placementRequest,
      user = transformedUser,
    )

    val result = taskService.reallocateTask(requestUserWithPermission, TaskType.placementRequest, assigneeUser.id, application.id)

    Assertions.assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity

    Assertions.assertThat(validationResult is ValidatableActionResult.Success).isTrue
    validationResult as ValidatableActionResult.Success

    Assertions.assertThat(validationResult.entity).isEqualTo(reallocation)
  }

  @Test
  fun `reallocateTask reallocates a placementApplication`() {
    val assigneeUser = generateAndStubAssigneeUser()
    val application = generateAndStubApplication()

    val placementApplication = PlacementApplicationEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(assigneeUser)
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

    every { placementApplicationServiceMock.reallocateApplication(assigneeUser, application) } returns AuthorisableActionResult.Success(
      ValidatableActionResult.Success(
        placementApplication,
      ),
    )

    val transformedUser = mockk<ApprovedPremisesUser>()

    every { userTransformerMock.transformJpaToApi(assigneeUser, ServiceName.approvedPremises) } returns transformedUser

    val reallocation = Reallocation(
      taskType = TaskType.placementApplication,
      user = transformedUser,
    )

    val result = taskService.reallocateTask(requestUserWithPermission, TaskType.placementApplication, assigneeUser.id, application.id)

    Assertions.assertThat(result is AuthorisableActionResult.Success).isTrue
    val validationResult = (result as AuthorisableActionResult.Success).entity

    Assertions.assertThat(validationResult is ValidatableActionResult.Success).isTrue
    validationResult as ValidatableActionResult.Success

    Assertions.assertThat(validationResult.entity).isEqualTo(reallocation)
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
          .produce(),
      )
      .produce()

    every { applicationRepositoryMock.findByIdOrNull(application.id) } returns application

    return application
  }
}
