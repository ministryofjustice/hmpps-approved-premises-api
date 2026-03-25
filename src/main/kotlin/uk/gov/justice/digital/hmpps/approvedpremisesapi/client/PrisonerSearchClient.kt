package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.json.JsonMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig

@Component
class PrisonerSearchClient(
  @Qualifier("prisonerSearchWebClient") webClientConfig: WebClientConfig,
  jsonMapper: JsonMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, jsonMapper, webClientCache) {

  fun getPrisoner(detailUrl: String) = getRequest<Prisoner> {
    path = detailUrl
  }
}

data class Prisoner(
  val prisonId: String,
  val prisonName: String,
)
