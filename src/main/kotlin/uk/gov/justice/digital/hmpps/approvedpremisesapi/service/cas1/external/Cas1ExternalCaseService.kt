package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalCaseSummaryDto

@Service
class Cas1ExternalCaseService(
  private val cas1ExternalApplicationV2Service: Cas1ExternalApplicationV2Service,
) {

  fun getCaseSummary(crn: String): Cas1ExternalCaseSummaryDto? {
    val currentApplication = cas1ExternalApplicationV2Service.getSuitableApplicationByCrn(crn)
    val residence = cas1ExternalApplicationV2Service.getCurrentResidentPlacement(crn)

    return if (currentApplication != null || residence != null) {
      Cas1ExternalCaseSummaryDto(currentApplication, residence)
    } else {
      null
    }
  }
}
