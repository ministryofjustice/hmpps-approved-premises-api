package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskWrapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserWithWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.convert.EnumConverterFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromAuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getNameFromOffenderDetailSummaryResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.kebabCaseToPascalCase
import java.util.UUID
import javax.transaction.Transactional

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
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun tasksReallocatableGet(
    type: String?,
    page: Int?,
    sortDirection: SortDirection?,
    allocatedFilter: AllocatedFilter?,
  ): ResponseEntity<List<Task>> {
    val user = userService.getUserForRequest()
    if (user.hasRole(UserRole.CAS1_WORKFLOW_MANAGER)) {
      if (type != null) {
        val taskType = enumConverterFactory.getConverter(TaskType::class.java).convert(
          type.kebabCaseToPascalCase(),
        ) ?: throw NotFoundProblem(type, "TaskType")

        when (taskType) {
          TaskType.assessment -> return assessmentTasksResponse(user, page, sortDirection, allocatedFilter)
          TaskType.placementRequest -> return placementRequestTasks(user, page, sortDirection, allocatedFilter)
          TaskType.placementApplication -> return placementApplicationTasks(user, page, sortDirection, allocatedFilter)
          else -> {
            throw BadRequestProblem()
          }
        }
      }

      return responseForAllTypes(user, allocatedFilter)
    } else {
      throw ForbiddenProblem()
    }
  }

  override fun tasksGet(): ResponseEntity<List<Task>> = runBlocking {
    val user = userService.getUserForRequest()
    val tasks = mutableListOf<Task>()

    if (user.hasRole(UserRole.CAS1_MATCHER)) {
      async {
        val placementRequests =
          placementRequestService.getVisiblePlacementRequestsForUser(
            user,
            null,
            null,
          )
        tasks += getPlacementRequestTasks(
          placementRequests.first,
          user,
        )
      }

      async {
        val placementApplications =
          placementApplicationService.getVisiblePlacementApplicationsForUser(
            user,
            null,
            null,
          )
        tasks += getPlacementApplicationTasks(
          placementApplications.first,
          user,
        )
      }
    }

    return@runBlocking ResponseEntity.ok(tasks)
  }

  override fun tasksTaskTypeGet(
    taskType: String,
    page: Int?,
    sortDirection: SortDirection?,
  ): ResponseEntity<List<Task>> = runBlocking {
    val user = userService.getUserForRequest()
    val tasks = mutableListOf<Task>()
    val type = enumConverterFactory.getConverter(TaskType::class.java).convert(
      taskType.kebabCaseToPascalCase(),
    ) ?: throw NotFoundProblem(taskType, "TaskType")

    var paginationMetaData: PaginationMetadata? = null

    if (user.hasRole(UserRole.CAS1_MATCHER)) {
      when (type) {
        TaskType.placementApplication -> {
          val (placementApplications, metaData) =
            placementApplicationService.getVisiblePlacementApplicationsForUser(
              user,
              page,
              sortDirection,
            )
          paginationMetaData = metaData
          async {
            tasks += getPlacementApplicationTasks(
              placementApplications,
              user,
            )
          }
        }
        TaskType.placementRequest -> {
          val (placementRequests, metaData) =
            placementRequestService.getVisiblePlacementRequestsForUser(
              user,
              page,
              sortDirection,
            )
          paginationMetaData = metaData
          async {
            tasks += getPlacementRequestTasks(
              placementRequests,
              user,
            )
          }
        }
        else -> {
          throw BadRequestProblem()
        }
      }
    }

    return@runBlocking ResponseEntity.ok().headers(
      paginationMetaData?.toHeaders(),
    ).body(
      tasks,
    )
  }

  override fun tasksTaskTypeIdGet(id: UUID, taskType: String): ResponseEntity<TaskWrapper> {
    val user = userService.getUserForRequest()
    val type = enumConverterFactory.getConverter(TaskType::class.java).convert(
      taskType.kebabCaseToPascalCase(),
    ) ?: throw NotFoundProblem(taskType, "TaskType")

    val transformedTask: Task
    var transformedAllocatableUsers: List<UserWithWorkload>

    when (type) {
      TaskType.assessment -> {
        val assessment = extractEntityFromAuthorisableActionResult(
          assessmentService.getAssessmentForUser(user, id),
        )

        transformedTask = getAssessmentTask(assessment, user)

        transformedAllocatableUsers = userService.getUsersWithQualificationsAndRolesPassingLAO(
          assessment.application.crn,
          assessment.application.getRequiredQualifications(),
          listOf(UserRole.CAS1_ASSESSOR),
        )
          .map {
            val workload = userService.getUserWorkload(it.id)
            userTransformer.transformJpaToAPIUserWithWorkload(it, workload)
          }
      }
      TaskType.placementRequest -> {
        val (placementRequest) = extractEntityFromAuthorisableActionResult(
          placementRequestService.getPlacementRequestForUser(user, id),
        )

        transformedTask = getPlacementRequestTask(placementRequest, user)

        transformedAllocatableUsers =
          userService.getUsersWithQualificationsAndRolesPassingLAO(
            placementRequest.application.crn,
            emptyList(),
            listOf(UserRole.CAS1_MATCHER),
          )
            .map {
              val workload = userService.getUserWorkload(it.id)
              userTransformer.transformJpaToAPIUserWithWorkload(it, workload)
            }
      }
      TaskType.placementApplication -> {
        val placementApplication = extractEntityFromAuthorisableActionResult(
          placementApplicationService.getApplication(id),
        )

        transformedTask = getPlacementApplicationTask(placementApplication, user)

        transformedAllocatableUsers = userService.getUsersWithQualificationsAndRolesPassingLAO(
          placementApplication.application.crn,
          placementApplication.application.getRequiredQualifications(),
          listOf(UserRole.CAS1_ASSESSOR),
        )
          .map {
            val workload = userService.getUserWorkload(it.id)
            userTransformer.transformJpaToAPIUserWithWorkload(it, workload)
          }
      } else -> {
        throw NotAllowedProblem(detail = "The Task Type $taskType is not currently supported")
      }
    }

    return ResponseEntity.ok(
      TaskWrapper(
        task = transformedTask,
        users = transformedAllocatableUsers,
      ),
    )
  }

  @Transactional
  override fun tasksTaskTypeIdAllocationsPost(
    id: UUID,
    taskType: String,
    xServiceName: ServiceName,
    body: NewReallocation?,
  ): ResponseEntity<Reallocation> {
    val user = userService.getUserForRequest()

    val type = enumConverterFactory.getConverter(TaskType::class.java).convert(
      taskType.kebabCaseToPascalCase(),
    ) ?: throw NotFoundProblem(taskType, "TaskType")

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

    val type = enumConverterFactory.getConverter(TaskType::class.java).convert(
      taskType.kebabCaseToPascalCase(),
    ) ?: throw NotFoundProblem(taskType, "TaskType")

    val validationResult = when (val authorisationResult = taskService.deallocateTask(user, type, id)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(id, taskType.toString())
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

  private fun getAssessmentTask(assessment: AssessmentEntity, user: UserEntity): AssessmentTask {
    val offenderDetailsResult =
      offenderService.getOffenderByCrn(
        assessment.application.crn,
        user.deliusUsername,
        user.hasQualification(UserQualification.LAO),
      )

    return taskTransformer.transformAssessmentToTask(
      assessment = assessment,
      personName = getNameFromOffenderDetailSummaryResult(offenderDetailsResult),
    )
  }

  private fun getPlacementRequestTask(
    placementRequest: PlacementRequestEntity,
    user: UserEntity,
  ): PlacementRequestTask {
    val offenderDetailsResult =
      offenderService.getOffenderByCrn(
        placementRequest.application.crn,
        user.deliusUsername,
        user.hasQualification(UserQualification.LAO),
      )

    return taskTransformer.transformPlacementRequestToTask(
      placementRequest = placementRequest,
      personName = getNameFromOffenderDetailSummaryResult(offenderDetailsResult),
    )
  }

  private fun getPlacementApplicationTask(
    placementApplication: PlacementApplicationEntity,
    user: UserEntity,
  ): PlacementApplicationTask {
    val offenderDetailsResult =
      offenderService.getOffenderByCrn(
        placementApplication.application.crn,
        user.deliusUsername,
        user.hasQualification(UserQualification.LAO),
      )

    return taskTransformer.transformPlacementApplicationToTask(
      placementApplication = placementApplication,
      personName = getNameFromOffenderDetailSummaryResult(offenderDetailsResult),
    )
  }

  private suspend fun getAssessmentTasks(
    assessments: List<AssessmentEntity>,
    user: UserEntity,
  ) = assessments.map {
    getAssessmentTask(it, user)
  }

  private suspend fun getPlacementRequestTasks(
    placementRequests: List<PlacementRequestEntity>,
    user: UserEntity,
  ) = placementRequests.map {
    getPlacementRequestTask(it, user)
  }

  private suspend fun getPlacementApplicationTasks(
    placementApplications: List<PlacementApplicationEntity>,
    user: UserEntity,
  ) =
    placementApplications.map {
      getPlacementApplicationTask(it, user)
    }

  private fun responseForAllTypes(
    user: UserEntity,
    allocatedFilter: AllocatedFilter?,
  ): ResponseEntity<List<Task>> = runBlocking {
    val (allReallocatableAssessmentTasks, _) =
      assessmentService.getAllReallocatable(
        null,
        null,
        allocatedFilter,
      )
    val assessmentTasks = getAssessmentTasks(allReallocatableAssessmentTasks, user)

    val (allReallocatablePlacementRequestTasks, _) =
      placementRequestService.getAllReallocatable(
        null,
        null,
        allocatedFilter,
      )
    val placementRequestTasks = getPlacementRequestTasks(
      allReallocatablePlacementRequestTasks,
      user,
    )

    val (allReallocatablePlacementApplicationTasks, _) =
      placementApplicationService.getAllReallocatable(
        null,
        null,
        allocatedFilter,
      )
    val placementApplicationTasks =
      getPlacementApplicationTasks(
        allReallocatablePlacementApplicationTasks,
        user,
      )

    val tasks: MutableList<Task> = ArrayList()
    async { tasks.addAll(assessmentTasks) }
    async { tasks.addAll(placementRequestTasks) }
    async { tasks.addAll(placementApplicationTasks) }
    return@runBlocking ResponseEntity.ok(tasks)
  }

  private fun assessmentTasksResponse(
    user: UserEntity,
    page: Int?,
    sortDirection: SortDirection?,
    allocatedFilter: AllocatedFilter?,
  ): ResponseEntity<List<Task>> = runBlocking {
    val (allReallocatable, metaData) =
      assessmentService.getAllReallocatable(
        page,
        sortDirection,
        allocatedFilter,
      )
    val assessmentTasks = getAssessmentTasks(allReallocatable, user)
    return@runBlocking ResponseEntity.ok().headers(
      metaData?.toHeaders(),
    ).body(
      assessmentTasks,
    )
  }

  private fun placementRequestTasks(
    user: UserEntity,
    page: Int?,
    sortDirection: SortDirection?,
    allocatedFilter: AllocatedFilter?,
  ): ResponseEntity<List<Task>> = runBlocking {
    val (allReallocatable, metaData) =
      placementRequestService.getAllReallocatable(
        page,
        sortDirection,
        allocatedFilter,
      )
    val placementRequestTasks = getPlacementRequestTasks(
      allReallocatable,
      user,
    )
    return@runBlocking ResponseEntity.ok().headers(
      metaData?.toHeaders(),
    ).body(
      placementRequestTasks,
    )
  }

  private fun placementApplicationTasks(
    user: UserEntity,
    page: Int?,
    sortDirection: SortDirection?,
    allocatedFilter: AllocatedFilter?,
  ): ResponseEntity<List<Task>> = runBlocking {
    val (allReallocatable, metaData) =
      placementApplicationService.getAllReallocatable(
        page,
        sortDirection,
        allocatedFilter,
      )
    val placementApplicationTasks =
      getPlacementApplicationTasks(
        allReallocatable,
        user,
      )

    return@runBlocking ResponseEntity.ok().headers(
      metaData?.toHeaders(),
    ).body(
      placementApplicationTasks,
    )
  }
}
