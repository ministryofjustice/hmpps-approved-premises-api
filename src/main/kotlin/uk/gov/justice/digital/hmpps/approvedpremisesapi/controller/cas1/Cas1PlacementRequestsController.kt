package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.PlacementRequestsCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementRequestDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementRequestSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestService
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
  private val placementRequestService: Cas1PlacementRequestService,
  private val cas1PlacementRequestSummaryTransformer: Cas1PlacementRequestSummaryTransformer,
  private val placementRequestDetailTransformer: PlacementRequestDetailTransformer,
  private val offenderDetailService: OffenderDetailService,
  private val cas1ChangeRequestRepository: Cas1ChangeRequestRepository,
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
      Cas1PlacementRequestService.AllActiveSearchCriteria(
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

  override fun getPlacementRequest(id: UUID): ResponseEntity<Cas1PlacementRequestDetail> {
    val user = getUserForRequest()

    val placementRequest = extractEntityFromCasResult(placementRequestService.getPlacementRequest(user, id))

    return ResponseEntity.ok(toCas1PlacementRequestDetail(user, placementRequest))
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

    val changeRequests = cas1ChangeRequestRepository.findAllByPlacementRequestAndResolvedIsFalse(placementRequestEntity)

    return placementRequestDetailTransformer.transformJpaToCas1PlacementRequestDetail(
      placementRequestEntity,
      personInfo,
      changeRequests,
    )
  }
}
