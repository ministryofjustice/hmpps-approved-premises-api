package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentRejection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReferralHistoryUserNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatedClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.swagger.PaginationHeaders
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentClarificationNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentReferralHistoryNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.sortByName
import java.util.UUID

@Suppress("LongParameterList", "ThrowsCount")
@RestController
class AssessmentController(
  private val objectMapper: ObjectMapper,
  private val assessmentService: AssessmentService,
  private val userService: UserService,
  private val offenderDetailService: OffenderDetailService,
  private val assessmentTransformer: AssessmentTransformer,
  private val assessmentClarificationNoteTransformer: AssessmentClarificationNoteTransformer,
  private val assessmentReferralHistoryNoteTransformer: AssessmentReferralHistoryNoteTransformer,
  private val cas3AssessmentService: Cas3AssessmentService,
) {

  @PaginationHeaders
  @Operation(
    tags = ["Assessment data"],
    summary = "Gets assessments the user is authorised to view",
    description = """This endpoint is deprecated; please use the CAS-specific endpoint instead""",
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/assessments"],
    produces = ["application/json"],
  )
  fun assessmentsGet(
    @RequestHeader(name = "X-Service-Name") xServiceName: ServiceName,
    @RequestParam(defaultValue = "asc") sortDirection: SortDirection,
    @RequestParam(defaultValue = "arrivalDate") sortBy: AssessmentSortField,
    @RequestParam statuses: List<AssessmentStatus>?,
    @RequestParam crnOrName: String?,
    @RequestParam page: Int?,
    @RequestParam perPage: Int?,
  ): ResponseEntity<List<AssessmentSummary>> {
    val user = userService.getUserForRequest()
    val domainSummaryStatuses = statuses?.map { assessmentTransformer.transformApiStatusToDomainSummaryState(it) } ?: emptyList()

    val (summaries, metadata) = when (xServiceName) {
      ServiceName.cas2v2 -> throw UnsupportedOperationException("CAS2v2 not supported")
      ServiceName.cas2 -> throw UnsupportedOperationException("CAS2 not supported")
      ServiceName.temporaryAccommodation -> {
        val (summaries, metadata) = assessmentService.getAssessmentSummariesForUserCAS3(
          user,
          crnOrName,
          xServiceName,
          domainSummaryStatuses,
          PageCriteria(sortBy, sortDirection, page, perPage),
        )
        val transformSummaries = when (sortBy) {
          AssessmentSortField.assessmentDueAt -> throw BadRequestProblem(errorDetail = "Sorting by due date is not supported for CAS3")
          AssessmentSortField.personName -> transformDomainToApi(summaries, user.cas3LaoStrategy()).sortByName(
            sortDirection,
          )
          else -> transformDomainToApi(summaries, user.cas3LaoStrategy())
        }
        Pair(transformSummaries, metadata)
      }

      else -> {
        val (summaries, metadata) = assessmentService.getVisibleAssessmentSummariesForUserCAS1(
          user,
          domainSummaryStatuses,
          PageCriteria(sortBy, sortDirection, page, perPage),
        )
        Pair(transformDomainToApi(summaries, user.cas1LaoStrategy()), metadata)
      }
    }

    return ResponseEntity.ok()
      .headers(metadata?.toHeaders())
      .body(summaries)
  }

  private fun transformDomainToApi(
    summaries: List<DomainAssessmentSummary>,
    laoStrategy: LaoStrategy,
  ): List<AssessmentSummary> {
    val crns = summaries.map { it.crn }
    val personInfoResults = offenderDetailService.getPersonInfoResults(crns.toSet(), laoStrategy)

    return summaries.map {
      val crn = it.crn
      assessmentTransformer.transformDomainToApiSummary(
        it,
        personInfoResults.firstOrNull { it.crn == crn } ?: PersonInfoResult.Unknown(crn),
      )
    }
  }

  @Operation(
    tags = ["Assessment data"],
    summary = "Gets a single assessment by its id",
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/assessments/{assessmentId}"],
    produces = ["application/json"],
  )
  fun assessmentsAssessmentIdGet(@PathVariable assessmentId: UUID): ResponseEntity<Assessment> {
    val user = userService.getUserForRequest()

    val assessment = extractEntityFromCasResult(assessmentService.getAssessmentAndValidate(user, assessmentId))

    val ignoreLaoRestrictions = (assessment.application is ApprovedPremisesApplicationEntity) && user.hasQualification(UserQualification.LAO)

    val personInfo = offenderDetailService.getPersonInfoResult(assessment.application.crn, user.deliusUsername, ignoreLaoRestrictions)

    val transformedResponse = assessmentTransformer.transformJpaToApi(assessment, personInfo)

    return ResponseEntity.ok(transformedResponse)
  }

  @Operation(
    tags = ["Assessment data"],
    summary = "Updates an assessment",
  )
  @RequestMapping(
    method = [RequestMethod.PUT],
    value = ["/assessments/{assessmentId}"],
    produces = ["application/json"],
    consumes = ["application/json"],
  )
  @Transactional
  fun assessmentsAssessmentIdPut(
    @PathVariable assessmentId: UUID,
    @RequestBody updateAssessment: UpdateAssessment,
    @RequestHeader("X-Service-Name") xServiceName: ServiceName?,
  ): ResponseEntity<Assessment> {
    val user = userService.getUserForRequest()
    val assessment =
      when (xServiceName) {
        ServiceName.temporaryAccommodation -> {
          extractEntityFromCasResult(cas3AssessmentService.updateAssessment(user, assessmentId, updateAssessment))
        }

        else -> {
          extractEntityFromCasResult(
            assessmentService
              .updateAssessment(
                user,
                assessmentId,
                objectMapper.writeValueAsString(updateAssessment.data),
              ),
          )
        }
      }

    val ignoreLao =
      (assessment.application is ApprovedPremisesApplicationEntity) && user.hasQualification(UserQualification.LAO)

    val personInfo = offenderDetailService.getPersonInfoResult(assessment.application.crn, user.deliusUsername, ignoreLao)

    return ResponseEntity.ok(
      assessmentTransformer.transformJpaToApi(assessment, personInfo),
    )
  }

  @Operation(
    tags = ["Assessment data"],
    summary = "Accepts an Assessment",
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/assessments/{assessmentId}/acceptance"],
    consumes = ["application/json"],
  )
  @Transactional
  fun assessmentsAssessmentIdAcceptancePost(
    @PathVariable assessmentId: UUID,
    @RequestBody assessmentAcceptance: AssessmentAcceptance,
  ): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(assessmentAcceptance.document)

    val assessmentAuthResult = assessmentService.acceptAssessment(
      acceptingUser = user,
      assessmentId = assessmentId,
      document = serializedData,
      placementRequirements = assessmentAcceptance.requirements,
      placementDates = assessmentAcceptance.placementDates,
      apType = assessmentAcceptance.apType,
      notes = assessmentAcceptance.notes,
      agreeWithShortNoticeReason = assessmentAcceptance.agreeWithShortNoticeReason,
      agreeWithShortNoticeReasonComments = assessmentAcceptance.agreeWithShortNoticeReasonComments,
      reasonForLateApplication = assessmentAcceptance.reasonForLateApplication,
    )

    extractEntityFromCasResult(assessmentAuthResult)

    return ResponseEntity(HttpStatus.OK)
  }

  @Operation(
    tags = ["Assessment data"],
    summary = "Rejects an Assessment",
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/assessments/{assessmentId}/rejection"],
    consumes = ["application/json"],
  )
  @Transactional
  fun assessmentsAssessmentIdRejectionPost(
    @PathVariable assessmentId: UUID,
    @RequestBody assessmentRejection: AssessmentRejection,
  ): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(assessmentRejection.document)

    val assessmentAuthResult =
      assessmentService.rejectAssessment(
        user,
        assessmentId,
        serializedData,
        assessmentRejection.rejectionRationale,
        assessmentRejection.referralRejectionReasonId,
        assessmentRejection.referralRejectionReasonDetail,
        assessmentRejection.isWithdrawn,
        assessmentRejection.agreeWithShortNoticeReason,
        assessmentRejection.agreeWithShortNoticeReasonComments,
        assessmentRejection.reasonForLateApplication,
      )

    extractEntityFromCasResult(assessmentAuthResult)

    return ResponseEntity(HttpStatus.OK)
  }

  @Operation(
    tags = ["Assessment data"],
    summary = "Closes an Assessment",
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/assessments/{assessmentId}/closure"],
  )
  fun assessmentsAssessmentIdClosurePost(@PathVariable assessmentId: UUID): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()
    val assessmentAuthResult = assessmentService.closeAssessment(user, assessmentId)
    extractEntityFromCasResult(assessmentAuthResult)
    return ResponseEntity(HttpStatus.OK)
  }

  @Operation(
    tags = ["Assessment data"],
    summary = "Adds a clarification note to an assessment",
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/assessments/{assessmentId}/notes"],
    produces = ["application/json"],
    consumes = ["application/json"],
  )
  fun assessmentsAssessmentIdNotesPost(
    @PathVariable assessmentId: UUID,
    @RequestBody newClarificationNote: NewClarificationNote,
  ): ResponseEntity<ClarificationNote> {
    val user = userService.getUserForRequest()

    val clarificationNoteResult = assessmentService.addAssessmentClarificationNote(user, assessmentId, newClarificationNote.query)

    return ResponseEntity.ok(
      assessmentClarificationNoteTransformer.transformJpaToApi(extractEntityFromCasResult(clarificationNoteResult)),
    )
  }

  @Operation(
    tags = ["Assessment data"],
    summary = "Updates an assessment's clarification note",
  )
  @RequestMapping(
    method = [RequestMethod.PUT],
    value = ["/assessments/{assessmentId}/notes/{noteId}"],
    produces = ["application/json"],
    consumes = ["application/json"],
  )
  fun assessmentsAssessmentIdNotesNoteIdPut(
    @PathVariable assessmentId: UUID,
    @PathVariable noteId: UUID,
    @RequestBody updatedClarificationNote: UpdatedClarificationNote,
  ): ResponseEntity<ClarificationNote> {
    val user = userService.getUserForRequest()
    val clarificationNoteResult = assessmentService.updateAssessmentClarificationNote(
      user,
      assessmentId,
      noteId,
      updatedClarificationNote.response,
      updatedClarificationNote.responseReceivedOn,
    )

    return ResponseEntity.ok(
      assessmentClarificationNoteTransformer.transformJpaToApi(extractEntityFromCasResult(clarificationNoteResult)),
    )
  }

  @Operation(
    tags = ["Assessment data"],
    summary = "Adds a user-written note to an assessment",
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/assessments/{assessmentId}/referral-history-notes"],
    produces = ["application/json"],
    consumes = ["application/json"],
  )
  fun assessmentsAssessmentIdReferralHistoryNotesPost(
    @PathVariable assessmentId: UUID,
    @RequestBody newReferralHistoryUserNote: NewReferralHistoryUserNote,
  ): ResponseEntity<ReferralHistoryNote> {
    val user = userService.getUserForRequest()

    val referralHistoryUserNoteResult =
      assessmentService.addAssessmentReferralHistoryUserNote(user, assessmentId, newReferralHistoryUserNote.message)

    return ResponseEntity.ok(
      assessmentReferralHistoryNoteTransformer.transformJpaToApi(
        extractEntityFromCasResult(
          referralHistoryUserNoteResult,
        ),
      ),
    )
  }
}
