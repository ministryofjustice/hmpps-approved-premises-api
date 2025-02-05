package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.PlacementRequestsCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementRequestSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LimitedAccessStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PlacementRequestSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import java.time.LocalDate
import java.util.UUID

@Service
class Cas1PlacementRequestsController(
  private val userService: UserService,
  private val placementRequestService: PlacementRequestService,
  private val cas1PlacementRequestSummaryTransformer: Cas1PlacementRequestSummaryTransformer,
  private val offenderService: OffenderService,
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
    val user = userService.getUserForRequest()

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
  ): List<Cas1PlacementRequestSummary> {
    return placementRequests.map {
      cas1PlacementRequestSummaryTransformer.transformCas1PlacementRequestSummaryJpaToApi(
        it,
        offenderService.getPersonInfoResult(it.getPersonCrn(), user.cas1LimitedAccessStrategy()),
      )
    }
  }
}
