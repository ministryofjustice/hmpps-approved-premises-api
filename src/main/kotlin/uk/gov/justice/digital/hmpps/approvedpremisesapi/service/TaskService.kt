package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AllocatedFilter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TaskRespository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.TypedTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotAllowedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import java.util.UUID

@Service
class TaskService(
  private val assessmentService: AssessmentService,
  private val userService: UserService,
  private val userAccessService: UserAccessService,
  private val placementRequestService: PlacementRequestService,
  private val userTransformer: UserTransformer,
  private val placementApplicationService: PlacementApplicationService,
  private val taskRespository: TaskRespository,
  private val assessmentRepository: AssessmentRepository,
  private val placementApplicationRepository: PlacementApplicationRepository,
  private val placementRequestRepository: PlacementRequestRepository,
) {
  fun getAllReallocatable(
    allocatedFilter: AllocatedFilter?,
    page: Int?,
  ): Pair<List<TypedTask>, PaginationMetadata?> {
    val pageSize = 10
    val pageable = if (page != null) { PageRequest.of(page - 1, pageSize) } else { null }

    val isAllocated = if (allocatedFilter == null) { null } else { allocatedFilter == AllocatedFilter.allocated }
    val reallocatableTaskResult = taskRespository.getAllReallocatable(isAllocated, pageable)
    val reallocatableTasks = reallocatableTaskResult.content

    val assessmentIds = reallocatableTasks.filter { it.type == "assessment" }.map { it.id }
    val placementApplicationIds = reallocatableTasks.filter { it.type == "placement_application" }.map { it.id }
    val placementRequestIds = reallocatableTasks.filter { it.type == "placement_request" }.map { it.id }

    val tasks = listOf(
      assessmentRepository.findAllById(assessmentIds).map { TypedTask.Assessment(it as ApprovedPremisesAssessmentEntity) },
      placementApplicationRepository.findAllById(placementApplicationIds).map { TypedTask.PlacementApplication(it) },
      placementRequestRepository.findAllById(placementRequestIds).map { TypedTask.PlacementRequest(it) },
    ).flatten()

    val metadata = getMetadata(reallocatableTaskResult, page)

    return Pair(tasks, metadata)
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
