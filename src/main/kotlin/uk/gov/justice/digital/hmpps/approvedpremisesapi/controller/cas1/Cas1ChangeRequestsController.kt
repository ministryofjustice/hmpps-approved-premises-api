package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.ChangeRequestsCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1RejectChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission.CAS1_CHANGE_REQUEST_LIST
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ChangeRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ChangeRequestTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import java.util.UUID

@Service
class Cas1ChangeRequestsController(
  private val cas1ChangeRequestService: Cas1ChangeRequestService,
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val cas1ChangeRequestTransformer: Cas1ChangeRequestTransformer,
  private val userAccessService: UserAccessService,
) : ChangeRequestsCas1Delegate {

  override fun create(placementRequestId: UUID, cas1NewChangeRequest: Cas1NewChangeRequest): ResponseEntity<Unit> {
    when (cas1NewChangeRequest.type) {
      Cas1ChangeRequestType.APPEAL -> userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_APPEAL_CREATE)
      Cas1ChangeRequestType.PLANNED_TRANSFER -> userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_PLANNED_TRANSFER_CREATE)
      Cas1ChangeRequestType.EXTENSION -> throw BadRequestProblem(errorDetail = "Change request type is not ${Cas1ChangeRequestType.PLANNED_TRANSFER} or ${Cas1ChangeRequestType.APPEAL}")
    }

    val result = cas1ChangeRequestService.createChangeRequest(placementRequestId, cas1NewChangeRequest)

    ensureEntityFromCasResultIsSuccess(result)

    return ResponseEntity(HttpStatus.OK)
  }

  override fun findOpen(
    page: Int?,
    cruManagementAreaId: UUID?,
    sortBy: Cas1ChangeRequestSortField?,
    sortDirection: SortDirection?,
  ): ResponseEntity<List<Cas1ChangeRequestSummary>> {
    userAccessService.ensureCurrentUserHasPermission(CAS1_CHANGE_REQUEST_LIST)

    val results = cas1ChangeRequestService.findOpen(
      cruManagementAreaId,
      PageCriteria(
        sortBy = sortBy ?: Cas1ChangeRequestSortField.NAME,
        sortDirection = sortDirection ?: SortDirection.asc,
        page = page,
      ),
    )

    val offenderSummaries = offenderService.getPersonSummaryInfoResults(
      crns = results.map { it.crn }.toSet(),
      laoStrategy = userService.getUserForRequest().cas1LaoStrategy(),
    ).associateBy { it.crn }

    return ResponseEntity.ok(
      results
        .map {
          cas1ChangeRequestTransformer.findOpenResultsToChangeRequestSummary(
            result = it,
            person = offenderSummaries[it.crn]!!,
          )
        },
    )
  }

  override fun get(
    placementRequestId: java.util.UUID,
    changeRequestId: java.util.UUID,
  ): ResponseEntity<List<Cas1ChangeRequest>> = super.get(placementRequestId, changeRequestId)

  override fun reject(
    placementRequestId: java.util.UUID,
    changeRequestId: java.util.UUID,
    cas1RejectChangeRequest: Cas1RejectChangeRequest,
  ): ResponseEntity<List<Cas1ChangeRequest>> = super.reject(placementRequestId, changeRequestId, cas1RejectChangeRequest)
}
