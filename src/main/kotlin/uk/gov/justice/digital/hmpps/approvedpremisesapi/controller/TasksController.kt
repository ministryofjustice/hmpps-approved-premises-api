package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.TasksApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AllocatedFilter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskWrapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.convert.EnumConverterFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TaskEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.TypedTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotAllowedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.TaskService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.TaskTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromAuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.kebabCaseToPascalCase
import java.util.UUID
import javax.transaction.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as ApiUserQualification

@Service
class TasksController(
  private val userService: UserService,
  private val assessmentService: AssessmentService,
  private val placementRequestService: PlacementRequestService,
  private val taskTransformer: TaskTransformer,
  private val offenderService: OffenderService,
  private val placementApplicationService: PlacementApplicationService,
  private val enumConverterFactory: EnumConverterFactory,
  private val userTransformer: UserTransformer,
  private val taskService: TaskService,
) : TasksApiDelegate {

  override fun tasksGet(
    type: TaskType?,
    types: List<TaskType>?,
    page: Int?,
    perPage: Int?,
    sortBy: TaskSortField?,
    sortDirection: SortDirection?,
    allocatedFilter: AllocatedFilter?,
    apAreaId: UUID?,
    allocatedToUserId: UUID?,
    requiredQualification: ApiUserQualification?,
    crnOrName: String?,
    isCompleted: Boolean?,
  ): ResponseEntity<List<Task>> {
    val user = userService.getUserForRequest()

    if (!user.hasAnyRole(UserRole.CAS1_WORKFLOW_MANAGER, UserRole.CAS1_MATCHER, UserRole.CAS1_ASSESSOR)) {
      throw ForbiddenProblem()
    }

    val taskEntityTypes = if (types != null) {
      types.map { toTaskEntityType(it) }
    } else if (type != null) {
      listOf(toTaskEntityType(type))
    } else {
      TaskEntityType.entries
    }

    val (typedTasks, metadata) = taskService.getAll(
      TaskService.TaskFilterCriteria(
        allocatedFilter = allocatedFilter,
        apAreaId = apAreaId,
        types = taskEntityTypes,
        allocatedToUserId = allocatedToUserId,
        requiredQualification = requiredQualification,
        crnOrName = crnOrName,
        isCompleted = isCompleted ?: false,
      ),
      PageCriteria(
        sortBy = sortBy ?: TaskSortField.createdAt,
        perPage = perPage,
        sortDirection = sortDirection ?: SortDirection.asc,
        page = page,
      ),
    )

    val offenderSummaries = getOffenderSummariesForCrns(typedTasks.map { it.crn }, user)
    val tasks = typedTasks.map {
      when (it) {
        is TypedTask.Assessment -> getAssessmentTask(it.entity, offenderSummaries)
        is TypedTask.PlacementRequest -> getPlacementRequestTask(it.entity, offenderSummaries)
        is TypedTask.PlacementApplication -> getPlacementApplicationTask(it.entity, offenderSummaries)
      }
    }

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(
      tasks,
    )
  }

  private fun toTaskEntityType(taskType: TaskType) = when (taskType) {
    TaskType.assessment -> TaskEntityType.ASSESSMENT
    TaskType.placementRequest -> TaskEntityType.PLACEMENT_REQUEST
    TaskType.placementApplication -> TaskEntityType.PLACEMENT_APPLICATION
    TaskType.bookingAppeal -> throw BadRequestProblem()
  }

  override fun tasksTaskTypeIdGet(id: UUID, taskType: String): ResponseEntity<TaskWrapper> {
    val user = userService.getUserForRequest()

    val taskInfo = when (toTaskType(taskType)) {
      TaskType.assessment -> {
        val assessment = extractEntityFromAuthorisableActionResult(
          assessmentService.getAssessmentForUser(user, id),
        ) as ApprovedPremisesAssessmentEntity
        val offenderSummaries = getOffenderSummariesForCrns(listOf(assessment.application.crn), user)

        val requiredPermission = if (assessment.createdFromAppeal) {
          UserPermission.CAS1_ASSESS_APPEALED_APPLICATION
        } else { UserPermission.CAS1_ASSESS_APPLICATION }

        TaskInfo(
          transformedTask = getAssessmentTask(assessment, offenderSummaries),
          crn = assessment.application.crn,
          requiredQualifications = assessment.application.getRequiredQualifications(),
          requiredPermission = requiredPermission,
        )
      }

      TaskType.placementRequest -> {
        val (placementRequest) = extractEntityFromAuthorisableActionResult(
          placementRequestService.getPlacementRequestForUser(user, id),
        )
        val offenderSummaries = getOffenderSummariesForCrns(listOf(placementRequest.application.crn), user)

        TaskInfo(
          transformedTask = getPlacementRequestTask(placementRequest, offenderSummaries),
          crn = placementRequest.application.crn,
          requiredQualifications = emptyList(),
          requiredPermission = UserPermission.CAS1_ASSESS_PLACEMENT_REQUEST,
        )
      }

      TaskType.placementApplication -> {
        val placementApplication = extractEntityFromAuthorisableActionResult(
          placementApplicationService.getApplication(id),
        )
        val offenderSummaries = getOffenderSummariesForCrns(listOf(placementApplication.application.crn), user)

        TaskInfo(
          transformedTask = getPlacementApplicationTask(placementApplication, offenderSummaries),
          crn = placementApplication.application.crn,
          requiredQualifications = placementApplication.application.getRequiredQualifications(),
          requiredPermission = UserPermission.CAS1_ASSESS_PLACEMENT_APPLICATION,
        )
      }

      else -> {
        throw NotAllowedProblem(detail = "The Task Type $taskType is not currently supported")
      }
    }

    val users = userService.getAllocatableUsersForAllocationType(
      taskInfo.crn,
      taskInfo.requiredQualifications,
      taskInfo.requiredPermission,
    )

    val workload = userService.getUserWorkloads(users.map { it.id })
    val transformedAllocatableUsers = users.map {
      userTransformer.transformJpaToAPIUserWithWorkload(it, workload[it.id]!!)
    }

    return ResponseEntity.ok(
      TaskWrapper(
        task = taskInfo.transformedTask,
        users = transformedAllocatableUsers,
      ),
    )
  }

  private data class TaskInfo(
    val transformedTask: Task,
    val crn: String,
    val requiredQualifications: List<UserQualification>,
    val requiredPermission: UserPermission,
  )

  @Transactional
  override fun tasksTaskTypeIdAllocationsPost(
    id: UUID,
    taskType: String,
    xServiceName: ServiceName,
    body: NewReallocation?,
  ): ResponseEntity<Reallocation> {
    val user = userService.getUserForRequest()

    val type = toTaskType(taskType)

    val userId = when {
      xServiceName == ServiceName.temporaryAccommodation -> user.id
      body?.userId == null -> throw BadRequestProblem(
        invalidParams = ValidationErrors(mutableMapOf("$.userId" to "empty")),
      )

      else -> body.userId
    }

    val validationResult = when (val authorisationResult = taskService.reallocateTask(user, type, userId, id)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(id, taskType.toString())
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> authorisationResult.entity
    }

    val reallocatedTask = when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(
        errorDetail = validationResult.message,
      )

      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(
        invalidParams = validationResult.validationMessages,
      )

      is ValidatableActionResult.ConflictError -> throw ConflictProblem(
        id = validationResult.conflictingEntityId,
        conflictReason = validationResult.message,
      )

      is ValidatableActionResult.Success -> validationResult.entity
    }

    return ResponseEntity(reallocatedTask, HttpStatus.CREATED)
  }

  @Transactional
  override fun tasksTaskTypeIdAllocationsDelete(id: UUID, taskType: String): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    val type = toTaskType(taskType)

    val validationResult = when (val authorisationResult = taskService.deallocateTask(user, type, id)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(id, taskType)
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> authorisationResult.entity
    }

    when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(
        errorDetail = validationResult.message,
      )

      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(
        invalidParams = validationResult.validationMessages,
      )

      is ValidatableActionResult.ConflictError -> throw ConflictProblem(
        id = validationResult.conflictingEntityId,
        conflictReason = validationResult.message,
      )

      is ValidatableActionResult.Success -> validationResult.entity
    }

    return ResponseEntity(Unit, HttpStatus.NO_CONTENT)
  }

  private fun getAssessmentTask(
    assessment: AssessmentEntity,
    offenderSummaries: List<PersonSummaryInfoResult>,
  ): AssessmentTask {
    return taskTransformer.transformAssessmentToTask(
      assessment = assessment,
      offenderSummaries = offenderSummaries,
    )
  }

  private fun getPlacementRequestTask(
    placementRequest: PlacementRequestEntity,
    offenderSummaries: List<PersonSummaryInfoResult>,
  ): PlacementRequestTask {
    return taskTransformer.transformPlacementRequestToTask(
      placementRequest = placementRequest,
      offenderSummaries = offenderSummaries,
    )
  }

  private fun getPlacementApplicationTask(
    placementApplication: PlacementApplicationEntity,
    offenderSummaries: List<PersonSummaryInfoResult>,
  ): PlacementApplicationTask {
    return taskTransformer.transformPlacementApplicationToTask(
      placementApplication = placementApplication,
      offenderSummaries = offenderSummaries,
    )
  }

  private fun getOffenderSummariesForCrns(crns: List<String>, user: UserEntity): List<PersonSummaryInfoResult> {
    return offenderService.getOffenderSummariesByCrns(
      crns.toSet(),
      user.deliusUsername,
      user.hasQualification(UserQualification.LAO),
    )
  }

  private fun toTaskType(type: String) = enumConverterFactory.getConverter(TaskType::class.java).convert(
    type.kebabCaseToPascalCase(),
  ) ?: throw NotFoundProblem(type, "TaskType")
}
