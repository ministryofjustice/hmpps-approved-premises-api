package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2AssessmentStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas2v2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2v2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.Cas2v2ApplicationNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.Cas2v2AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.Cas2v2StatusUpdateService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.Cas2v2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.Cas2v2ApplicationNotesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.Cas2v2AssessmentsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.net.URI
import java.util.UUID

@Cas2v2Controller
class Cas2v2AssessmentsController(
  private val cas2v2AssessmentService: Cas2v2AssessmentService,
  private val cas2v2ApplicationNoteService: Cas2v2ApplicationNoteService,
  private val cas2v2AssessmentsTransformer: Cas2v2AssessmentsTransformer,
  private val cas2v2ApplicationNotesTransformer: Cas2v2ApplicationNotesTransformer,
  private val cas2v2StatusUpdateService: Cas2v2StatusUpdateService,
  private val cas2v2UserService: Cas2v2UserService,
) {
  @GetMapping("/assessments/{assessmentId}")
  fun assessmentsAssessmentIdGet(
    @PathVariable assessmentId: UUID,
  ): ResponseEntity<Cas2v2Assessment> {
    val assessmentResult = cas2v2AssessmentService.getAssessment(assessmentId)
    val cas2v2AssessmentEntity = extractEntityFromCasResult(assessmentResult)
    return ResponseEntity.ok(cas2v2AssessmentsTransformer.transformJpaToApiRepresentation(cas2v2AssessmentEntity))
  }

  @PutMapping("/assessments/{assessmentId}")
  fun assessmentsAssessmentIdPut(
    @PathVariable assessmentId: UUID,
    @RequestBody updateCas2Assessment: UpdateCas2v2Assessment,
  ): ResponseEntity<Cas2v2Assessment> {
    val assessmentResult = cas2v2AssessmentService.updateAssessment(assessmentId, updateCas2Assessment)

    val cas2v2AssessmentEntity = extractEntityFromCasResult(assessmentResult)
    return ResponseEntity.ok(
      cas2v2AssessmentsTransformer.transformJpaToApiRepresentation(cas2v2AssessmentEntity),
    )
  }

  @PostMapping("/assessments/{assessmentId}/status-updates")
  fun assessmentsAssessmentIdStatusUpdatesPost(
    @PathVariable assessmentId: UUID,
    @RequestBody cas2v2AssessmentStatusUpdate: Cas2v2AssessmentStatusUpdate,
  ): ResponseEntity<Unit> {
    val result = cas2v2StatusUpdateService.createForAssessment(
      assessmentId = assessmentId,
      statusUpdate = cas2v2AssessmentStatusUpdate,
      assessor = cas2v2UserService.getUserForRequest(),
    )

    processAuthorisationFor(result).run { processValidation(result) }

    return ResponseEntity(HttpStatus.CREATED)
  }

  @PostMapping("/assessments/{assessmentId}/notes")
  fun assessmentsAssessmentIdNotesPost(
    @PathVariable assessmentId: UUID,
    @RequestBody body: NewCas2v2ApplicationNote,
  ): ResponseEntity<Cas2v2ApplicationNote> {
    val noteResult = cas2v2ApplicationNoteService.createAssessmentNote(assessmentId, body)

    val note = extractEntityFromCasResult(noteResult)
    return ResponseEntity.created(URI.create("/cas2v2/assessments/$assessmentId/notes/${note.id}"))
      .body(
        cas2v2ApplicationNotesTransformer.transformJpaToApi(note),
      )
  }

  private fun <EntityType> processAuthorisationFor(
    result: CasResult<EntityType>,
  ): Any? = extractEntityFromCasResult(result)

  private fun <EntityType : Any> processValidation(casResult: CasResult<EntityType>): Any = extractEntityFromCasResult(casResult)
}
