package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.json.JsonMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.TierVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CaseService
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

  fun getV2TiersForCrns(crnList: List<String>) = postRequest<Map<String, Tier?>> {
    path = "/v2/crns/tier"
    body = jsonMapper.writeValueAsString(crnList)
  }

  fun getV3TiersForCrns(crnList: List<String>) = postRequest<Map<String, Tier?>> {
    path = "/v3/crns/tier"
    body = jsonMapper.writeValueAsString(crnList)
  }

  fun getTiers(crnList: CaseService.VersionedCaseList) = when (crnList.version) {
    TierVersion.V2 -> getV2TiersForCrns(crnList.crnList)
    TierVersion.V3 -> getV3TiersForCrns(crnList.crnList)
  }
}
