package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.TasksApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.TaskTransformer

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

    val assessments = assessmentService.getVisibleAssessmentsForUser(user).mapNotNull {
      val applicationCrn = it.application.crn

      val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(applicationCrn, user.deliusUsername)) {
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        is AuthorisableActionResult.NotFound -> {
          log.error("Could not get Offender Details for CRN: $applicationCrn")
          return@mapNotNull null
        }

        is AuthorisableActionResult.Unauthorised -> return@mapNotNull null
      }

      if (offenderDetails.otherIds.nomsNumber == null) {
        log.error("No NOMS number for CRN: $applicationCrn")
        return@mapNotNull null
      }

      val inmateDetails = when (val inmateDetailsResult = offenderService.getInmateDetailByNomsNumber(offenderDetails.otherIds.nomsNumber)) {
        is AuthorisableActionResult.Success -> inmateDetailsResult.entity
        is AuthorisableActionResult.NotFound -> {
          log.error("Could not get Inmate Details for NOMS number: ${offenderDetails.otherIds.nomsNumber}")
          return@mapNotNull null
        }

        is AuthorisableActionResult.Unauthorised -> return@mapNotNull null
      }

      taskTransformer.transformAssessmentToTask(it, offenderDetails, inmateDetails)
    }

    return ResponseEntity.ok(assessments)
  }
}
