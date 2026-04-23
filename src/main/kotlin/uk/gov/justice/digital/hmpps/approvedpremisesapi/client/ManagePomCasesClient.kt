package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig

@Component
class ManagePomCasesClient(
  @Qualifier("managePomCasesWebClient") webClientConfig: WebClientConfig,
  jsonMapper: JsonMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, jsonMapper, webClientCache) {

  fun getPomAllocation(detailUrl: String) = getRequest<PomAllocation> {
    path = detailUrl
  }
}

sealed interface AllocationResponse

data class PomAllocation(
  val manager: Manager,
  val prison: Prison,
) : AllocationResponse

data class Manager(
  val code: Long,
)

data class Prison(
  val code: String,
)
