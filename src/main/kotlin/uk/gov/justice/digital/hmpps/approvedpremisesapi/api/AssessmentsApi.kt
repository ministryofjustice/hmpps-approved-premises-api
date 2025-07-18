/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.13.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
*/
package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.swagger.PaginationHeaders

@RestController
interface AssessmentsApi {

  fun getDelegate(): AssessmentsApiDelegate = object : AssessmentsApiDelegate {}

  @Operation(
    tags = ["Assessment data"],
    summary = "Accepts an Assessment",
    operationId = "assessmentsAssessmentIdAcceptancePost",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully accepted the assessment"),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/assessments/{assessmentId}/acceptance"],
    consumes = ["application/json"],
  )
  fun assessmentsAssessmentIdAcceptancePost(@Parameter(description = "Id of the assessment", required = true) @PathVariable("assessmentId") assessmentId: java.util.UUID, @Parameter(description = "Information needed to accept an assessment", required = true) @RequestBody assessmentAcceptance: AssessmentAcceptance): ResponseEntity<Unit> = getDelegate().assessmentsAssessmentIdAcceptancePost(assessmentId, assessmentAcceptance)

  @Operation(
    tags = ["Assessment data"],
    summary = "Closes an Assessment",
    operationId = "assessmentsAssessmentIdClosurePost",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully closed the assessment"),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/assessments/{assessmentId}/closure"],
  )
  fun assessmentsAssessmentIdClosurePost(@Parameter(description = "Id of the assessment", required = true) @PathVariable("assessmentId") assessmentId: java.util.UUID): ResponseEntity<Unit> = getDelegate().assessmentsAssessmentIdClosurePost(assessmentId)

  @Operation(
    tags = ["Assessment data"],
    summary = "Gets a single assessment by its id",
    operationId = "assessmentsAssessmentIdGet",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully retrieved assessment", content = [Content(schema = Schema(implementation = Assessment::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/assessments/{assessmentId}"],
    produces = ["application/json"],
  )
  fun assessmentsAssessmentIdGet(@Parameter(description = "Id of the assessment", required = true) @PathVariable("assessmentId") assessmentId: java.util.UUID): ResponseEntity<Assessment> = getDelegate().assessmentsAssessmentIdGet(assessmentId)

  @Operation(
    tags = ["Assessment data"],
    summary = "Updates an assessment's clarification note",
    operationId = "assessmentsAssessmentIdNotesNoteIdPut",
    description = """""",
    responses = [
      ApiResponse(responseCode = "201", description = "successfully updated a clarification note", content = [Content(schema = Schema(implementation = ClarificationNote::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.PUT],
    value = ["/assessments/{assessmentId}/notes/{noteId}"],
    produces = ["application/json"],
    consumes = ["application/json"],
  )
  fun assessmentsAssessmentIdNotesNoteIdPut(@Parameter(description = "Id of the assessment", required = true) @PathVariable("assessmentId") assessmentId: java.util.UUID, @Parameter(description = "Id of the clarification note", required = true) @PathVariable("noteId") noteId: java.util.UUID, @Parameter(description = "Clarification note", required = true) @RequestBody updatedClarificationNote: UpdatedClarificationNote): ResponseEntity<ClarificationNote> = getDelegate().assessmentsAssessmentIdNotesNoteIdPut(assessmentId, noteId, updatedClarificationNote)

  @Operation(
    tags = ["Assessment data"],
    summary = "Adds a clarification note to an assessment",
    operationId = "assessmentsAssessmentIdNotesPost",
    description = """""",
    responses = [
      ApiResponse(responseCode = "201", description = "successfully created a clarification note", content = [Content(schema = Schema(implementation = ClarificationNote::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/assessments/{assessmentId}/notes"],
    produces = ["application/json"],
    consumes = ["application/json"],
  )
  fun assessmentsAssessmentIdNotesPost(@Parameter(description = "Id of the assessment", required = true) @PathVariable("assessmentId") assessmentId: java.util.UUID, @Parameter(description = "Clarification note", required = true) @RequestBody newClarificationNote: NewClarificationNote): ResponseEntity<ClarificationNote> = getDelegate().assessmentsAssessmentIdNotesPost(assessmentId, newClarificationNote)

  @Operation(
    tags = ["Assessment data"],
    summary = "Updates an assessment",
    operationId = "assessmentsAssessmentIdPut",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully retrieved assessment", content = [Content(schema = Schema(implementation = Assessment::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.PUT],
    value = ["/assessments/{assessmentId}"],
    produces = ["application/json"],
    consumes = ["application/json"],
  )
  fun assessmentsAssessmentIdPut(@Parameter(description = "Id of the assessment", required = true) @PathVariable("assessmentId") assessmentId: java.util.UUID, @Parameter(description = "Updated assessment", required = true) @RequestBody updateAssessment: UpdateAssessment, @Parameter(description = "Required for operations specific to each CAS", `in` = ParameterIn.HEADER, schema = Schema(allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"])) @RequestHeader(value = "X-Service-Name", required = false) xServiceName: ServiceName?): ResponseEntity<Assessment> = getDelegate().assessmentsAssessmentIdPut(assessmentId, updateAssessment, xServiceName)

  @Operation(
    tags = ["Assessment data"],
    summary = "Adds a user-written note to an assessment",
    operationId = "assessmentsAssessmentIdReferralHistoryNotesPost",
    description = """""",
    responses = [
      ApiResponse(responseCode = "201", description = "successfully created a user-written note", content = [Content(schema = Schema(implementation = ReferralHistoryNote::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/assessments/{assessmentId}/referral-history-notes"],
    produces = ["application/json"],
    consumes = ["application/json"],
  )
  fun assessmentsAssessmentIdReferralHistoryNotesPost(@Parameter(description = "Id of the assessment", required = true) @PathVariable("assessmentId") assessmentId: java.util.UUID, @Parameter(description = "User note", required = true) @RequestBody newReferralHistoryUserNote: NewReferralHistoryUserNote): ResponseEntity<ReferralHistoryNote> = getDelegate().assessmentsAssessmentIdReferralHistoryNotesPost(assessmentId, newReferralHistoryUserNote)

  @Operation(
    tags = ["Assessment data"],
    summary = "Rejects an Assessment",
    operationId = "assessmentsAssessmentIdRejectionPost",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully rejected the assessment"),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/assessments/{assessmentId}/rejection"],
    consumes = ["application/json"],
  )
  fun assessmentsAssessmentIdRejectionPost(@Parameter(description = "Id of the assessment", required = true) @PathVariable("assessmentId") assessmentId: java.util.UUID, @Parameter(description = "Rejection info", required = true) @RequestBody assessmentRejection: AssessmentRejection): ResponseEntity<Unit> = getDelegate().assessmentsAssessmentIdRejectionPost(assessmentId, assessmentRejection)

  @PaginationHeaders
  @Operation(
    tags = ["Assessment data"],
    summary = "Gets assessments the user is authorised to view",
    operationId = "assessmentsGet",
    description = """This endpoint is deprecated; please use the CAS-specific endpoint instead""",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully retrieved assessments", content = [Content(array = ArraySchema(schema = Schema(implementation = AssessmentSummary::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/assessments"],
    produces = ["application/json"],
  )
  fun assessmentsGet(@Parameter(description = "Only assessments for this service will be returned", `in` = ParameterIn.HEADER, required = true, schema = Schema(allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"])) @RequestHeader(value = "X-Service-Name", required = true) xServiceName: ServiceName, @RequestParam(value = "sortDirection", required = false) sortDirection: SortDirection?, @RequestParam(value = "sortBy", required = false) sortBy: AssessmentSortField?, @RequestParam(value = "statuses", required = false) statuses: kotlin.collections.List<AssessmentStatus>?, @RequestParam(value = "crnOrName", required = false) crnOrName: kotlin.String?, @RequestParam(value = "page", required = false) page: kotlin.Int?, @RequestParam(value = "perPage", required = false) perPage: kotlin.Int?): ResponseEntity<List<AssessmentSummary>> = getDelegate().assessmentsGet(xServiceName, sortDirection, sortBy, statuses, crnOrName, page, perPage)
}
