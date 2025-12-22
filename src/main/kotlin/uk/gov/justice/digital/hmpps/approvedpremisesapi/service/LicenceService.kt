package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.LicenceApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.Licence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.LicenceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.toCasResult

@Service
class LicenceService(
  private val licenceApiClient: LicenceApiClient,
) {
  fun getLicence(crn: String): CasResult<Licence> {
    val summaries = when (
      val summariesResult = licenceApiClient
        .getLicenceSummaries(crn)
        .toCasResult(entityType = "Licence", id = crn)
    ) {
      is CasResult.Success -> summariesResult.value
      is CasResult.Error -> return summariesResult.reviseType()
    }

    val activeSummary = summaries.firstOrNull { it.statusCode == LicenceStatus.ACTIVE }
      ?: return CasResult.NotFound(entityType = "Licence", id = crn)

    return licenceApiClient
      .getLicenceDetails(activeSummary.id)
      .toCasResult(entityType = "Licence", id = crn)
  }
}
