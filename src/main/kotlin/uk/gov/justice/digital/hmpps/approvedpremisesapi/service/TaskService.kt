package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotAllowedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import java.util.UUID

@Service
class TaskService(
  private val assessmentService: AssessmentService,
  private val applicationRepository: ApplicationRepository,
  private val userService: UserService
) {
  fun reallocateTask(requestUser: UserEntity, taskType: TaskType, userToAllocateToId: UUID, applicationId: UUID): AuthorisableActionResult<ValidatableActionResult<AssessmentEntity>> {
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

    return when (taskType) {
      TaskType.assessment -> {
        assessmentService.reallocateAssessment(assigneeUser, application)
      }
      else -> {
        throw NotAllowedProblem(detail = "The Task Type $taskType is not currently supported")
      }
    }
  }
}
