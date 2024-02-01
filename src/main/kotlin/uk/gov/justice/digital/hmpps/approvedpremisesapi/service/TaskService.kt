package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AllocatedFilter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TaskEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TaskRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.TypedTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotAllowedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageable
import java.util.UUID

@Service
class TaskService(
  private val assessmentService: AssessmentService,
  private val userService: UserService,
  private val userAccessService: UserAccessService,
  private val placementRequestService: PlacementRequestService,
  private val userTransformer: UserTransformer,
  private val placementApplicationService: PlacementApplicationService,
  private val taskRepository: TaskRepository,
  private val assessmentRepository: AssessmentRepository,
  private val placementApplicationRepository: PlacementApplicationRepository,
  private val placementRequestRepository: PlacementRequestRepository,
) {

  data class TaskFilterCriteria(
    val allocatedFilter: AllocatedFilter?,
    val apAreaId: UUID?,
  )

  fun getAll(
    filterCriteria: TaskFilterCriteria,
    pageCriteria: PageCriteria<TaskSortField>,
  ): Pair<List<TypedTask>, PaginationMetadata?> {
    val pageable = getPageable(
      pageCriteria.withSortBy(
        when (pageCriteria.sortBy) {
          TaskSortField.createdAt -> "created_at"
        },
      ),
    )

    val allocatedFilter = filterCriteria.allocatedFilter
    val apAreaId = filterCriteria.apAreaId

    val isAllocated = if (allocatedFilter == null) { null } else { allocatedFilter == AllocatedFilter.allocated }
    val tasksResult = taskRepository.getAllReallocatable(isAllocated, apAreaId, pageable)
    val tasks = tasksResult.content

    val assessmentIds = tasks.filter { it.type == TaskEntityType.ASSESSMENT }.map { it.id }
    val assessments = assessmentRepository.findAllById(assessmentIds).map { TypedTask.Assessment(it as ApprovedPremisesAssessmentEntity) }

    val placementApplicationIds = tasks.filter { it.type == TaskEntityType.PLACEMENT_APPLICATION }.map { it.id }
    val placementApplications = placementApplicationRepository.findAllById(placementApplicationIds).map { TypedTask.PlacementApplication(it) }

    val placementRequestIds = tasks.filter { it.type == TaskEntityType.PLACEMENT_REQUEST }.map { it.id }
    val placementRequests = placementRequestRepository.findAllById(placementRequestIds).map { TypedTask.PlacementRequest(it) }

    val typedTasks = tasks
      .map { task ->
        val candidateList = when (task.type) {
          TaskEntityType.ASSESSMENT -> assessments
          TaskEntityType.PLACEMENT_APPLICATION -> placementApplications
          TaskEntityType.PLACEMENT_REQUEST -> placementRequests
        }

        candidateList.first { it.id == task.id }
      }

    val metadata = getMetadata(tasksResult, pageCriteria)
    return Pair(typedTasks, metadata)
  }

  fun reallocateTask(requestUser: UserEntity, taskType: TaskType, userToAllocateToId: UUID, id: UUID): AuthorisableActionResult<ValidatableActionResult<Reallocation>> {
    if (!userAccessService.userCanReallocateTask(requestUser)) {
      return AuthorisableActionResult.Unauthorised()
    }

    val assigneeUser = when (val assigneeUserResult = userService.updateUserFromCommunityApiById(userToAllocateToId)) {
      is AuthorisableActionResult.Success -> assigneeUserResult.entity
      else -> return AuthorisableActionResult.NotFound()
    }

    val result = when (taskType) {
      TaskType.assessment -> {
        assessmentService.reallocateAssessment(assigneeUser, id)
      }
      TaskType.placementRequest -> {
        placementRequestService.reallocatePlacementRequest(assigneeUser, id)
      }
      TaskType.placementApplication -> {
        placementApplicationService.reallocateApplication(assigneeUser, id)
      }
      else -> {
        throw NotAllowedProblem(detail = "The Task Type $taskType is not currently supported")
      }
    }

    val validationResult = when (result) {
      is AuthorisableActionResult.NotFound -> return AuthorisableActionResult.NotFound()
      is AuthorisableActionResult.Unauthorised -> return AuthorisableActionResult.Unauthorised()
      is AuthorisableActionResult.Success -> result.entity
    }

    return when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> AuthorisableActionResult.Success(ValidatableActionResult.GeneralValidationError(validationResult.message))
      is ValidatableActionResult.FieldValidationError -> AuthorisableActionResult.Success(ValidatableActionResult.FieldValidationError(validationResult.validationMessages))
      is ValidatableActionResult.ConflictError -> AuthorisableActionResult.Success(ValidatableActionResult.ConflictError(validationResult.conflictingEntityId, validationResult.message))
      is ValidatableActionResult.Success -> AuthorisableActionResult.Success(
        ValidatableActionResult.Success(
          entityToReallocation(validationResult.entity, taskType),
        ),
      )
    }
  }

  fun deallocateTask(
    requestUser: UserEntity,
    taskType: TaskType,
    id: UUID,
  ): AuthorisableActionResult<ValidatableActionResult<Unit>> {
    if (!userAccessService.userCanDeallocateTask(requestUser)) {
      return AuthorisableActionResult.Unauthorised()
    }

    val result = when (taskType) {
      TaskType.assessment -> assessmentService.deallocateAssessment(id)
      else -> throw NotAllowedProblem(detail = "The Task Type $taskType is not currently supported")
    }

    val validationResult = when (result) {
      is AuthorisableActionResult.NotFound -> return AuthorisableActionResult.NotFound()
      is AuthorisableActionResult.Unauthorised -> return AuthorisableActionResult.Unauthorised()
      is AuthorisableActionResult.Success -> result.entity
    }

    return when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> AuthorisableActionResult.Success(ValidatableActionResult.GeneralValidationError(validationResult.message))
      is ValidatableActionResult.FieldValidationError -> AuthorisableActionResult.Success(ValidatableActionResult.FieldValidationError(validationResult.validationMessages))
      is ValidatableActionResult.ConflictError -> AuthorisableActionResult.Success(ValidatableActionResult.ConflictError(validationResult.conflictingEntityId, validationResult.message))
      is ValidatableActionResult.Success -> AuthorisableActionResult.Success(
        ValidatableActionResult.Success(
          Unit,
        ),
      )
    }
  }

  private fun entityToReallocation(entity: Any, taskType: TaskType): Reallocation {
    val allocatedToUser = when (entity) {
      is PlacementRequestEntity -> entity.allocatedToUser
      is AssessmentEntity -> entity.allocatedToUser
      is PlacementApplicationEntity -> entity.allocatedToUser!!
      else -> throw RuntimeException("Unexpected type")
    }

    return Reallocation(
      taskType = taskType,
      user = userTransformer.transformJpaToApi(allocatedToUser!!, ServiceName.approvedPremises) as ApprovedPremisesUser,
    )
  }
}
