package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.BaseHMPPSClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.WebClientCache
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary

class WebClientBufferingTest : IntegrationTestBase() {
  @Autowired
  lateinit var needsBufferingWebClient: NeedsBufferingWebClient

  @Value("\${max-response-in-memory-size-bytes}")
  var maxResponseInMemorySizeBytes: Int = 0

  @Test
  fun `Very large responses from a downstream request are streamed instead of throwing a DataBufferLimitException`() {
    val expected = OffenderDetailsSummaryFactory().produceMany().take(1000).toList()

    assertThat(objectMapper.writeValueAsString(expected).length).isGreaterThan(maxResponseInMemorySizeBytes)

    mockSuccessfulGetCallWithJsonResponse(
      "/example",
      expected,
    )

    val result = needsBufferingWebClient.getBigPileOfData()

    assertThat(result is ClientResult.Success).isTrue
    result as ClientResult.Success
    assertThat(result.body).isEqualTo(expected)
  }
}

@Component
class NeedsBufferingWebClient(
  @Qualifier("communityApiWebClient") private val webClient: WebClient,
  objectMapper: ObjectMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClient, objectMapper, webClientCache) {
  private val cacheConfig = WebClientCache.PreemptiveCacheConfig(
    cacheName = "offenderDetailSummary",
    successSoftTtlSeconds = 5,
    failureSoftTtlBackoffSeconds = listOf(5, 10, 20),
    hardTtlSeconds = 30,
  )

  fun getBigPileOfData() = getRequest<List<OffenderDetailSummary>> {
    path = "/example"
    isPreemptiveCall = true
    preemptiveCacheKey = "bigPileOfData"
    preemptiveCacheConfig = cacheConfig
  }
}
