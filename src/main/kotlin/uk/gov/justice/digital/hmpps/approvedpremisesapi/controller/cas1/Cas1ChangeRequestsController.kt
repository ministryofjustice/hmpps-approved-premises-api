package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.ChangeRequestsCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1RejectChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection

@Service
class Cas1ChangeRequestsController : ChangeRequestsCas1Delegate {

  override fun create(placementRequestId: java.util.UUID, cas1NewChangeRequest: Cas1NewChangeRequest): ResponseEntity<Unit> = super.create(placementRequestId, cas1NewChangeRequest)

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
