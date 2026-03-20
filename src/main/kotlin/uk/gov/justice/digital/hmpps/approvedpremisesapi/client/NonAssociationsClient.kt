package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.nonassociations.NonAssociationsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig

@Component
class NonAssociationsClient(
  @Qualifier("nonAssociationsWebClient") webClientConfig: WebClientConfig,
  jsonMapper: JsonMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, jsonMapper, webClientCache) {
  fun getNonAssociations(nomsNumber: String) = getRequest<NonAssociationsPage> {
    path = "/prisoner/$nomsNumber/non-associations"
  }
}
