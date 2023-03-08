package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.TasksApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.TaskTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.mapAndTransformAssessments

@Service
class TasksController(
  private val userService: UserService,
  private val assessmentService: AssessmentService,
  private val taskTransformer: TaskTransformer,
  private val offenderService: OffenderService,
) : TasksApiDelegate {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun tasksGet(): ResponseEntity<List<Task>> {
    val user = userService.getUserForRequest()

    val assessments = mapAndTransformAssessments(
      log,
      assessmentService.getVisibleAssessmentsForUser(user),
      user.deliusUsername,
      offenderService,
      taskTransformer::transformAssessmentToTask
    ) as List<Task>

    return ResponseEntity.ok(assessments)
  }
}
