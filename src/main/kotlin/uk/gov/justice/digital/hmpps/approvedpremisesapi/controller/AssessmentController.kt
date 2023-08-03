package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.AssessmentsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentRejection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortOrder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatedClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentClarificationNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPersonDetailsForCrn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.mapAndTransformAssessmentSummaries
import java.util.UUID
import javax.transaction.Transactional

@Service
class AssessmentController(
  private val objectMapper: ObjectMapper,
  private val assessmentService: AssessmentService,
  private val userService: UserService,
  private val offenderService: OffenderService,
  private val assessmentTransformer: AssessmentTransformer,
  private val assessmentClarificationNoteTransformer: AssessmentClarificationNoteTransformer,
) : AssessmentsApiDelegate {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Suppress("NAME_SHADOWING")
  override fun assessmentsGet(
    xServiceName: ServiceName,
    sortOrder: SortOrder?,
    sortField: AssessmentSortField?,
    statuses: List<AssessmentStatus>?,
  ): ResponseEntity<List<AssessmentSummary>> {
    val user = userService.getUserForRequest()

    val summaries = assessmentService.getVisibleAssessmentSummariesForUser(user, xServiceName)

    val sortOrder = when {
      xServiceName == ServiceName.temporaryAccommodation && sortOrder == null -> SortOrder.ascending
      else -> sortOrder
    }

    val sortField = when {
      xServiceName == ServiceName.temporaryAccommodation && sortField == null -> AssessmentSortField.assessmentArrivalDate
      else -> sortField
    }

    return ResponseEntity.ok(
      mapAndTransformAssessmentSummaries(
        log,
        summaries,
        user.deliusUsername,
        offenderService,
        assessmentTransformer::transformDomainToApiSummary,
        user.hasQualification(UserQualification.LAO),
        sortOrder,
        sortField,
        statuses,
      ),
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

    val (offenderDetails, inmateDetails) = getPersonDetailsForCrn(log, applicationCrn, user.deliusUsername, offenderService, user.hasQualification(UserQualification.LAO)) ?: throw InternalServerErrorProblem("Unable to get Person via crn: $applicationCrn")

    return ResponseEntity.ok(
      assessmentTransformer.transformJpaToApi(assessment, offenderDetails, inmateDetails),
    )
  }

  @Transactional
  override fun assessmentsAssessmentIdPut(assessmentId: UUID, updateAssessment: UpdateAssessment): ResponseEntity<Assessment> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(updateAssessment.data)

    val assessmentAuthResult = assessmentService.updateAssessment(user, assessmentId, serializedData)
    val assessmentValidationResult = when (assessmentAuthResult) {
      is AuthorisableActionResult.Success -> assessmentAuthResult.entity
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(assessmentId, "Assessment")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    }

    val assessment = when (assessmentValidationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = assessmentValidationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = assessmentValidationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = assessmentValidationResult.conflictingEntityId, conflictReason = assessmentValidationResult.message)
      is ValidatableActionResult.Success -> assessmentValidationResult.entity
    }

    val applicationCrn = assessment.application.crn

    val (offenderDetails, inmateDetails) = getPersonDetailsForCrn(log, applicationCrn, user.deliusUsername, offenderService, user.hasQualification(UserQualification.LAO)) ?: throw InternalServerErrorProblem("Unable to get Person via crn: $applicationCrn")

    return ResponseEntity.ok(
      assessmentTransformer.transformJpaToApi(assessment, offenderDetails, inmateDetails),
    )
  }

  @Transactional
  override fun assessmentsAssessmentIdAcceptancePost(assessmentId: UUID, assessmentAcceptance: AssessmentAcceptance): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(assessmentAcceptance.document)

    val assessmentAuthResult = assessmentService.acceptAssessment(
      user = user,
      assessmentId = assessmentId,
      document = serializedData,
      placementRequirements = assessmentAcceptance.requirements,
      placementDates = assessmentAcceptance.placementDates,
      notes = assessmentAcceptance.notes,
    )

    val assessmentValidationResult = when (assessmentAuthResult) {
      is AuthorisableActionResult.Success -> assessmentAuthResult.entity
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(assessmentId, "Assessment")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    }

    when (assessmentValidationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = assessmentValidationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = assessmentValidationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = assessmentValidationResult.conflictingEntityId, conflictReason = assessmentValidationResult.message)
      is ValidatableActionResult.Success -> assessmentValidationResult.entity
    }

    return ResponseEntity(HttpStatus.OK)
  }

  @Transactional
  override fun assessmentsAssessmentIdRejectionPost(assessmentId: UUID, assessmentRejection: AssessmentRejection): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(assessmentRejection.document)

    val assessmentAuthResult = assessmentService.rejectAssessment(user, assessmentId, serializedData, assessmentRejection.rejectionRationale)

    val assessmentValidationResult = when (assessmentAuthResult) {
      is AuthorisableActionResult.Success -> assessmentAuthResult.entity
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(assessmentId, "Assessment")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    }

    when (assessmentValidationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = assessmentValidationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = assessmentValidationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = assessmentValidationResult.conflictingEntityId, conflictReason = assessmentValidationResult.message)
      is ValidatableActionResult.Success -> Unit
    }

    return ResponseEntity(HttpStatus.OK)
  }

  override fun assessmentsAssessmentIdClosurePost(assessmentId: UUID): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    val assessmentValidationResult = when (val assessmentAuthResult = assessmentService.closeAssessment(user, assessmentId)) {
      is AuthorisableActionResult.Success -> assessmentAuthResult.entity
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(assessmentId, "Assessment")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    }

    when (assessmentValidationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = assessmentValidationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = assessmentValidationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = assessmentValidationResult.conflictingEntityId, conflictReason = assessmentValidationResult.message)
      is ValidatableActionResult.Success -> Unit
    }

    return ResponseEntity(HttpStatus.OK)
  }

  override fun assessmentsAssessmentIdNotesPost(
    assessmentId: UUID,
    newClarificationNote: NewClarificationNote,
  ): ResponseEntity<ClarificationNote> {
    val user = userService.getUserForRequest()

    val clarificationNoteResult = assessmentService.addAssessmentClarificationNote(user, assessmentId, newClarificationNote.query)
    val clarificationNote = when (clarificationNoteResult) {
      is AuthorisableActionResult.Success -> clarificationNoteResult.entity
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(assessmentId, "Assessment")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    }

    return ResponseEntity.ok(
      assessmentClarificationNoteTransformer.transformJpaToApi(clarificationNote),
    )
  }

  override fun assessmentsAssessmentIdNotesNoteIdPut(
    assessmentId: UUID,
    noteId: UUID,
    updatedClarificationNote: UpdatedClarificationNote,
  ): ResponseEntity<ClarificationNote> {
    val user = userService.getUserForRequest()
    val clarificationNoteResult = assessmentService.updateAssessmentClarificationNote(
      user,
      assessmentId,
      noteId,
      updatedClarificationNote.response,
      updatedClarificationNote.responseReceivedOn,
    )

    val clarificationNoteEntityResult = when (clarificationNoteResult) {
      is AuthorisableActionResult.Success -> clarificationNoteResult.entity
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(assessmentId, "Assessment")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    }

    val clarificationNoteForResponse = when (clarificationNoteEntityResult) {
      is ValidatableActionResult.Success -> clarificationNoteEntityResult.entity
      else -> throw InternalServerErrorProblem("You must provide a response")
    }

    return ResponseEntity.ok(
      assessmentClarificationNoteTransformer.transformJpaToApi(clarificationNoteForResponse),
    )
  }
}
