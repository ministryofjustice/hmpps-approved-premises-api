package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserWithWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.convert.EnumConverterFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.AllocationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromAuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getNameFromPersonSummaryInfoResult
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
  override fun tasksReallocatableGet(
    type: String?,
    page: Int?,
    sortBy: TaskSortField?,
    sortDirection: SortDirection?,
    allocatedFilter: AllocatedFilter?,
  ): ResponseEntity<List<Task>> {
    val user = userService.getUserForRequest()
    val sortBy = sortBy ?: TaskSortField.createdAt
    val sortDirection = sortDirection ?: SortDirection.asc

    if (user.hasRole(UserRole.CAS1_WORKFLOW_MANAGER)) {
      if (type != null) {
        val taskType = enumConverterFactory.getConverter(TaskType::class.java).convert(
          type.kebabCaseToPascalCase(),
        ) ?: throw NotFoundProblem(type, "TaskType")

        return when (taskType) {
          TaskType.assessment -> assessmentTasksResponse(user, page, sortBy, sortDirection, allocatedFilter)
          TaskType.placementRequest -> placementRequestTasks(user, page, sortBy, sortDirection, allocatedFilter)
          TaskType.placementApplication -> placementApplicationTasks(user, page, sortBy, sortDirection, allocatedFilter)
          else -> {
            throw BadRequestProblem()
          }
        }
      }

      return responseForAllTypes(user, page, sortBy, sortDirection, allocatedFilter)
    } else {
      throw ForbiddenProblem()
    }
  }

  override fun tasksGet(): ResponseEntity<List<Task>> = runBlocking {
    val user = userService.getUserForRequest()
    var tasks = listOf<Task>()

    if (user.hasRole(UserRole.CAS1_MATCHER)) {
      val placementRequests =
        placementRequestService.getVisiblePlacementRequestsForUser(
          user,
          null,
          null,
        )

      val placementApplications =
        placementApplicationService.getVisiblePlacementApplicationsForUser(
          user,
          null,
          null,
        )

      val crns = listOf(
        placementRequests.first.map { it.application.crn },
        placementApplications.first.map { it.application.crn },
      ).flatten()

      val offenderSummaries = getOffenderSummariesForCrns(crns, user)

      tasks = listOf(
        getPlacementRequestTasks(
          placementRequests.first,
          offenderSummaries,
        ),
        getPlacementApplicationTasks(
          placementApplications.first,
          offenderSummaries,
        ),
      ).flatten()
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
          val crns = placementApplications.map { it.application.crn }
          val offenderSummaries = getOffenderSummariesForCrns(crns, user)

          paginationMetaData = metaData
          async {
            tasks += getPlacementApplicationTasks(
              placementApplications,
              offenderSummaries,
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
          val crns = placementRequests.map { it.application.crn }
          val offenderSummaries = getOffenderSummariesForCrns(crns, user)

          paginationMetaData = metaData
          async {
            tasks += getPlacementRequestTasks(
              placementRequests,
              offenderSummaries,
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
    val transformedAllocatableUsers: List<UserWithWorkload>

    when (type) {
      TaskType.assessment -> {
        val assessment = extractEntityFromAuthorisableActionResult(
          assessmentService.getAssessmentForUser(user, id),
        )
        val offenderSummaries = getOffenderSummariesForCrns(listOf(assessment.application.crn), user)

        transformedTask = getAssessmentTask(assessment) {
          getPersonNameFromApplication(it.application, offenderSummaries)
        }

        transformedAllocatableUsers = userService.getAllocatableUsersForAllocationType(
          assessment.application.crn,
          assessment.application.getRequiredQualifications(),
          AllocationType.Assessment,
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
        val offenderSummaries = getOffenderSummariesForCrns(listOf(placementRequest.application.crn), user)

        transformedTask = getPlacementRequestTask(placementRequest) {
          getPersonNameFromApplication(it.application, offenderSummaries)
        }

        transformedAllocatableUsers =
          userService.getAllocatableUsersForAllocationType(
            placementRequest.application.crn,
            emptyList(),
            AllocationType.PlacementRequest,
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
        val offenderSummaries = getOffenderSummariesForCrns(listOf(placementApplication.application.crn), user)

        transformedTask = getPlacementApplicationTask(placementApplication) {
          getPersonNameFromApplication(it.application, offenderSummaries)
        }

        transformedAllocatableUsers = userService.getAllocatableUsersForAllocationType(
          placementApplication.application.crn,
          placementApplication.application.getRequiredQualifications(),
          AllocationType.PlacementApplication,
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

  private fun getAssessmentTask(
    assessment: AssessmentEntity,
    personNameFunc: (AssessmentEntity) -> String,
  ): AssessmentTask {
    return taskTransformer.transformAssessmentToTask(
      assessment = assessment,
      personName = personNameFunc(assessment),
    )
  }

  private fun getPlacementRequestTask(
    placementRequest: PlacementRequestEntity,
    personNameFunc: (PlacementRequestEntity) -> String,
  ): PlacementRequestTask {
    return taskTransformer.transformPlacementRequestToTask(
      placementRequest = placementRequest,
      personName = personNameFunc(placementRequest),
    )
  }

  private fun getPlacementApplicationTask(
    placementApplication: PlacementApplicationEntity,
    personNameFunc: (PlacementApplicationEntity) -> String,
  ): PlacementApplicationTask {
    return taskTransformer.transformPlacementApplicationToTask(
      placementApplication = placementApplication,
      personName = personNameFunc(placementApplication),
    )
  }

  private suspend fun getAssessmentTasks(
    assessments: List<AssessmentEntity>,
    offenderSummaries: List<PersonSummaryInfoResult>,
  ) = assessments.map {
    getAssessmentTask(it) { assessment ->
      getPersonNameFromApplication(assessment.application, offenderSummaries)
    }
  }

  private suspend fun getPlacementRequestTasks(
    placementRequests: List<PlacementRequestEntity>,
    offenderSummaries: List<PersonSummaryInfoResult>,
  ) = placementRequests.map {
    getPlacementRequestTask(it) { placementRequest ->
      getPersonNameFromApplication(placementRequest.application, offenderSummaries)
    }
  }

  private suspend fun getPlacementApplicationTasks(
    placementApplications: List<PlacementApplicationEntity>,
    offenderSummaries: List<PersonSummaryInfoResult>,
  ) =
    placementApplications.map {
      getPlacementApplicationTask(it) { placementApplication ->
        getPersonNameFromApplication(placementApplication.application, offenderSummaries)
      }
    }

  private fun responseForAllTypes(
    user: UserEntity,
    page: Int?,
    sortBy: TaskSortField,
    sortDirection: SortDirection,
    allocatedFilter: AllocatedFilter?,
  ): ResponseEntity<List<Task>> = runBlocking {
    val (allocatedTasks, metadata) = taskService.getAllReallocatable(
      allocatedFilter,
      page,
      sortBy,
      sortDirection,
    )

    val crns = allocatedTasks.map { it.crn }

    val offenderSummaries = getOffenderSummariesForCrns(crns, user)

    val tasks = allocatedTasks.map {
      when (it) {
        is TypedTask.Assessment -> getAssessmentTask(it.entity) { assessment ->
          getPersonNameFromApplication(assessment.application, offenderSummaries)
        }
        is TypedTask.PlacementRequest -> getPlacementRequestTask(it.entity) { placementRequest ->
          getPersonNameFromApplication(placementRequest.application, offenderSummaries)
        }
        is TypedTask.PlacementApplication -> getPlacementApplicationTask(it.entity) { placementApplication ->
          getPersonNameFromApplication(placementApplication.application, offenderSummaries)
        }
      }
    }

    return@runBlocking ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(
      tasks,
    )
  }

  private fun assessmentTasksResponse(
    user: UserEntity,
    page: Int?,
    sortField: TaskSortField,
    sortDirection: SortDirection?,
    allocatedFilter: AllocatedFilter?,
  ): ResponseEntity<List<Task>> = runBlocking {
    val (allReallocatable, metaData) =
      assessmentService.getAllReallocatable(
        page,
        sortField,
        sortDirection,
        allocatedFilter,
      )
    val offenderSummaries = getOffenderSummariesForCrns(allReallocatable.map { it.application.crn }, user)
    val assessmentTasks = getAssessmentTasks(allReallocatable, offenderSummaries)
    return@runBlocking ResponseEntity.ok().headers(
      metaData?.toHeaders(),
    ).body(
      assessmentTasks,
    )
  }

  private fun placementRequestTasks(
    user: UserEntity,
    page: Int?,
    sortField: TaskSortField,
    sortDirection: SortDirection?,
    allocatedFilter: AllocatedFilter?,
  ): ResponseEntity<List<Task>> = runBlocking {
    val (allReallocatable, metaData) =
      placementRequestService.getAllReallocatable(
        page,
        sortField,
        sortDirection,
        allocatedFilter,
      )
    val offenderSummaries = getOffenderSummariesForCrns(allReallocatable.map { it.application.crn }, user)
    val placementRequestTasks = getPlacementRequestTasks(
      allReallocatable,
      offenderSummaries,
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
    sortField: TaskSortField,
    sortDirection: SortDirection?,
    allocatedFilter: AllocatedFilter?,
  ): ResponseEntity<List<Task>> = runBlocking {
    val (allReallocatable, metaData) =
      placementApplicationService.getAllReallocatable(
        page,
        sortField,
        sortDirection,
        allocatedFilter,
      )
    val offenderSummaries = getOffenderSummariesForCrns(allReallocatable.map { it.application.crn }, user)

    val placementApplicationTasks =
      getPlacementApplicationTasks(
        allReallocatable,
        offenderSummaries,
      )

    return@runBlocking ResponseEntity.ok().headers(
      metaData?.toHeaders(),
    ).body(
      placementApplicationTasks,
    )
  }

  private fun getPersonNameFromApplication(application: ApplicationEntity, offenderSummaries: List<PersonSummaryInfoResult>): String {
    val crn = application.crn
    val offenderSummary = offenderSummaries.first { it.crn == crn }
    return getNameFromPersonSummaryInfoResult(offenderSummary)
  }

  private fun getOffenderSummariesForCrns(crns: List<String>, user: UserEntity): List<PersonSummaryInfoResult> {
    return offenderService.getOffenderSummariesByCrns(
      crns.toSet(),
      user.deliusUsername,
      user.hasQualification(UserQualification.LAO),
      false,
    )
  }
}
