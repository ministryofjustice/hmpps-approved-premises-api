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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ChangeRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ChangeRequestTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import java.util.UUID

@Service
class Cas1ChangeRequestsController(
  private val cas1ChangeRequestService: Cas1ChangeRequestService,
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val cas1ChangeRequestTransformer: Cas1ChangeRequestTransformer,
) : ChangeRequestsCas1Delegate {

  override fun create(placementRequestId: java.util.UUID, cas1NewChangeRequest: Cas1NewChangeRequest): ResponseEntity<Unit> = super.create(placementRequestId, cas1NewChangeRequest)

  override fun findOpen(
    page: Int?,
    cruManagementAreaId: UUID?,
    sortBy: Cas1ChangeRequestSortField?,
    sortDirection: SortDirection?,
  ): ResponseEntity<List<Cas1ChangeRequestSummary>> {
    // TODO: permission

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
