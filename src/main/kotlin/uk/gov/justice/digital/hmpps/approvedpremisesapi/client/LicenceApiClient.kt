package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.json.JsonMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.Licence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.LicenceSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig

@Component
class LicenceApiClient(
  @Qualifier("licenceApiWebClient") webClientConfig: WebClientConfig,
  jsonMapper: JsonMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, jsonMapper, webClientCache) {

  fun getLicenceDetails(licenceId: Long) = getRequest<Licence> {
    path = "/public/licences/id/$licenceId"
  }

  fun getLicenceSummaries(crn: String) = getRequest<List<LicenceSummary>> {
    path = "/public/licence-summaries/crn/$crn"
  }
}
