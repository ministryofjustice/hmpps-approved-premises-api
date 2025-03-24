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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ChangeRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import java.util.UUID

@Service
class Cas1ChangeRequestsController(
  private val userAccessService: UserAccessService,
  private val cas1ChangeRequestService: Cas1ChangeRequestService,
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

  override fun findOpen(page: kotlin.Int?, sortBy: Cas1ChangeRequestSortField?, sortDirection: SortDirection?): ResponseEntity<List<Cas1ChangeRequestSummary>> = super.findOpen(page, sortBy, sortDirection)

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
