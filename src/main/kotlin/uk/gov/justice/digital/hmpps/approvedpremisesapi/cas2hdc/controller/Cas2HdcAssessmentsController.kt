package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcAssessmentStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcNewApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcUpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcAssessmentNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcAssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcStatusUpdateService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcApplicationNotesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcAssessmentsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.ParamDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.ValidatableActionResult
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
  fun assessmentsAssessmentIdGet(@PathVariable assessmentId: UUID): ResponseEntity<Cas2HdcAssessment> {
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
    @RequestBody cas2HdcUpdateAssessment: Cas2HdcUpdateAssessment,
  ): ResponseEntity<Cas2HdcAssessment> {
    val assessmentResult = assessmentService.updateAssessment(assessmentId, cas2HdcUpdateAssessment)
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
    @RequestBody cas2HdcAssessmentStatusUpdate: Cas2HdcAssessmentStatusUpdate,
  ): ResponseEntity<Unit> {
    val result = cas2HdcStatusUpdateService.createForAssessment(
      assessmentId = assessmentId,
      statusUpdate = cas2HdcAssessmentStatusUpdate,
      assessor = cas2HdcUserService.getUserForRequest(Cas2ServiceOrigin.HDC),
    )

    processAuthorisationFor(assessmentId, result)
      .run { processValidation(this as ValidatableActionResult<Cas2StatusUpdateEntity>) }

    return ResponseEntity(HttpStatus.CREATED)
  }

  @PostMapping("/assessments/{assessmentId}/notes")
  fun assessmentsAssessmentIdNotesPost(
    @PathVariable assessmentId: UUID,
    @RequestBody body: Cas2HdcNewApplicationNote,
  ): ResponseEntity<Cas2HdcApplicationNote> {
    val noteResult = assessmentNoteService.createAssessmentNote(assessmentId, body)

    val validationResult = processAuthorisationFor(assessmentId, noteResult) as ValidatableActionResult<Cas2HdcApplicationNote>

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
