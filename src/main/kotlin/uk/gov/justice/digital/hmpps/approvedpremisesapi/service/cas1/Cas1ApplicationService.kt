package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.data.domain.Limit
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository

@Service
class Cas1ApplicationService(
  private val applicationRepository: ApprovedPremisesApplicationRepository,
  private val offlineApplicationRepository: OfflineApplicationRepository,
) {
  fun getApplicationsForCrn(crn: String, limit: Int) = applicationRepository.findByCrn(crn, Limit.of(limit))

  fun getOfflineApplicationsForCrn(crn: String, limit: Int) = offlineApplicationRepository.findAllByCrn(crn, Limit.of(limit))
}
