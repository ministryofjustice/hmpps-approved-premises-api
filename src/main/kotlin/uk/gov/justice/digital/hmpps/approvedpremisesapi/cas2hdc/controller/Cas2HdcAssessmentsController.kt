package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2AssessmentStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.NewCas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.UpdateCas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcAssessmentNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcAssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcStatusUpdateService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcApplicationNotesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcAssessmentsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ParamDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import java.net.URI
import java.util.UUID

@Cas2HdcController
class Cas2HdcAssessmentsController(
  private val assessmentService: Cas2HdcAssessmentService,
  private val assessmentNoteService: Cas2HdcAssessmentNoteService,
  private val cas2HdcAssessmentsTransformer: Cas2HdcAssessmentsTransformer,
  private val cas2HdcApplicationNotesTransformer: Cas2HdcApplicationNotesTransformer,
  private val cas2HdcStatusUpdateService: Cas2HdcStatusUpdateService,
  private val cas2HdcUserService: Cas2HdcUserService,
) {

  @SuppressWarnings("ThrowsCount")
  @GetMapping("/assessments/{assessmentId}")
  fun assessmentsAssessmentIdGet(@PathVariable assessmentId: UUID): ResponseEntity<Cas2Assessment> {
    val assessment = when (
      val assessmentResult = assessmentService.getAssessment(assessmentId)
    ) {
      is AuthorisableActionResult.NotFound -> null
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> assessmentResult.entity
    }

    if (assessment != null) {
      return ResponseEntity.ok(cas2HdcAssessmentsTransformer.transformJpaToApiRepresentation(assessment))
    }

    throw NotFoundProblem(assessmentId, "Assessment")
  }

  @SuppressWarnings("ThrowsCount")
  @PutMapping("/assessments/{assessmentId}")
  fun assessmentsAssessmentIdPut(
    @PathVariable assessmentId: UUID,
    @RequestBody updateCas2Assessment: UpdateCas2Assessment,
  ): ResponseEntity<Cas2Assessment> {
    val assessmentResult = assessmentService.updateAssessment(assessmentId, updateCas2Assessment)
    val validationResult = when (assessmentResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(assessmentId, "Assessment")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> assessmentResult.entity
    }

    val updatedAssessment = when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages.mapValues { ParamDetails(it.value) })
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = validationResult.conflictingEntityId, conflictReason = validationResult.message)
      is ValidatableActionResult.Success -> validationResult.entity
    }

    return ResponseEntity.ok(
      cas2HdcAssessmentsTransformer.transformJpaToApiRepresentation(updatedAssessment),
    )
  }

  @PostMapping("/assessments/{assessmentId}/status-updates")
  fun assessmentsAssessmentIdStatusUpdatesPost(
    @PathVariable assessmentId: UUID,
    @RequestBody cas2AssessmentStatusUpdate: Cas2AssessmentStatusUpdate,
  ): ResponseEntity<Unit> {
    val result = cas2HdcStatusUpdateService.createForAssessment(
      assessmentId = assessmentId,
      statusUpdate = cas2AssessmentStatusUpdate,
      assessor = cas2HdcUserService.getUserForRequest(Cas2ServiceOrigin.HDC),
    )

    processAuthorisationFor(assessmentId, result)
      .run { processValidation(this as ValidatableActionResult<Cas2StatusUpdateEntity>) }

    return ResponseEntity(HttpStatus.CREATED)
  }

  @PostMapping("/assessments/{assessmentId}/notes")
  fun assessmentsAssessmentIdNotesPost(
    @PathVariable assessmentId: UUID,
    @RequestBody body: NewCas2ApplicationNote,
  ): ResponseEntity<Cas2ApplicationNote> {
    val noteResult = assessmentNoteService.createAssessmentNote(assessmentId, body)

    val validationResult = processAuthorisationFor(assessmentId, noteResult) as ValidatableActionResult<Cas2ApplicationNote>

    val note = processValidation(validationResult) as Cas2ApplicationNoteEntity

    return ResponseEntity
      .created(URI.create("/cas2-hdc/assessments/$assessmentId/notes/${note.id}"))
      .body(
        cas2HdcApplicationNotesTransformer.transformJpaToApi(note),
      )
  }

  private fun <EntityType> processAuthorisationFor(
    assessmentId: UUID,
    result: AuthorisableActionResult<ValidatableActionResult<EntityType>>,
  ): Any = when (result) {
    is AuthorisableActionResult.NotFound -> throw(NotFoundProblem(assessmentId, "Cas2Application"))
    is AuthorisableActionResult.Unauthorised -> throw(ForbiddenProblem())
    is AuthorisableActionResult.Success -> result.entity
  }

  private fun <EntityType : Any> processValidation(validationResult: ValidatableActionResult<EntityType>): Any = when (validationResult) {
    is ValidatableActionResult.GeneralValidationError -> throw(BadRequestProblem(errorDetail = validationResult.message))
    is ValidatableActionResult.FieldValidationError -> throw(BadRequestProblem(invalidParams = validationResult.validationMessages.mapValues { ParamDetails(it.value) }))
    is ValidatableActionResult.ConflictError -> throw(ConflictProblem(id = validationResult.conflictingEntityId, conflictReason = validationResult.message))
    is ValidatableActionResult.Success -> validationResult.entity
  }
}
