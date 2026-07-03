package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PaginationHeaders
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1AssessmentRejection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1NewClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1UpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1UpdatedClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1AssessmentClarificationNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Cas1Controller
@Tag(name = "CAS1 Assessments")
class Cas1AssessmentsController(
  private val cas1AssessmentService: Cas1AssessmentService,
  private val userService: UserService,
  private val cas1AssessmentTransformer: Cas1AssessmentTransformer,
  private val jsonMapper: JsonMapper,
  private val cas1AssessmentClarificationNoteTransformer: Cas1AssessmentClarificationNoteTransformer,
  private val offenderDetailService: OffenderDetailService,
) {

  @PaginationHeaders
  @Operation(
    summary = "Gets assessments the user is authorised to view",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully retrieved assessments", content = [Content(array = ArraySchema(schema = Schema(implementation = Cas1AssessmentSummary::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/assessments"],
    produces = ["application/json"],
  )
  fun getAssessmentsForUser(
    @RequestParam sortDirection: SortDirection?,
    @RequestParam sortBy: Cas1AssessmentSortField?,
    @RequestParam statuses: List<Cas1AssessmentStatus>?,
    @RequestParam page: Int?,
    @RequestParam perPage: Int?,
  ): ResponseEntity<List<Cas1AssessmentSummary>> {
    val user = userService.getUserForRequest()
    val resolvedSortDirection = sortDirection ?: SortDirection.asc
    val resolvedSortBy = sortBy ?: Cas1AssessmentSortField.assessmentArrivalDate

    val domainSummaryStatuses =
      statuses?.map { cas1AssessmentTransformer.transformCas1AssessmentStatusToDomainSummaryState(it) } ?: emptyList()
    val (summaries, metadata) = cas1AssessmentService.findApprovedPremisesAssessmentSummariesNotReallocatedForUser(
      user,
      domainSummaryStatuses,
      PageCriteria(resolvedSortBy, resolvedSortDirection, page, perPage),
    )

    val transformedSummaries = transformDomainToApi(summaries, user.cas1LaoStrategy())

    return ResponseEntity.ok()
      .headers(metadata?.toHeaders())
      .body(transformedSummaries)
  }

  @Operation(
    summary = "Gets a single assessment by its id",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully retrieved assessment", content = [Content(schema = Schema(implementation = Cas1Assessment::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/assessments/{assessmentId}"],
    produces = ["application/json"],
  )
  fun getAssessment(@PathVariable assessmentId: UUID): ResponseEntity<Cas1Assessment> {
    val user = userService.getUserForRequest()

    val assessment = extractEntityFromCasResult(
      cas1AssessmentService.getAssessmentAndValidate(
        user,
        assessmentId,
      ),
    )

    val personInfo = offenderDetailService.getPersonInfoResult(assessment.application.crn, user.cas1LaoStrategy())

    val transformedResponse = cas1AssessmentTransformer.transformJpaToCas1Assessment(assessment, personInfo)

    return ResponseEntity.ok(transformedResponse)
  }

  @Operation(
    summary = "Updates an assessment",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully updated assessment", content = [Content(schema = Schema(implementation = Cas1Assessment::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.PUT],
    value = ["/assessments/{assessmentId}"],
    produces = ["application/json"],
    consumes = ["application/json"],
  )
  @Transactional
  fun updateAssessment(
    @PathVariable assessmentId: UUID,
    @RequestBody cas1UpdateAssessment: Cas1UpdateAssessment,
  ): ResponseEntity<Cas1Assessment> {
    val user = userService.getUserForRequest()

    val assessment = extractEntityFromCasResult(
      cas1AssessmentService
        .updateAssessment(
          user,
          assessmentId,
          jsonMapper.writeValueAsString(cas1UpdateAssessment.data),
        ),
    )

    val personInfo = offenderDetailService.getPersonInfoResult(assessment.application.crn, user.cas1LaoStrategy())

    return ResponseEntity.ok(
      cas1AssessmentTransformer.transformJpaToCas1Assessment(assessment, personInfo),
    )
  }

  @Operation(
    summary = "Adds a clarification note to an assessment",
    responses = [
      ApiResponse(responseCode = "201", description = "successfully created a clarification note", content = [Content(schema = Schema(implementation = Cas1ClarificationNote::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/assessments/{assessmentId}/notes"],
    produces = ["application/json"],
    consumes = ["application/json"],
  )
  @Transactional
  fun addClarificationNoteToAssessment(
    @PathVariable assessmentId: UUID,
    @RequestBody cas1NewClarificationNote: Cas1NewClarificationNote,
  ): ResponseEntity<Cas1ClarificationNote> {
    val user = userService.getUserForRequest()

    val clarificationNoteResult =
      cas1AssessmentService.addAssessmentClarificationNote(user, assessmentId, cas1NewClarificationNote.query)

    return ResponseEntity.ok(
      cas1AssessmentClarificationNoteTransformer.transformJpaToCas1ClarificationNote(
        extractEntityFromCasResult(
          clarificationNoteResult,
        ),
      ),
    )
  }

  @Operation(
    summary = "Updates an assessment's clarification note",
    responses = [
      ApiResponse(responseCode = "201", description = "successfully updated a clarification note", content = [Content(schema = Schema(implementation = Cas1ClarificationNote::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.PUT],
    value = ["/assessments/{assessmentId}/notes/{noteId}"],
    produces = ["application/json"],
    consumes = ["application/json"],
  )
  @Transactional
  fun updateAssessmentClarificationNote(
    @PathVariable assessmentId: UUID,
    @PathVariable noteId: UUID,
    @RequestBody cas1UpdatedClarificationNote: Cas1UpdatedClarificationNote,
  ): ResponseEntity<Cas1ClarificationNote> {
    val user = userService.getUserForRequest()
    val clarificationNoteResult = cas1AssessmentService.updateAssessmentClarificationNote(
      user,
      assessmentId,
      noteId,
      cas1UpdatedClarificationNote.response,
      cas1UpdatedClarificationNote.responseReceivedOn,
    )

    return ResponseEntity.ok(
      cas1AssessmentClarificationNoteTransformer.transformJpaToCas1ClarificationNote(extractEntityFromCasResult(clarificationNoteResult)),
    )
  }

  @Operation(
    summary = "Accepts an Assessment",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully accepted the assessment"),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/assessments/{assessmentId}/acceptance"],
    consumes = ["application/json"],
  )
  fun acceptAssessment(
    @PathVariable assessmentId: UUID,
    @RequestBody cas1AssessmentAcceptance: Cas1AssessmentAcceptance,
  ): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    val serializedData = jsonMapper.writeValueAsString(cas1AssessmentAcceptance.document)

    val assessmentAuthResult = cas1AssessmentService.acceptAssessment(
      acceptingUser = user,
      assessmentId = assessmentId,
      document = serializedData,
      placementRequirements = cas1AssessmentAcceptance.requirements,
      placementDates = cas1AssessmentAcceptance.placementDates,
      notes = cas1AssessmentAcceptance.notes,
      agreeWithShortNoticeReason = cas1AssessmentAcceptance.agreeWithShortNoticeReason,
      agreeWithShortNoticeReasonComments = cas1AssessmentAcceptance.agreeWithShortNoticeReasonComments,
      reasonForLateApplication = cas1AssessmentAcceptance.reasonForLateApplication,
    )

    extractEntityFromCasResult(assessmentAuthResult)

    return ResponseEntity(HttpStatus.OK)
  }

  @Operation(
    summary = "Rejects an Assessment",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully rejected the assessment"),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/assessments/{assessmentId}/rejection"],
    consumes = ["application/json"],
  )
  fun rejectAssessment(
    @PathVariable assessmentId: UUID,
    @RequestBody cas1AssessmentRejection: Cas1AssessmentRejection,
  ): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    val serializedData = jsonMapper.writeValueAsString(cas1AssessmentRejection.document)

    val assessmentAuthResult =
      cas1AssessmentService.rejectAssessment(
        user,
        assessmentId,
        serializedData,
        cas1AssessmentRejection.rejectionRationale,
        cas1AssessmentRejection.agreeWithShortNoticeReason,
        cas1AssessmentRejection.agreeWithShortNoticeReasonComments,
        cas1AssessmentRejection.reasonForLateApplication,
      )

    extractEntityFromCasResult(assessmentAuthResult)

    return ResponseEntity(HttpStatus.OK)
  }

  private fun transformDomainToApi(
    summaries: List<DomainAssessmentSummary>,
    laoStrategy: LaoStrategy,
  ): List<Cas1AssessmentSummary> {
    val crns = summaries.map { it.crn }
    val personInfoResults = offenderDetailService.getPersonInfoResults(crns.toSet(), laoStrategy)

    return summaries.map {
      val crn = it.crn
      cas1AssessmentTransformer.transformDomainToCas1AssessmentSummary(
        it,
        personInfoResults.firstOrNull { it.crn == crn } ?: PersonInfoResult.Unknown(crn),
      )
    }
  }
}
