package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AllocatedFilter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TaskEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TaskRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1TaskService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.TypedTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PaginationConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import java.util.UUID

class Cas1TaskServiceTest {
  private val assessmentServiceMock = mockk<AssessmentService>()
  private val userServiceMock = mockk<UserService>()
  private val userAccessServiceMock = mockk<UserAccessService>()
  private val userTransformerMock = mockk<UserTransformer>()
  private val cas1PlacementApplicationServiceMock = mockk<Cas1PlacementApplicationService>()
  private val taskRepositoryMock = mockk<TaskRepository>()
  private val assessmentRepositoryMock = mockk<AssessmentRepository>()
  private val placementApplicationRepositoryMock = mockk<PlacementApplicationRepository>()

  private val cas1TaskService = Cas1TaskService(
    assessmentServiceMock,
    userServiceMock,
    userAccessServiceMock,
    userTransformerMock,
    cas1PlacementApplicationServiceMock,
    taskRepositoryMock,
    assessmentRepositoryMock,
    placementApplicationRepositoryMock,
  )

  private val requestUser = UserEntityFactory().withDefaults().withRoleEnums(listOf(UserRole.CAS1_CRU_MEMBER)).produce()

  @Test
  fun `reallocateTask returns Unauthorised when requestUser does not have role to reallocate the task`() {
    val requestUser = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withRoleEnums(UserRole.getAllRolesExcept(UserRole.CAS1_CRU_MEMBER))
      .produce()

    every { userAccessServiceMock.userCanReallocateTask(any()) } returns false

    val result = cas1TaskService.reallocateTask(requestUser, TaskType.assessment, UUID.randomUUID(), UUID.randomUUID())

    assertThatCasResult(result).isUnauthorised()
  }

  @Test
  fun `reallocateTask returns Not Found when assignee user does not exist`() {
    every { userAccessServiceMock.userCanReallocateTask(any()) } returns true

    val assigneeUserId = UUID.fromString("55aa66be-0819-494e-955b-90b9aaa4f0c6")
    every { userServiceMock.updateUserFromDelius(assigneeUserId, ServiceName.approvedPremises) } returns CasResult.NotFound("task", "id")

    val result = cas1TaskService.reallocateTask(requestUser, TaskType.assessment, assigneeUserId, UUID.randomUUID())

    assertThatCasResult(result).isNotFound("user", "55aa66be-0819-494e-955b-90b9aaa4f0c6")
  }

  @Test
  fun `reallocateTask reallocates an assessment`() {
    every { userAccessServiceMock.userCanReallocateTask(any()) } returns true

    val assigneeUser = generateAndStubAssigneeUser()
    val application = generateApplication()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(assigneeUser)
      .produce()

    every {
      assessmentServiceMock.reallocateAssessment(
        allocatingUser = requestUser,
        assigneeUser = assigneeUser,
        id = assessment.id,
      )
    } returns CasResult.Success(assessment)

    val transformedUser = mockk<ApprovedPremisesUser>()

    every { userTransformerMock.transformJpaToApi(assigneeUser, ServiceName.approvedPremises) } returns transformedUser

    val reallocation = Reallocation(
      taskType = TaskType.assessment,
      user = transformedUser,
    )

    val result = cas1TaskService.reallocateTask(requestUser, TaskType.assessment, assigneeUser.id, assessment.id)

    assertThatCasResult(result).isSuccess().with {
      assertThat(it).isEqualTo(reallocation)
    }
  }

  @Test
  fun `reallocateTask reallocates a placementApplication`() {
    every { userAccessServiceMock.userCanReallocateTask(any()) } returns true

    val assigneeUser = generateAndStubAssigneeUser()
    val application = generateApplication()

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

    every {
      cas1PlacementApplicationServiceMock.reallocateApplication(assigneeUser, placementApplication.id)
    } returns CasResult.Success(
      placementApplication,
    )

    val transformedUser = mockk<ApprovedPremisesUser>()

    every { userTransformerMock.transformJpaToApi(assigneeUser, ServiceName.approvedPremises) } returns transformedUser

    val reallocation = Reallocation(
      taskType = TaskType.placementApplication,
      user = transformedUser,
    )

    val result = cas1TaskService.reallocateTask(requestUser, TaskType.placementApplication, assigneeUser.id, placementApplication.id)

    assertThatCasResult(result).isSuccess().with {
      assertThat(it).isEqualTo(reallocation)
    }
  }

