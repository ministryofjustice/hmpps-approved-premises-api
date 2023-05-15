package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.BaseHMPPSClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import java.time.Duration
import java.time.Instant

class PreemptiveCacheTest : IntegrationTestBase() {

  @Autowired
  lateinit var preemptivelyCachedClient: PreemptivelyCachedClient

  @BeforeEach
  fun mockInstant() {
    mockkStatic(Instant::class)
  }

  @AfterEach
  fun unmockInstant() {
    unmockkStatic(Instant::class)
  }

  @Test
  fun `Making a request with isPreemptiveCall = true behaves correctly`() {
    val firstCallInstant = Instant.parse("2023-04-25T11:35:00+01:00")
    val fourSecondsLaterInstant = Instant.parse("2023-04-25T11:35:04+01:00")
    val sixSecondsLaterInstant = Instant.parse("2023-04-25T11:35:06+01:00")

    val crn = "ABCD1234"

    val offenderDetailsResponseOne = OffenderDetailsSummaryFactory()
      .withCrn(crn)
      .produce()

    val offenderDetailsResponseTwo = OffenderDetailsSummaryFactory()
      .withCrn(crn)
      .produce()

    mockSuccessfulGetCallWithJsonResponse(
      url = "/secure/offenders/crn/$crn",
      responseBody = offenderDetailsResponseOne
    )

    every { Instant.now() } returns firstCallInstant

    // The first call should make an upstream request
    val firstResult = preemptivelyCachedClient.getOffenderDetailSummaryWithCall(crn)
    assertThat(firstResult is ClientResult.Success).isTrue
    assertThat((firstResult as ClientResult.Success).body).isEqualTo(offenderDetailsResponseOne)
    wiremockServer.verify(exactly(1), getRequestedFor(urlEqualTo("/secure/offenders/crn/${offenderDetailsResponseOne.otherIds.crn}")))

    // Subsequent calls up to successSoftTtlSeconds should return the cached value without making an upstream request
    every { Instant.now() } returns fourSecondsLaterInstant

    val secondResult = preemptivelyCachedClient.getOffenderDetailSummaryWithCall(crn)
    assertThat(secondResult is ClientResult.Success).isTrue
    assertThat((secondResult as ClientResult.Success).body).isEqualTo(offenderDetailsResponseOne)
    wiremockServer.verify(exactly(1), getRequestedFor(urlEqualTo("/secure/offenders/crn/${offenderDetailsResponseOne.otherIds.crn}")))

    mockSuccessfulGetCallWithJsonResponse(
      url = "/secure/offenders/crn/$crn",
      responseBody = offenderDetailsResponseTwo
    )

    // The next call after successSoftTtlSeconds should make an upstream request and replace the original cached value
    every { Instant.now() } returns sixSecondsLaterInstant

    val thirdResult = preemptivelyCachedClient.getOffenderDetailSummaryWithCall(crn)
    assertThat(thirdResult is ClientResult.Success).isTrue
    assertThat((thirdResult as ClientResult.Success).body).isEqualTo(offenderDetailsResponseTwo)
    wiremockServer.verify(exactly(2), getRequestedFor(urlEqualTo("/secure/offenders/crn/${offenderDetailsResponseOne.otherIds.crn}")))

    // The next call should not make an upstream request and return the second cached value
    val fourthResult = preemptivelyCachedClient.getOffenderDetailSummaryWithCall(crn)
    assertThat(fourthResult is ClientResult.Success).isTrue
    assertThat((fourthResult as ClientResult.Success).body).isEqualTo(offenderDetailsResponseTwo)
    wiremockServer.verify(exactly(2), getRequestedFor(urlEqualTo("/secure/offenders/crn/${offenderDetailsResponseOne.otherIds.crn}")))
  }

  @Test
  fun `Making a request with isPreemptiveCall = false behaves correctly`() {
    val crn = "ABCD1234"

    val offenderDetailsResponse = OffenderDetailsSummaryFactory()
      .withCrn(crn)
      .produce()

    mockSuccessfulGetCallWithJsonResponse(
      url = "/secure/offenders/crn/$crn",
      responseBody = offenderDetailsResponse
    )

    // The first call should return a ClientResult.Failure.PreemptiveCacheTimeout as no cache entry is present in Redis
    val firstResult = preemptivelyCachedClient.getOffenderDetailSummaryWithWait(crn)
    assertThat(firstResult is ClientResult.Failure.PreemptiveCacheTimeout).isTrue

    Thread {
      Thread.sleep(500)

      val qualifiedKey = "offenderDetailSummary-$crn"

      val cacheEntry = BaseHMPPSClient.PreemptiveCacheEntry(
        httpStatus = HttpStatus.OK,
        refreshableAfter = Instant.now().plusSeconds(10),
        body = objectMapper.writeValueAsString(offenderDetailsResponse),
        method = null,
        path = null
      )

      redisTemplate.boundValueOps(qualifiedKey).set(
        objectMapper.writeValueAsString(cacheEntry),
        Duration.ofSeconds(10)
      )
    }.start()

    // The second call should return a ClientResult.Success after a wait as the cache entry appears in Redis
    val secondResult = preemptivelyCachedClient.getOffenderDetailSummaryWithCall(crn)
    assertThat(secondResult is ClientResult.Success).isTrue
    assertThat((secondResult as ClientResult.Success).body).isEqualTo(offenderDetailsResponse)
  }
}

@Component
class PreemptivelyCachedClient(
  @Qualifier("communityApiWebClient") private val webClient: WebClient,
  objectMapper: ObjectMapper,
  redisTemplate: RedisTemplate<String, String>,
  @Value("\${preemptive-cache-key-prefix}") preemptiveCacheKeyPrefix: String,
) : BaseHMPPSClient(webClient, objectMapper, redisTemplate, preemptiveCacheKeyPrefix) {
  private val cacheConfig = PreemptiveCacheConfig(
    cacheName = "offenderDetailSummary",
    successSoftTtlSeconds = 5,
    failureSoftTtlSeconds = 1,
    hardTtlSeconds = 30
  )

  fun getOffenderDetailSummaryWithCall(crn: String) = getRequest<OffenderDetailSummary> {
    path = "/secure/offenders/crn/$crn"
    isPreemptiveCall = true
    preemptiveCacheKey = crn
    preemptiveCacheConfig = cacheConfig
  }

  fun getOffenderDetailSummaryWithWait(crn: String) = getRequest<OffenderDetailSummary> {
    isPreemptiveCall = false
    preemptiveCacheKey = crn
    preemptiveCacheConfig = cacheConfig
    preemptiveCacheTimeoutMs = 10_000
  }
}
