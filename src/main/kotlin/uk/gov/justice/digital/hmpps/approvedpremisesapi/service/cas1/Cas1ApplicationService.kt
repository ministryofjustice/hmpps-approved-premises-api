package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.data.domain.Limit
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageableOrAllPages
import java.util.UUID

@Service
class Cas1ApplicationService(
  private val approvedPremisesApplicationRepository: ApprovedPremisesApplicationRepository,
  private val applicationRepository: ApplicationRepository,
  private val offlineApplicationRepository: OfflineApplicationRepository,
) {
  fun getApplicationsForCrn(crn: String, limit: Int) = approvedPremisesApplicationRepository.findByCrn(crn, Limit.of(limit))

  fun getOfflineApplicationsForCrn(crn: String, limit: Int) = offlineApplicationRepository.findAllByCrn(crn, Limit.of(limit))

  fun getAllApprovedPremisesApplications(
    page: Int?,
    crnOrName: String?,
    sortDirection: SortDirection?,
    status: List<ApprovedPremisesApplicationStatus>,
    sortBy: ApplicationSortField?,
    apAreaId: UUID?,
    releaseType: String?,
    pageSize: Int? = 10,
    createdByUserId: UUID? = null,
  ): Pair<List<ApprovedPremisesApplicationSummary>, PaginationMetadata?> {
    val sortField = when (sortBy) {
      ApplicationSortField.arrivalDate -> "arrivalDate"
      ApplicationSortField.createdAt -> "a.created_at"
      ApplicationSortField.tier -> "tier"
      ApplicationSortField.releaseType -> "releaseType"
      else -> "a.created_at"
    }
    val pageable = getPageableOrAllPages(sortField, sortDirection, page, pageSize)

    val statusNames = status.map { it.name }

    val response = applicationRepository.findAllApprovedPremisesSummaries(
      pageable = pageable,
      crnOrName = crnOrName,
      statusProvided = statusNames.isNotEmpty(),
      status = statusNames,
      apAreaId = apAreaId,
      releaseType,
      createdByUserId,
    )

    return Pair(response.content, getMetadata(response, page, pageSize))
  }
}
