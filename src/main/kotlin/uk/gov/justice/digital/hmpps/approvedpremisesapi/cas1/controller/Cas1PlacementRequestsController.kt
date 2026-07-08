package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1NewBookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1PlacementRequestDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1PlacementRequestSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1WithdrawPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.NotAllowedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingNotMadeTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestDetailTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PlacementRequestSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.time.LocalDate
import java.util.UUID

@Cas1Controller
@Tag(name = "CAS1 Placement Requests")
class Cas1PlacementRequestsController(
  private val userService: UserService,
  private val placementRequestService: Cas1PlacementRequestService,
  private val cas1PlacementRequestSummaryTransformer: Cas1PlacementRequestSummaryTransformer,
  private val placementRequestDetailTransformer: PlacementRequestDetailTransformer,
  private val offenderDetailService: OffenderDetailService,
  private val cas1WithdrawableService: Cas1WithdrawableService,
  private val bookingNotMadeTransformer: BookingNotMadeTransformer,
  private val userAccessService: Cas1UserAccessService,
) {

  @Operation(
    summary = "Gets all placement requests",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully retrieved placement requests", content = [Content(array = ArraySchema(schema = Schema(implementation = Cas1PlacementRequestSummary::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/placement-requests"],
    produces = ["application/json"],
  )
  fun search(
    @RequestParam status: PlacementRequestStatus?,
    @RequestParam crnOrName: String?,
    @Schema(description = "Filter on the tier captured when the application was created")
    @RequestParam tier: RiskTierLevel?,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) arrivalDateStart: LocalDate?,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) arrivalDateEnd: LocalDate?,
    @RequestParam requestType: PlacementRequestRequestType?,
    @RequestParam cruManagementAreaId: UUID?,
    @RequestParam page: Int?,
    @RequestParam sortBy: PlacementRequestSortField?,
    @RequestParam sortDirection: SortDirection?,
  ): ResponseEntity<List<Cas1PlacementRequestSummary>> {
    val user = getUserForRequest()

    if (!user.hasPermission(UserPermission.CAS1_VIEW_CRU_DASHBOARD)) {
      throw ForbiddenProblem()
    }

    val (requests, metadata) = placementRequestService.getAllCas1Active(
      Cas1PlacementRequestService.AllActiveSearchCriteria(
        status = status,
        crnOrName = crnOrName,
        tierOnApplicationCreation = tier?.value,
        arrivalDateStart = arrivalDateStart,
        arrivalDateEnd = arrivalDateEnd,
        requestType = requestType,
        cruManagementAreaId = cruManagementAreaId,
      ),
      PageCriteria(
        sortBy = sortBy ?: PlacementRequestSortField.createdAt,
        sortDirection = sortDirection ?: SortDirection.asc,
        page = page,
      ),
    )

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(
      mapPersonDetailOntoPlacementRequests(requests, user),
    )
  }

  private fun mapPersonDetailOntoPlacementRequests(
    placementRequests: List<uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1PlacementRequestSummary>,
    user: UserEntity,
  ): List<Cas1PlacementRequestSummary> {
    val offenderDetails = offenderDetailService.getPersonInfoResults(
      placementRequests.map { it.getPersonCrn() }.toSet(),
      user.cas1LaoStrategy(),
    )
    return placementRequests.map { placementRequest ->
      val crn = placementRequest.getPersonCrn()
      cas1PlacementRequestSummaryTransformer.transformCas1PlacementRequestSummaryJpaToApi(
        placementRequest,
        offenderDetails.find { it.crn == crn }!!,
      )
    }
  }

  @Operation(
    summary = "Gets placement requests for a given user",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully retrieved placement requests", content = [Content(schema = Schema(implementation = Cas1PlacementRequestDetail::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/placement-requests/{id}"],
    produces = ["application/json"],
  )
  fun getPlacementRequest(@Parameter(description = "ID of the placement request") @PathVariable id: UUID): ResponseEntity<Cas1PlacementRequestDetail> {
    val user = getUserForRequest()

    val placementRequest = extractEntityFromCasResult(placementRequestService.getPlacementRequest(user, id))

    return ResponseEntity.ok(toCas1PlacementRequestDetail(user, placementRequest))
  }

  @Operation(
    summary = "Withdraws a placement request",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas1PlacementRequestDetail::class))]),
      ApiResponse(responseCode = "404", description = "invalid applicationId", content = [Content(schema = Schema(implementation = Problem::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/placement-requests/{id}/withdrawal"],
    produces = ["application/json"],
    consumes = ["application/json"],
  )
  @SuppressWarnings("ThrowsCount")
  fun withdrawPlacementRequest(
    @Parameter(description = "ID of the placement request") @PathVariable id: UUID,
    @Parameter(description = "Withdrawal details") @RequestBody body: Cas1WithdrawPlacementRequest?,
  ): ResponseEntity<Cas1PlacementRequestDetail> {
    val user = getUserForRequest()

    val reason = when (body?.reason) {
      WithdrawPlacementRequestReason.duplicatePlacementRequest -> PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST
      WithdrawPlacementRequestReason.alternativeProvisionIdentified -> PlacementRequestWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED
      WithdrawPlacementRequestReason.changeInCircumstances -> PlacementRequestWithdrawalReason.CHANGE_IN_CIRCUMSTANCES
      WithdrawPlacementRequestReason.changeInReleaseDecision -> PlacementRequestWithdrawalReason.CHANGE_IN_RELEASE_DECISION
      WithdrawPlacementRequestReason.noCapacityDueToLostBed -> PlacementRequestWithdrawalReason.NO_CAPACITY_DUE_TO_LOST_BED
      WithdrawPlacementRequestReason.noCapacityDueToPlacementPrioritisation -> PlacementRequestWithdrawalReason.NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION
      WithdrawPlacementRequestReason.noCapacity -> PlacementRequestWithdrawalReason.NO_CAPACITY
      WithdrawPlacementRequestReason.errorInPlacementRequest -> PlacementRequestWithdrawalReason.ERROR_IN_PLACEMENT_REQUEST
      WithdrawPlacementRequestReason.withdrawnByPP -> throw NotAllowedProblem("Withdrawal reason is reserved for internal use")
      WithdrawPlacementRequestReason.relatedApplicationWithdrawn -> throw NotAllowedProblem("Withdrawal reason is reserved for internal use")
      WithdrawPlacementRequestReason.relatedPlacementRequestWithdrawn -> throw NotAllowedProblem("Withdrawal reason is reserved for internal use")
      WithdrawPlacementRequestReason.relatedPlacementApplicationWithdrawn -> throw NotAllowedProblem("Withdrawal reason is reserved for internal use")
      null -> null
    }

    val placementRequest = extractEntityFromCasResult(
      cas1WithdrawableService.withdrawPlacementRequest(
        id,
        user,
        reason,
      ),
    )

    return ResponseEntity.ok(toCas1PlacementRequestDetail(user, placementRequest))
  }

  @Operation(
    summary = "Records that an attempt to match was made but no suitable Beds could be found",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully recorded that a Booking could not be made", content = [Content(schema = Schema(implementation = BookingNotMade::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/placement-requests/{id}/booking-not-made"],
    produces = ["application/json"],
    consumes = ["application/json"],
  )
  fun bookingNotMade(
    @Parameter(description = "ID of the placement request", required = true) @PathVariable id: UUID,
    @Parameter(description = "Details about the failure to match", required = true) @RequestBody body: Cas1NewBookingNotMade,
  ): ResponseEntity<BookingNotMade> {
    val user = getUserForRequest()

    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_PLACEMENT_REQUEST_RECORD_UNABLE_TO_MATCH)

    val result = placementRequestService.createBookingNotMade(
      user = user,
      placementRequestId = id,
      notes = body.notes,
    )

    val bookingNotMade = extractEntityFromCasResult(result)

    return ResponseEntity.ok(bookingNotMadeTransformer.transformJpaToApi(bookingNotMade))
  }

  private fun getUserForRequest(): UserEntity = userService.getUserForRequest()

  private fun toCas1PlacementRequestDetail(
    forUser: UserEntity,
    placementRequestEntity: PlacementRequestEntity,
  ): Cas1PlacementRequestDetail {
    val personInfo = offenderDetailService.getPersonInfoResult(
      placementRequestEntity.application.crn,
      forUser.cas1LaoStrategy(),
    )

    return placementRequestDetailTransformer.transformJpaToCas1PlacementRequestDetail(
      placementRequestEntity,
      personInfo,
    )
  }
}
