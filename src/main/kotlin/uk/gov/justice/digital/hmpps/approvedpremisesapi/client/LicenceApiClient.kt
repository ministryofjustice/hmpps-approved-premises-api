package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.Licence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.LicenceSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig

@Component
class LicenceApiClient(
  @Qualifier("licenceApiWebClient") webClientConfig: WebClientConfig,
  objectMapper: ObjectMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, objectMapper, webClientCache) {

  fun getLicenceDetails(licenceId: String) = getRequest<Licence> {
    path = "/public/licences/id/$licenceId"
  }

  fun getLicenceSummaries(crn: String) = getRequest<List<LicenceSummary>> {
    path = "/public/licence-summaries/crn/$crn"
  }
}
