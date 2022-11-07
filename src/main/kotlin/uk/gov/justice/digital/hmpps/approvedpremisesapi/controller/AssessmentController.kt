package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.AssessmentsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import java.util.UUID

@Service
class AssessmentController(
  private val assessmentService: AssessmentService,
  private val userService: UserService,
  private val offenderService: OffenderService,
  private val assessmentTransformer: AssessmentTransformer
) : AssessmentsApiDelegate {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun assessmentsGet(): ResponseEntity<List<Assessment>> {
    val user = userService.getUserForRequest()

    val assessments = assessmentService.getVisibleAssessmentsForUser(user)

    return ResponseEntity.ok(
      assessments.mapNotNull {
        val applicationCrn = it.application.crn

        val offenderDetailsResult = offenderService.getOffenderByCrn(applicationCrn, user.deliusUsername)
        val offenderDetails = when (offenderDetailsResult) {
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

        val inmateDetailsResult = offenderService.getInmateDetailByNomsNumber(offenderDetails.otherIds.nomsNumber)
        val inmateDetails = when (inmateDetailsResult) {
          is AuthorisableActionResult.Success -> inmateDetailsResult.entity
          is AuthorisableActionResult.NotFound -> {
            log.error("Could not get Inmate Details for NOMS number: ${offenderDetails.otherIds.nomsNumber}")
            return@mapNotNull null
          }
          is AuthorisableActionResult.Unauthorised -> return@mapNotNull null
        }

        assessmentTransformer.transformJpaToApi(it, offenderDetails, inmateDetails)
      }
    )
  }

  override fun assessmentsAssessmentIdGet(assessmentId: UUID): ResponseEntity<Assessment> {
    val user = userService.getUserForRequest()

    val assessmentResult = assessmentService.getAssessmentForUser(user, assessmentId)
    val assessment = when (assessmentResult) {
      is AuthorisableActionResult.Success -> assessmentResult.entity
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(assessmentId, "Assessment")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    }

    val applicationCrn = assessment.application.crn

    val offenderDetailsResult = offenderService.getOffenderByCrn(applicationCrn, user.deliusUsername)
    val offenderDetails = when (offenderDetailsResult) {
      is AuthorisableActionResult.Success -> offenderDetailsResult.entity
      else -> throw InternalServerErrorProblem("Could not get Offender Details for CRN: $applicationCrn")
    }

    if (offenderDetails.otherIds.nomsNumber == null) {
      throw InternalServerErrorProblem("No NOMS number for CRN: $applicationCrn")
    }

    val inmateDetailsResult = offenderService.getInmateDetailByNomsNumber(offenderDetails.otherIds.nomsNumber)
    val inmateDetails = when (inmateDetailsResult) {
      is AuthorisableActionResult.Success -> inmateDetailsResult.entity
      else -> throw InternalServerErrorProblem("Could not get Inmate Details for NOMS: ${offenderDetails.otherIds.nomsNumber}")
    }

    return ResponseEntity.ok(
      assessmentTransformer.transformJpaToApi(assessment, offenderDetails, inmateDetails)
    )
  }
}
