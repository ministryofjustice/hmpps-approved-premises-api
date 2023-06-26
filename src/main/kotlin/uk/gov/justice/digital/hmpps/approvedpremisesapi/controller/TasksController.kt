package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.TasksApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.TaskTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getNameFromOffenderDetailSummaryResult

@Service
class TasksController(
  private val userService: UserService,
  private val assessmentService: AssessmentService,
  private val placementRequestService: PlacementRequestService,
  private val taskTransformer: TaskTransformer,
  private val offenderService: OffenderService,
  private val placementApplicationService: PlacementApplicationService,
) : TasksApiDelegate {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun tasksReallocatableGet(): ResponseEntity<List<Task>> {
    val user = userService.getUserForRequest()

    if (user.hasRole(UserRole.CAS1_WORKFLOW_MANAGER)) {
      val assessmentTasks = getAssessmentTasks(assessmentService.getAllReallocatable(), user)
      val placementRequestTasks = getPlacementRequestTasks(placementRequestService.getAllReallocatable(), user)
      val placementApplicationTasks = getPlacementApplicationTasks(placementApplicationService.getAllReallocatable(), user)

      return ResponseEntity.ok(assessmentTasks + placementRequestTasks + placementApplicationTasks)
    } else {
      throw ForbiddenProblem()
    }
  }

  override fun tasksGet(): ResponseEntity<List<Task>> {
    val user = userService.getUserForRequest()
    val tasks = mutableListOf<Task>()

    if (user.hasRole(UserRole.CAS1_MATCHER)) {
      tasks += getPlacementRequestTasks(placementRequestService.getVisiblePlacementRequestsForUser(user), user)

      tasks += getPlacementApplicationTasks(placementApplicationService.getVisiblePlacementApplicationsForUser(user), user)
    }

    return ResponseEntity.ok(tasks)
  }

  private fun getAssessmentTasks(assessments: List<AssessmentEntity>, user: UserEntity) = assessments.map {
    val offenderDetailsResult = offenderService.getOffenderByCrn(it.application.crn, user.deliusUsername)

    taskTransformer.transformAssessmentToTask(
      assessment = it,
      personName = getNameFromOffenderDetailSummaryResult(offenderDetailsResult),
    )
  }

  private fun getPlacementRequestTasks(placementRequests: List<PlacementRequestEntity>, user: UserEntity) = placementRequests.map {
    val offenderDetailsResult = offenderService.getOffenderByCrn(it.application.crn, user.deliusUsername)

    taskTransformer.transformPlacementRequestToTask(
      placementRequest = it,
      personName = getNameFromOffenderDetailSummaryResult(offenderDetailsResult),
    )
  }

  private fun getPlacementApplicationTasks(placementApplications: List<PlacementApplicationEntity>, user: UserEntity) = placementApplications.map {
    val offenderDetailsResult = offenderService.getOffenderByCrn(it.application.crn, user.deliusUsername)

    taskTransformer.transformPlacementApplicationToTask(
      placementApplication = it,
      personName = getNameFromOffenderDetailSummaryResult(offenderDetailsResult),
    )
  }
}
