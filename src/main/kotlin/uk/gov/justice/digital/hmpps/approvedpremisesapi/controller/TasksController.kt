package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.TasksApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.TaskTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.mapAndTransformAssessments
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.mapAndTransformPlacementApplications
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.mapAndTransformPlacementRequests

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

  override fun tasksGet(): ResponseEntity<List<Task>> {
    val user = userService.getUserForRequest()

    if (user.hasRole(UserRole.CAS1_WORKFLOW_MANAGER)) {
      val assessments = mapAndTransformAssessments(
        log,
        assessmentService.getAllReallocatable(),
        user.deliusUsername,
        this.offenderService,
        taskTransformer::transformAssessmentToTask,
      )

      val placementRequests = mapAndTransformPlacementRequests(
        log,
        placementRequestService.getAllReallocatable(),
        user.deliusUsername,
        this.offenderService,
        taskTransformer::transformPlacementRequestToTask,
      )

      val placementApplications = mapAndTransformPlacementApplications(
        log,
        placementApplicationService.getAllReallocatable(),
        user.deliusUsername,
        this.offenderService,
        taskTransformer::transformPlacementApplicationToTask,
      )

      return ResponseEntity.ok(placementRequests + assessments + placementApplications)
    } else {
      throw ForbiddenProblem()
    }
  }
}
