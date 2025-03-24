package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig

@Component
class ManagePomCasesClient(
  @Qualifier("managePomCasesWebClient") webClientConfig: WebClientConfig,
  objectMapper: ObjectMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, objectMapper, webClientCache) {

  fun getPomAllocation(detailUrl: String) = getRequest<PomAllocation> {
    path = detailUrl
  }
}

sealed interface AllocationResponse

data class PomAllocation(
  val manager: Manager,
  val prison: Prison,
) : AllocationResponse

data object PomDeallocated : AllocationResponse

data object PomNotAllocated : AllocationResponse

data class Manager(
  val code: Long,
)

data class Prison(
  val code: String,
)
