package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2.AssessmentsCas2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.AssessmentsTransformer

@Service("Cas2AssessmentsController")
class AssessmentsController(
  private val assessmentService: AssessmentService,
  private val assessmentsTransformer: AssessmentsTransformer,
) : AssessmentsCas2Delegate {

  override fun assessmentsAssessmentIdPut(
    assessmentId: java.util.UUID,
    updateCas2Assessment: UpdateCas2Assessment,
  ):
    ResponseEntity<Cas2Assessment> {
    val assessmentResult = assessmentService.updateAssessment(assessmentId, updateCas2Assessment)
    val validationResult = when (assessmentResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(assessmentId, "Assessment")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> assessmentResult.entity
    }

    val updatedAssessment = when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = validationResult.conflictingEntityId, conflictReason = validationResult.message)
      is ValidatableActionResult.Success -> validationResult.entity
    }

    return ResponseEntity.ok(
      assessmentsTransformer.transformJpaToApiRepresentation(updatedAssessment),
    )
  }
}