  @Test
  fun `deallocateTask returns Unauthorised when requestUser does not have permissions to deallocate the task`() {
    val requestUser = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    every { userAccessServiceMock.userCanDeallocateTask(any()) } returns false

    val result = cas1TaskService.deallocateTask(requestUser, TaskType.assessment, UUID.randomUUID())

    assertThatCasResult(result).isUnauthorised()
  }

  @Test
  fun `deallocateTask deallocates an assessment`() {
    every { userAccessServiceMock.userCanDeallocateTask(any()) } returns true

    val assigneeUser = generateAndStubAssigneeUser()
    val application = generateApplication()

    val assessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(assigneeUser)
      .produce()

    every { assessmentServiceMock.deallocateAssessment(assessment.id) } returns CasResult.Success(assessment)

    val result = cas1TaskService.deallocateTask(requestUser, TaskType.assessment, assessment.id)

    assertThatCasResult(result).isSuccess()
  }

  @Test
  fun `getAll returns all tasks for a particular page`() {
    mockkStatic("uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PaginationUtilsKt")

    PaginationConfig(defaultPageSize = 10).postInit()

    val isAllocated = true
    val apAreaId = UUID.randomUUID()
    val cruManagementAreaId = UUID.randomUUID()

    val assessments = List(3) { generateAssessment() }
    val placementApplications = List(4) { generatePlacementApplication() }

    val assessmentTasks = assessments.map {
      Task(
        it.id,
        it.createdAt.toLocalDateTime(),
        TaskEntityType.ASSESSMENT,
        (it.application as ApprovedPremisesApplicationEntity).name,
        it.allocatedToUser?.name,
        it.submittedAt?.toLocalDateTime(),
        it.decision?.name,
      )
    }
    val placementApplicationTasks = placementApplications.map {
      Task(
        it.id,
        it.createdAt.toLocalDateTime(),
        TaskEntityType.PLACEMENT_APPLICATION,
        it.application.name,
        it.allocatedToUser?.name,
        it.submittedAt?.toLocalDateTime(),
        it.decision?.name,
      )
    }

    val tasks = listOf(
      assessmentTasks,
      placementApplicationTasks,
    ).flatten()

    val page = mockk<Page<Task>>()
    val metadata = mockk<PaginationMetadata>()
    val taskEntityTypes = TaskEntityType.entries
    val allocatedToUserId = UUID.randomUUID()
    val requiredQualification = UserQualification.pipe
    val crnOrName = "CRN123"
    val isCompleted = false

    every { page.content } returns tasks
    every {
      taskRepositoryMock.getAll(
        isAllocated,
        apAreaId,
        cruManagementAreaId,
        taskEntityTypes.map { it.name },
        allocatedToUserId,
        requiredQualification.value,
        crnOrName,
        isCompleted,
        PageRequest.of(0, 10, Sort.by("created_at").ascending()),
      )
    } returns page
    every { assessmentRepositoryMock.findAllById(assessments.map { it.id }) } returns assessments
    every { placementApplicationRepositoryMock.findAllById(placementApplications.map { it.id }) } returns placementApplications

    val pageCriteria = PageCriteria(TaskSortField.createdAt, SortDirection.asc, 1)

    every { getMetadata(page, pageCriteria) } returns metadata

    val result = cas1TaskService.getAll(
      Cas1TaskService.TaskFilterCriteria(
        AllocatedFilter.allocated,
        apAreaId,
        cruManagementAreaId,
        taskEntityTypes,
        allocatedToUserId,
        requiredQualification,
        crnOrName,
        isCompleted,
      ),
      pageCriteria,
    )

    val expectedTasks = listOf(
      assessments.map { TypedTask.Assessment(it) },
      placementApplications.map { TypedTask.PlacementApplication(it) },
    ).flatten()

    assertThat(result.first).isEqualTo(expectedTasks)
    assertThat(result.second).isEqualTo(metadata)
  }

  private fun generateAndStubAssigneeUser(): UserEntity {
    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    every {
      userServiceMock.updateUserFromDelius(
        user.id,
        ServiceName.approvedPremises,
      )
    } returns
      CasResult.Success(
        UserService.GetUserResponse.Success(user),
      )

    return user
  }

  private fun generateApplication(): ApprovedPremisesApplicationEntity {
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

    return application
  }

  private fun generateAssessment(application: ApprovedPremisesApplicationEntity = generateApplication()): ApprovedPremisesAssessmentEntity = ApprovedPremisesAssessmentEntityFactory()
    .withApplication(application)
    .produce()

  private fun generatePlacementApplication(): PlacementApplicationEntity {
    val application = generateApplication()
    return PlacementApplicationEntityFactory()
      .withApplication(application)
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
  }
}
