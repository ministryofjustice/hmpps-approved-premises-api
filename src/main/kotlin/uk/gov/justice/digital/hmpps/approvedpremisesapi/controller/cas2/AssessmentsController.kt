package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.zalando.problem.AbstractThrowableProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2.AssessmentsCas2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2AssessmentStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ExternalUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2AssessmentNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.StatusUpdateService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.ApplicationNotesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.AssessmentsTransformer
import java.net.URI
import java.util.UUID

@Service("Cas2AssessmentsController")
class AssessmentsController(
  private val assessmentService: Cas2AssessmentService,
  private val assessmentNoteService: Cas2AssessmentNoteService,
  private val assessmentsTransformer: AssessmentsTransformer,
  private val applicationNotesTransformer: ApplicationNotesTransformer,
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
    cas2AssessmentStatusUpdate: Cas2AssessmentStatusUpdate,
  ): ResponseEntity<Unit> {
    val result = statusUpdateService.createForAssessment(
      assessmentId = assessmentId,
      statusUpdate = cas2AssessmentStatusUpdate,
      assessor = externalUserService.getUserForRequest(),
    )

    processAuthorisationFor(assessmentId, result)
      .run { processValidation(this as ValidatableActionResult<Cas2StatusUpdateEntity>) }

    return ResponseEntity(HttpStatus.CREATED)
  }

  override fun assessmentsAssessmentIdNotesPost(
    assessmentId: UUID,
    body: NewCas2ApplicationNote,
  ): ResponseEntity<Cas2ApplicationNote> {
    val noteResult = assessmentNoteService.createAssessmentNote(assessmentId, body)

    val validationResult = processAuthorisationFor(assessmentId, noteResult) as ValidatableActionResult<Cas2ApplicationNote>

    val note = processValidation(validationResult) as Cas2ApplicationNoteEntity

    return ResponseEntity
      .created(URI.create("/cas2/assessments/$assessmentId/notes/${note.id}"))
      .body(
        applicationNotesTransformer.transformJpaToApi(note),
      )
  }

  private fun <EntityType> processAuthorisationFor(
    assessmentId: UUID,
    result: AuthorisableActionResult<ValidatableActionResult<EntityType>>,
  ): Any = when (result) {
    is AuthorisableActionResult.NotFound -> throwProblem(NotFoundProblem(assessmentId, "Cas2Application"))
    is AuthorisableActionResult.Unauthorised -> throwProblem(ForbiddenProblem())
    is AuthorisableActionResult.Success -> result.entity
  }

  private fun throwProblem(problem: AbstractThrowableProblem): Unit = throw problem

  private fun <EntityType : Any> processValidation(validationResult: ValidatableActionResult<EntityType>): Any = when (validationResult) {
    is ValidatableActionResult.GeneralValidationError -> throwProblem(BadRequestProblem(errorDetail = validationResult.message))
    is ValidatableActionResult.FieldValidationError -> throwProblem(BadRequestProblem(invalidParams = validationResult.validationMessages))
    is ValidatableActionResult.ConflictError -> throwProblem(ConflictProblem(id = validationResult.conflictingEntityId, conflictReason = validationResult.message))
    is ValidatableActionResult.Success -> validationResult.entity
  }
}
