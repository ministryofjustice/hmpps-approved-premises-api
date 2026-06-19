package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.NewCas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.UpdateCas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2ApplicationNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2StatusUpdateService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.Cas2ApplicationNotesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.Cas2AssessmentsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.net.URI
import java.util.UUID

@Cas2Controller
class Cas2AssessmentsController(
  private val cas2AssessmentService: Cas2AssessmentService,
  private val cas2ApplicationNoteService: Cas2ApplicationNoteService,
  private val cas2AssessmentsTransformer: Cas2AssessmentsTransformer,
  private val cas2ApplicationNotesTransformer: Cas2ApplicationNotesTransformer,
  private val cas2StatusUpdateService: Cas2StatusUpdateService,
  private val cas2UserService: Cas2UserService,
) {

  @Operation(description = "Get an assessment. There are no constraints on assessments returned")
  @GetMapping("/assessments/{assessmentId}")
  fun assessmentsAssessmentIdGet(
    @PathVariable assessmentId: UUID,
  ): ResponseEntity<Cas2Assessment> {
    val assessmentResult = cas2AssessmentService.getAssessment(assessmentId)
    val cas2AssessmentEntity = extractEntityFromCasResult(assessmentResult)
    return ResponseEntity.ok(cas2AssessmentsTransformer.transformJpaToApiRepresentation(cas2AssessmentEntity))
  }

  @Operation(description = "Update an assessment. There are no constraints on who can access this endpoint")
  @PutMapping("/assessments/{assessmentId}")
  fun assessmentsAssessmentIdPut(
    @PathVariable assessmentId: UUID,
    @RequestBody updateCas2Assessment: UpdateCas2Assessment,
  ): ResponseEntity<Cas2Assessment> {
    val assessmentResult = cas2AssessmentService.updateAssessment(assessmentId, updateCas2Assessment)

    val cas2AssessmentEntity = extractEntityFromCasResult(assessmentResult)
    return ResponseEntity.ok(
      cas2AssessmentsTransformer.transformJpaToApiRepresentation(cas2AssessmentEntity),
    )
  }

  @PostMapping("/assessments/{assessmentId}/status-updates")
  fun assessmentsAssessmentIdStatusUpdatesPost(
    @PathVariable assessmentId: UUID,
    @RequestBody cas2AssessmentStatusUpdate: Cas2AssessmentStatusUpdate,
  ): ResponseEntity<Unit> {
    val result = cas2StatusUpdateService.createForAssessment(
      assessmentId = assessmentId,
      statusUpdate = cas2AssessmentStatusUpdate,
      assessor = cas2UserService.getUserForRequest(),
    )

    processAuthorisationFor(result).run { processValidation(result) }

    return ResponseEntity(HttpStatus.CREATED)
  }

  @PostMapping("/assessments/{assessmentId}/notes")
  fun assessmentsAssessmentIdNotesPost(
    @PathVariable assessmentId: UUID,
    @RequestBody body: NewCas2ApplicationNote,
  ): ResponseEntity<Cas2ApplicationNote> {
    val noteResult = cas2ApplicationNoteService.createAssessmentNote(assessmentId, body)

    val note = extractEntityFromCasResult(noteResult)
    return ResponseEntity.created(URI.create("/cas2v2/assessments/$assessmentId/notes/${note.id}"))
      .body(
        cas2ApplicationNotesTransformer.transformJpaToApi(note),
      )
  }

  private fun <EntityType> processAuthorisationFor(
    result: CasResult<EntityType>,
  ): Any? = extractEntityFromCasResult(result)

  private fun <EntityType : Any> processValidation(casResult: CasResult<EntityType>): Any = extractEntityFromCasResult(casResult)
}
