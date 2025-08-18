package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TaskEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TaskRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotAllowedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as ApiUserQualification

@Service
class Cas1TaskService(
  private val assessmentService: AssessmentService,
  private val userService: UserService,
  private val userAccessService: UserAccessService,
  private val userTransformer: UserTransformer,
  private val cas1PlacementApplicationService: Cas1PlacementApplicationService,
  private val taskRepository: TaskRepository,
  private val assessmentRepository: AssessmentRepository,
  private val placementApplicationRepository: PlacementApplicationRepository,
) {

  data class TaskFilterCriteria(
    val allocatedFilter: AllocatedFilter?,
    val apAreaId: UUID?,
    val cruManagementAreaId: UUID?,
    val types: List<TaskEntityType>,
    val allocatedToUserId: UUID?,
    val requiredQualification: ApiUserQualification?,
    val crnOrName: String?,
    val isCompleted: Boolean,
  )

  fun getAll(
    filterCriteria: TaskFilterCriteria,
    pageCriteria: PageCriteria<TaskSortField>,
  ): Pair<List<TypedTask>, PaginationMetadata?> {
    val taskTypes = filterCriteria.types

    val tasksResult = getAllEntities(filterCriteria, pageCriteria, taskTypes)
    val tasks = tasksResult.content

    val assessments = if (taskTypes.contains(TaskEntityType.ASSESSMENT)) {
      val assessmentIds = tasks.idsForType(TaskEntityType.ASSESSMENT)
      assessmentRepository.findAllById(assessmentIds).map { TypedTask.Assessment(it as ApprovedPremisesAssessmentEntity) }
    } else {
      emptyList()
    }

    val placementApplications = if (taskTypes.contains(TaskEntityType.PLACEMENT_APPLICATION)) {
      val placementApplicationIds = tasks.idsForType(TaskEntityType.PLACEMENT_APPLICATION)
      placementApplicationRepository.findAllById(placementApplicationIds).map { TypedTask.PlacementApplication(it) }
    } else {
      emptyList()
    }

    val typedTasks = tasks
      .map { task ->
        val candidateList = when (task.type) {
          TaskEntityType.ASSESSMENT -> assessments
          TaskEntityType.PLACEMENT_APPLICATION -> placementApplications
        }

        candidateList.first { it.id == task.id }
      }

    val metadata = getMetadata(tasksResult, pageCriteria)
    return Pair(typedTasks, metadata)
  }

  private fun getAllEntities(
    filterCriteria: TaskFilterCriteria,
    pageCriteria: PageCriteria<TaskSortField>,
    taskTypes: List<TaskEntityType>,
  ): Page<Task> {
    val pageable = pageCriteria.toPageable(
      when (pageCriteria.sortBy) {
        TaskSortField.createdAt -> "created_at"
        TaskSortField.dueAt -> "due_at"
        TaskSortField.allocatedTo -> "allocated_to"
        TaskSortField.person -> "person"
        TaskSortField.completedAt -> "completed_at"
        TaskSortField.taskType -> "type"
        TaskSortField.decision -> "decision"
        TaskSortField.expectedArrivalDate -> "sorting_date"
        TaskSortField.apType -> "ap_type"
      },
    )

    val allocatedFilter = filterCriteria.allocatedFilter

    val isAllocated = allocatedFilter?.let { allocatedFilter == AllocatedFilter.allocated }

    val repoFunction = if (taskTypes.size == 1) {
      when (taskTypes[0]) {
        TaskEntityType.ASSESSMENT -> taskRepository::getAllAssessments
        TaskEntityType.PLACEMENT_APPLICATION -> taskRepository::getAllPlacementApplications
      }
    } else {
      taskRepository::getAll
    }

    return repoFunction(
      isAllocated,
      filterCriteria.apAreaId,
      filterCriteria.cruManagementAreaId,
      taskTypes.map { it.name },
      filterCriteria.allocatedToUserId,
      filterCriteria.requiredQualification?.value,
      filterCriteria.crnOrName,
      filterCriteria.isCompleted,
      pageable,
    )
  }

  private fun List<Task>.idsForType(type: TaskEntityType) = this.filter { it.type == type }.map { it.id }

  @SuppressWarnings("ReturnCount", "CyclomaticComplexMethod")
  @Transactional
  fun reallocateTask(requestUser: UserEntity, taskType: TaskType, userToAllocateToId: UUID, taskId: UUID): CasResult<Reallocation> {
    if (!userAccessService.userCanReallocateTask(requestUser)) {
      return CasResult.Unauthorised()
    }

    val assigneeUserResult = userService.updateUserFromDelius(userToAllocateToId, ServiceName.approvedPremises)

    val assigneeUser =
      if (assigneeUserResult is CasResult.Success &&
        assigneeUserResult.value is UserService.GetUserResponse.Success
      ) {
        assigneeUserResult.value.user
      } else {
        return CasResult.NotFound("user", userToAllocateToId.toString())
      }

    val result = when (taskType) {
      TaskType.assessment -> {
        assessmentService.reallocateAssessment(
          allocatingUser = requestUser,
          assigneeUser = assigneeUser,
          id = taskId,
        )
      }
      TaskType.placementApplication -> {
        cas1PlacementApplicationService.reallocateApplication(assigneeUser, taskId)
      }
    }

    return when (result) {
      is CasResult.Success -> CasResult.Success(entityToReallocation(result.value, taskType))
      is CasResult.Error -> result.reviseType()
    }
  }

  @Deprecated("Not used by CAS1.  For CAS3, superseded by Cas3AssessmentService.deallocateAssessment()")
  fun deallocateTask(
    requestUser: UserEntity,
    taskType: TaskType,
    id: UUID,
  ): CasResult<Unit> {
    if (!userAccessService.userCanDeallocateTask(requestUser)) {
      return CasResult.Unauthorised()
    }

    val result = when (taskType) {
      TaskType.assessment -> assessmentService.deallocateAssessment(id)
      else -> throw NotAllowedProblem(detail = "The Task Type $taskType is not currently supported")
    }

    return when (result) {
      is CasResult.Success -> CasResult.Success(Unit)
      is CasResult.Error -> result.reviseType()
    }
  }

  fun getUserWorkloads(userIds: List<UUID>): Map<UUID, UserWorkload> = taskRepository.findWorkloadForUserIds(userIds).associate {
    it.getUserId() to UserWorkload(
      numTasksPending = listOf(
        it.getPendingAssessments(),
        it.getPendingPlacementApplications(),
      ).sum(),
      numTasksCompleted7Days = listOf(
        it.getCompletedAssessmentsInTheLastSevenDays(),
        it.getCompletedPlacementApplicationsInTheLastSevenDays(),
      ).sum(),
      numTasksCompleted30Days = listOf(
        it.getCompletedAssessmentsInTheLastThirtyDays(),
        it.getCompletedPlacementApplicationsInTheLastThirtyDays(),
      ).sum(),
    )
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  private fun entityToReallocation(entity: Any, taskType: TaskType): Reallocation {
    val allocatedToUser = when (entity) {
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

sealed class TypedTask(
  val id: UUID,
  val createdAt: OffsetDateTime,
  val crn: String,
) {
  data class Assessment(val entity: ApprovedPremisesAssessmentEntity) : TypedTask(entity.id, entity.createdAt, entity.application.crn)
  data class PlacementApplication(val entity: PlacementApplicationEntity) : TypedTask(entity.id, entity.createdAt, entity.application.crn)
}

data class UserWorkload(
  var numTasksPending: Int,
  var numTasksCompleted7Days: Int,
  var numTasksCompleted30Days: Int,
)
