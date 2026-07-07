package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.json.JsonMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.TierVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig

@Component
class HMPPSTierApiClient(
  @Qualifier("hmppsTierApiWebClient") webClientConfig: WebClientConfig,
  jsonMapper: JsonMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, jsonMapper, webClientCache) {
  fun getTier(crn: String) = getRequest<Tier> {
    path = "/crn/$crn/tier"
  }

  fun getTierV3(crn: String) = getRequest<Tier> {
    path = "/v3/crn/$crn/tier"
  }

  fun getTier(crn: String, version: TierVersion) = when (version) {
    TierVersion.V2 -> getTier(crn)
    TierVersion.V3 -> getTierV3(crn)
  }
}
