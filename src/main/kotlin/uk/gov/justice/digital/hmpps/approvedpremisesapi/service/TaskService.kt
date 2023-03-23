package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotAllowedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import java.util.UUID

@Service
class TaskService(
  private val assessmentService: AssessmentService,
  private val applicationRepository: ApplicationRepository,
  private val userService: UserService,
  private val placementRequestService: PlacementRequestService,
  private val userTransformer: UserTransformer
) {
  fun reallocateTask(requestUser: UserEntity, taskType: TaskType, userToAllocateToId: UUID, applicationId: UUID): AuthorisableActionResult<ValidatableActionResult<Reallocation>> {
    if (!requestUser.hasRole(UserRole.WORKFLOW_MANAGER)) {
      return AuthorisableActionResult.Unauthorised()
    }

    val assigneeUser = when (val assigneeUserResult = userService.updateUserFromCommunityApiById(userToAllocateToId)) {
      is AuthorisableActionResult.Success -> assigneeUserResult.entity
      else -> return AuthorisableActionResult.NotFound()
    }

    val application = applicationRepository.findByIdOrNull(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    if (application !is ApprovedPremisesApplicationEntity) {
      throw RuntimeException("Only CAS1 Applications are currently supported")
    }

    val result = when (taskType) {
      TaskType.assessment -> {
        assessmentService.reallocateAssessment(assigneeUser, application)
      }
      TaskType.placementRequest -> {
        placementRequestService.reallocatePlacementRequest(assigneeUser, application)
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
          entityToReallocation(validationResult.entity, taskType)
        )
      )
    }
  }

  private fun entityToReallocation(entity: Any, taskType: TaskType): Reallocation {
    val allocatedToUser = when (entity) {
      is PlacementRequestEntity -> entity.allocatedToUser
      is AssessmentEntity -> entity.allocatedToUser
      else -> throw RuntimeException("Unexpected type")
    }

    return Reallocation(
      taskType = taskType,
      user = userTransformer.transformJpaToApi(allocatedToUser, ServiceName.approvedPremises)
    )
  }
}
