package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRepository.FindOpenChangeRequestResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import java.util.UUID

@Service
class Cas1ChangeRequestService(
  val cas1ChangeRequestRepository: Cas1ChangeRequestRepository,
) {
  fun findOpen(
    cruManagementAreaId: UUID?,
    pageCriteria: PageCriteria<Cas1ChangeRequestSortField>,
  ): List<FindOpenChangeRequestResult> = cas1ChangeRequestRepository.findOpen(
    cruManagementAreaId,
    pageCriteria.toPageableOrAllPages(
      sortBy = when (pageCriteria.sortBy) {
        Cas1ChangeRequestSortField.NAME -> "name"
        Cas1ChangeRequestSortField.TIER -> "tier"
        Cas1ChangeRequestSortField.CANONICAL_ARRIVAL_DATE -> "canonicalArrivalDate"
        Cas1ChangeRequestSortField.LENGTH_OF_STAY_DAYS -> "lengthOfStayDays"
      },
    ),
  )
}
