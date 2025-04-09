package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository

@Service
class Cas1ApplicationService(
  private val applicationRepository: ApprovedPremisesApplicationRepository,
  private val offlineApplicationRepository: OfflineApplicationRepository,
) {
  fun getApplicationsForCrn(crn: String) = applicationRepository.findByCrn(crn)

  fun getOfflineApplicationsForCrn(crn: String) = offlineApplicationRepository.findAllByCrn(crn)
}
