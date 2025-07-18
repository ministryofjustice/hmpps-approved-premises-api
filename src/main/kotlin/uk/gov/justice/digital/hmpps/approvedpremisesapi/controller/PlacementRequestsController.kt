package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PlacementRequestsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementRequestBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementRequestBookingConfirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotAllowedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestService.PlacementRequestAndCancellations
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingNotMadeTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestDetailTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.time.LocalDate
import java.util.UUID

@Service
class PlacementRequestsController(
  private val userService: UserService,
  private val placementRequestService: Cas1PlacementRequestService,
  private val placementRequestTransformer: PlacementRequestTransformer,
  private val placementRequestDetailTransformer: PlacementRequestDetailTransformer,
  private val bookingNotMadeTransformer: BookingNotMadeTransformer,
  private val cas1WithdrawableService: Cas1WithdrawableService,
  private val userAccessService: UserAccessService,
  private val offenderDetailService: OffenderDetailService,
) : PlacementRequestsApiDelegate {

  @Deprecated("This will be removed once UI has removed usage in code")
  override fun placementRequestsIdBookingPost(id: UUID, newPlacementRequestBooking: NewPlacementRequestBooking): ResponseEntity<NewPlacementRequestBookingConfirmation> = ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

  @Deprecated("Use Cas1PlacementRequestsController.search instead")
  override fun placementRequestsDashboardGet(
    status: PlacementRequestStatus?,
    crn: String?,
    crnOrName: String?,
    tier: RiskTierLevel?,
    arrivalDateStart: LocalDate?,
    arrivalDateEnd: LocalDate?,
    requestType: PlacementRequestRequestType?,
    apAreaId: UUID?,
    cruManagementAreaId: UUID?,
    page: Int?,
    sortBy: PlacementRequestSortField?,
    sortDirection: SortDirection?,
  ): ResponseEntity<List<PlacementRequest>> {
    val user = userService.getUserForRequest()

    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_VIEW_CRU_DASHBOARD)

    val (requests, metadata) = placementRequestService.getAllActive(
      Cas1PlacementRequestService.AllActiveSearchCriteria(
        status = status,
        crn = crn,
        crnOrName = crnOrName,
        tier = tier?.value,
        arrivalDateStart = arrivalDateStart,
        arrivalDateEnd = arrivalDateEnd,
        requestType = requestType,
        apAreaId = apAreaId,
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

  override fun placementRequestsIdGet(id: UUID): ResponseEntity<PlacementRequestDetail> {
    val user = userService.getUserForRequest()

    val placementRequest = extractEntityFromCasResult(placementRequestService.getPlacementRequestForUser(user, id))

    return ResponseEntity.ok(toPlacementRequestDetail(user, placementRequest))
  }

  override fun placementRequestsIdBookingNotMadePost(id: UUID, newBookingNotMade: NewBookingNotMade): ResponseEntity<BookingNotMade> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_PLACEMENT_REQUEST_RECORD_UNABLE_TO_MATCH)

    val user = userService.getUserForRequest()

    val result = placementRequestService.createBookingNotMade(
      user = user,
      placementRequestId = id,
      notes = newBookingNotMade.notes,
    )

    val bookingNotMade = extractEntityFromCasResult(result)

    return ResponseEntity(bookingNotMadeTransformer.transformJpaToApi(bookingNotMade), HttpStatus.OK)
  }

  override fun placementRequestsIdWithdrawalPost(id: UUID, body: WithdrawPlacementRequest?): ResponseEntity<PlacementRequestDetail> {
    val user = userService.getUserForRequest()

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

    val placementRequestAndCancellations = extractEntityFromCasResult(
      cas1WithdrawableService.withdrawPlacementRequest(
        id,
        user,
        reason,
      ),
    )

    return ResponseEntity.ok(toPlacementRequestDetail(user, placementRequestAndCancellations))
  }

  private fun toPlacementRequestDetail(
    forUser: UserEntity,
    placementRequestAndCancellations: PlacementRequestAndCancellations,
  ): PlacementRequestDetail {
    val personInfo = offenderDetailService.getPersonInfoResult(
      placementRequestAndCancellations.placementRequest.application.crn,
      forUser.cas1LaoStrategy(),
    )

    return placementRequestDetailTransformer.transformJpaToApi(
      placementRequestAndCancellations.placementRequest,
      personInfo,
      placementRequestAndCancellations.cancellations,
    )
  }

  private fun mapPersonDetailOntoPlacementRequests(placementRequests: List<PlacementRequestEntity>, user: UserEntity): List<PlacementRequest> = placementRequests.map {
    val personInfo = offenderDetailService.getPersonInfoResult(it.application.crn, user.cas1LaoStrategy())

    placementRequestTransformer.transformJpaToApi(it, personInfo)
  }
}
