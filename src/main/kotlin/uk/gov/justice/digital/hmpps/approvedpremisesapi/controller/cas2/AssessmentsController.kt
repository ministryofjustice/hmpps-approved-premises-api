package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.zalando.problem.AbstractThrowableProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2.AssessmentsCas2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2AssessmentStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ExternalUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.StatusUpdateService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.AssessmentsTransformer
import java.util.UUID

@Service("Cas2AssessmentsController")
class AssessmentsController(
  private val assessmentService: AssessmentService,
  private val assessmentsTransformer: AssessmentsTransformer,
  private val statusUpdateService: StatusUpdateService,
  private val externalUserService: ExternalUserService,
) : AssessmentsCas2Delegate {

  override fun assessmentsAssessmentIdGet(assessmentId: UUID): ResponseEntity<Cas2Assessment> {
    val assessment = when (
      val assessmentResult = assessmentService.getAssessment(assessmentId)
    ) {
      is AuthorisableActionResult.NotFound -> null
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> assessmentResult.entity
    }

    if (assessment != null) {
      return ResponseEntity.ok(assessmentsTransformer.transformJpaToApiRepresentation(assessment))
    }

    throw NotFoundProblem(assessmentId, "Assessment")
  }

  override fun assessmentsAssessmentIdPut(
    assessmentId: java.util.UUID,
    updateCas2Assessment: UpdateCas2Assessment,
  ): ResponseEntity<Cas2Assessment> {
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

  override fun assessmentsAssessmentIdStatusUpdatesPost(
    assessmentId: UUID,
    statusUpdate: Cas2AssessmentStatusUpdate,
  ): ResponseEntity<Unit> {
    val result = statusUpdateService.createForAssessment(
      assessmentId = assessmentId,
      statusUpdate = statusUpdate,
      assessor = externalUserService.getUserForRequest(),
    )

    processAuthorisationFor(assessmentId, result)
      .run { processValidation(this as ValidatableActionResult<Cas2StatusUpdateEntity>) }

    return ResponseEntity(HttpStatus.CREATED)
  }

  private fun <EntityType> processAuthorisationFor(
    assessmentId: UUID,
    result: AuthorisableActionResult<ValidatableActionResult<EntityType>>,
  ): Any {
    return when (result) {
      is AuthorisableActionResult.NotFound -> throwProblem(NotFoundProblem(assessmentId, "Cas2Application"))
      is AuthorisableActionResult.Unauthorised -> throwProblem(ForbiddenProblem())
      is AuthorisableActionResult.Success -> result.entity
    }
  }

  private fun throwProblem(problem: AbstractThrowableProblem) {
    throw problem
  }

  private fun <EntityType : Any> processValidation(validationResult: ValidatableActionResult<EntityType>): Any {
    return when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throwProblem(BadRequestProblem(errorDetail = validationResult.message))
      is ValidatableActionResult.FieldValidationError -> throwProblem(BadRequestProblem(invalidParams = validationResult.validationMessages))
      is ValidatableActionResult.ConflictError -> throwProblem(ConflictProblem(id = validationResult.conflictingEntityId, conflictReason = validationResult.message))
      is ValidatableActionResult.Success -> validationResult.entity
    }
  }
}
