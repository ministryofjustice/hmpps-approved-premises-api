package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.ChangeRequestsCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection

@Service
class Cas1ChangeRequestsController : ChangeRequestsCas1Delegate {

  override fun create(cas1NewChangeRequest: Cas1NewChangeRequest): ResponseEntity<Unit> = super.create(cas1NewChangeRequest)

  override fun find(
    status: Cas1ChangeRequestStatus?,
    page: Int?,
    sortBy: Cas1ChangeRequestSortField?,
    sortDirection: SortDirection?,
  ): ResponseEntity<List<Cas1ChangeRequestSummary>> = super.find(status, page, sortBy, sortDirection)
}
