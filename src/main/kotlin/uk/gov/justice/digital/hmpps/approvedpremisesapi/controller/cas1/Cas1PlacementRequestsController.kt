package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.PlacementRequestsCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementRequestDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementRequestSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1WithdrawPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotAllowedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestDetailTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PlacementRequestSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.time.LocalDate
import java.util.UUID

@Service
class Cas1PlacementRequestsController(
  private val userService: UserService,
  private val placementRequestService: PlacementRequestService,
  private val cas1PlacementRequestSummaryTransformer: Cas1PlacementRequestSummaryTransformer,
  private val placementRequestDetailTransformer: PlacementRequestDetailTransformer,
  private val offenderService: OffenderService,
  private val cas1WithdrawableService: Cas1WithdrawableService,
) : PlacementRequestsCas1Delegate {

  override fun search(
    status: PlacementRequestStatus?,
    crnOrName: String?,
    tier: RiskTierLevel?,
    arrivalDateStart: LocalDate?,
    arrivalDateEnd: LocalDate?,
    requestType: PlacementRequestRequestType?,
    cruManagementAreaId: UUID?,
    page: Int?,
    sortBy: PlacementRequestSortField?,
    sortDirection: SortDirection?,
  ): ResponseEntity<List<Cas1PlacementRequestSummary>> {
    val user = getUserForRequest()

    if (!user.hasPermission(UserPermission.CAS1_VIEW_CRU_DASHBOARD)) {
      throw ForbiddenProblem()
    }

    val (requests, metadata) = placementRequestService.getAllCas1Active(
      PlacementRequestService.AllActiveSearchCriteria(
        status = status,
        crnOrName = crnOrName,
        tier = tier?.value,
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
  ): List<Cas1PlacementRequestSummary> = placementRequests.map {
    cas1PlacementRequestSummaryTransformer.transformCas1PlacementRequestSummaryJpaToApi(
      it,
      offenderService.getPersonInfoResult(it.getPersonCrn(), user.cas1LaoStrategy()),
    )
  }

  override fun getPlacementRequest(id: UUID): ResponseEntity<Cas1PlacementRequestDetail> {
    val user = getUserForRequest()

    val placementRequest = extractEntityFromCasResult(placementRequestService.getPlacementRequest(user, id))

    return ResponseEntity.ok(toCas1PlacementRequestDetail(user, placementRequest))
  }

  override fun withdrawPlacementRequest(
    id: UUID,
    body: Cas1WithdrawPlacementRequest?,
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

    val (placementRequest, _) = extractEntityFromCasResult(
      cas1WithdrawableService.withdrawPlacementRequest(
        id,
        user,
        reason,
      ),
    )

    return ResponseEntity.ok(toCas1PlacementRequestDetail(user, placementRequest))
  }

  private fun getUserForRequest(): UserEntity = userService.getUserForRequest()

  private fun toCas1PlacementRequestDetail(
    forUser: UserEntity,
    placementRequestEntity: PlacementRequestEntity,
  ): Cas1PlacementRequestDetail {
    val personInfo = offenderService.getPersonInfoResult(
      placementRequestEntity.application.crn,
      forUser.cas1LaoStrategy(),
    )

    return placementRequestDetailTransformer.transformJpaToCas1PlacementRequestDetail(
      placementRequestEntity,
      personInfo,
    )
  }
}
