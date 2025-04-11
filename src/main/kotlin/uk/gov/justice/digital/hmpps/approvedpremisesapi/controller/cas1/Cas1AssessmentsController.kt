package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.AssessmentsCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentRejection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1UpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1UpdatedClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1AssessmentClarificationNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Service
class Cas1AssessmentsController(
  private val cas1AssessmentService: Cas1AssessmentService,
  private val userService: UserService,
  private val offenderService: OffenderService,
  private val cas1AssessmentTransformer: Cas1AssessmentTransformer,
  private val objectMapper: ObjectMapper,
  private val cas1AssessmentClarificationNoteTransformer: Cas1AssessmentClarificationNoteTransformer,
) : AssessmentsCas1Delegate {

  override fun getAssessmentsForUser(
    sortDirection: SortDirection?,
    sortBy: Cas1AssessmentSortField?,
    statuses: List<Cas1AssessmentStatus>?,
    crnOrName: String?,
    page: Int?,
    perPage: Int?,
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

  override fun getAssessment(assessmentId: UUID): ResponseEntity<Cas1Assessment> {
    val user = userService.getUserForRequest()

    val assessment = extractEntityFromCasResult(
      cas1AssessmentService.getAssessmentAndValidate(
        user,
        assessmentId,
      ),
    ) as ApprovedPremisesAssessmentEntity

    val personInfo = offenderService.getPersonInfoResult(assessment.application.crn, user.cas1LaoStrategy())

    val transformedResponse = cas1AssessmentTransformer.transformJpaToCas1Assessment(assessment, personInfo)

    return ResponseEntity.ok(transformedResponse)
  }

  @Transactional
  override fun updateAssessment(
    assessmentId: UUID,
    cas1UpdateAssessment: Cas1UpdateAssessment,
  ): ResponseEntity<Cas1Assessment> {
    val user = userService.getUserForRequest()

    val assessment = extractEntityFromCasResult(
      cas1AssessmentService
        .updateAssessment(
          user,
          assessmentId,
          objectMapper.writeValueAsString(cas1UpdateAssessment.data),
        ),
    ) as ApprovedPremisesAssessmentEntity

    val personInfo = offenderService.getPersonInfoResult(assessment.application.crn, user.cas1LaoStrategy())

    return ResponseEntity.ok(
      cas1AssessmentTransformer.transformJpaToCas1Assessment(assessment, personInfo),
    )
  }

  @Transactional
  override fun addClarificationNoteToAssessment(
    assessmentId: UUID,
    cas1NewClarificationNote: Cas1NewClarificationNote,
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

  @Transactional
  override fun updateAssessmentClarificationNote(
    assessmentId: UUID,
    noteId: UUID,
    cas1UpdatedClarificationNote: Cas1UpdatedClarificationNote,
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

  @Transactional
  override fun acceptAssessment(
    assessmentId: UUID,
    cas1AssessmentAcceptance: Cas1AssessmentAcceptance,
  ): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(cas1AssessmentAcceptance.document)

    val assessmentAuthResult = cas1AssessmentService.acceptAssessment(
      acceptingUser = user,
      assessmentId = assessmentId,
      document = serializedData,
      placementRequirements = cas1AssessmentAcceptance.requirements,
      placementDates = cas1AssessmentAcceptance.placementDates,
      apType = cas1AssessmentAcceptance.apType,
      notes = cas1AssessmentAcceptance.notes,
      agreeWithShortNoticeReason = cas1AssessmentAcceptance.agreeWithShortNoticeReason,
      agreeWithShortNoticeReasonComments = cas1AssessmentAcceptance.agreeWithShortNoticeReasonComments,
      reasonForLateApplication = cas1AssessmentAcceptance.reasonForLateApplication,
    )

    extractEntityFromCasResult(assessmentAuthResult)

    return ResponseEntity(HttpStatus.OK)
  }

  @Transactional
  override fun rejectAssessment(
    assessmentId: UUID,
    cas1AssessmentRejection: Cas1AssessmentRejection,
  ): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(cas1AssessmentRejection.document)

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
    val personInfoResults = offenderService.getPersonInfoResults(crns.toSet(), laoStrategy)

    return summaries.map {
      val crn = it.crn
      cas1AssessmentTransformer.transformDomainToCas1AssessmentSummary(
        it,
        personInfoResults.firstOrNull { it.crn == crn } ?: PersonInfoResult.Unknown(crn),
      )
    }
  }
}
