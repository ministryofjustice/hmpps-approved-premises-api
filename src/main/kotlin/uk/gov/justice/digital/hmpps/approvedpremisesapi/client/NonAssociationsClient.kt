package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.nonassociations.NonAssociationsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig

@Component
class NonAssociationsClient(
  @Qualifier("nonAssociationsWebClient") webClientConfig: WebClientConfig,
  objectMapper: ObjectMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, objectMapper, webClientCache) {
  fun getNonAssociations(nomsNumber: String) = getRequest<NonAssociationsPage> {
    path = "/prisoner/$nomsNumber/non-associations"
  }
}
