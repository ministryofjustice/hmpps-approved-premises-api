package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.BaseHMPPSClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CacheKeyResolver
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.MarshallableHttpMethod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.WebClientCache
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.client.PreemptiveCacheTest.Companion.CACHE_NAME
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import java.time.Duration
import java.time.Instant

class PreemptiveCacheTest : IntegrationTestBase() {

  companion object {
    const val CACHE_NAME = "inmateDetails"
  }

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
  fun `Making a request with isPreemptiveCall = true behaves correctly for 2xx responses`() {
    val firstCallInstant = Instant.parse("2023-04-25T11:35:00+01:00")
    val fourSecondsLaterInstant = Instant.parse("2023-04-25T11:35:04+01:00")
    val sixSecondsLaterInstant = Instant.parse("2023-04-25T11:35:06+01:00")

    val nomsNumber = "ABCD1234"

    val offenderDetailsResponseOne = InmateDetailFactory()
      .withOffenderNo(nomsNumber)
      .produce()

    val offenderDetailsResponseTwo = InmateDetailFactory()
      .withOffenderNo(nomsNumber)
      .produce()

    mockSuccessfulGetCallWithJsonResponse(
      url = "/api/offenders/$nomsNumber",
      responseBody = offenderDetailsResponseOne,
    )

    every { Instant.now() } returns firstCallInstant

    // The first call should make an upstream request
    val firstResult = preemptivelyCachedClient.getInmateDetailsWithCall(nomsNumber)
    assertThat(firstResult is ClientResult.Success).isTrue
    assertThat((firstResult as ClientResult.Success).body).isEqualTo(offenderDetailsResponseOne)
    wiremockServer.verify(exactly(1), getRequestedFor(urlEqualTo("/api/offenders/$nomsNumber")))

    // Subsequent calls up to successSoftTtlSeconds should return the cached value without making an upstream request
    every { Instant.now() } returns fourSecondsLaterInstant

    val secondResult = preemptivelyCachedClient.getInmateDetailsWithCall(nomsNumber)
    assertThat(secondResult is ClientResult.Success).isTrue
    assertThat((secondResult as ClientResult.Success).body).isEqualTo(offenderDetailsResponseOne)
    wiremockServer.verify(exactly(1), getRequestedFor(urlEqualTo("/api/offenders/$nomsNumber")))

    mockSuccessfulGetCallWithJsonResponse(
      url = "/api/offenders/$nomsNumber",
      responseBody = offenderDetailsResponseTwo,
    )

    // The next call after successSoftTtlSeconds should make an upstream request and replace the original cached value
    every { Instant.now() } returns sixSecondsLaterInstant

    val thirdResult = preemptivelyCachedClient.getInmateDetailsWithCall(nomsNumber)
    assertThat(thirdResult is ClientResult.Success).isTrue
    assertThat((thirdResult as ClientResult.Success).body).isEqualTo(offenderDetailsResponseTwo)
    wiremockServer.verify(exactly(2), getRequestedFor(urlEqualTo("/api/offenders/$nomsNumber")))

    // The next call should not make an upstream request and return the second cached value
    val fourthResult = preemptivelyCachedClient.getInmateDetailsWithCall(nomsNumber)
    assertThat(fourthResult is ClientResult.Success).isTrue
    assertThat((fourthResult as ClientResult.Success).body).isEqualTo(offenderDetailsResponseTwo)
    wiremockServer.verify(exactly(2), getRequestedFor(urlEqualTo("/api/offenders/$nomsNumber")))
  }

  /**
   * For this test the pre-emptive cache config is overridden from 'main' at the
   * bottom of this class
   *
   * failureSoftTtlBackoffSeconds = listOf(5, 10, 20),
   */
  @Test
  fun `Making a request with isPreemptiveCall = true behaves correctly for non-2xx responses`() {
    val firstCallInstant = Instant.parse("2023-04-25T11:35:00+01:00")
    val fourSecondsLaterInstant = Instant.parse("2023-04-25T11:35:04+01:00")
    val sixSecondsLaterInstant = Instant.parse("2023-04-25T11:35:06+01:00")

    val nomsNumber = "ABCD1234"

    // Call 1 - 404, add to cache
    // The first call should make an upstream request, attempt number in metadata should be set to 1
    mockUnsuccessfulGetCall(
      url = "/api/offenders/$nomsNumber",
      responseStatus = 404,
    )

    every { Instant.now() } returns firstCallInstant

    // Call 2 - 404 read from cache
    //
    // Subsequent calls up to the first amount of seconds in failureSoftTtlBackoffSeconds
    // should return the cached value without making an upstream request
    assertStatusCodeFailure(
      preemptivelyCachedClient.getInmateDetailsWithCall(nomsNumber),
      HttpStatus.NOT_FOUND,
    )
    assertMetadataAttemptCount("$preemptiveCacheKeyPrefix-inmateDetails-$nomsNumber-metadata", MarshallableHttpMethod.GET, 1)
    assertCallCount("/api/offenders/$nomsNumber", 1)

    // Call 3 - 404 read from cache
    //
    // Subsequent calls up to the first amount of seconds in failureSoftTtlBackoffSeconds
    // should return the cached value without making an upstream request
    every { Instant.now() } returns fourSecondsLaterInstant

    assertStatusCodeFailure(
      preemptivelyCachedClient.getInmateDetailsWithCall(nomsNumber),
      HttpStatus.NOT_FOUND,
    )
    assertCallCount("/api/offenders/$nomsNumber", 1)

    mockUnsuccessfulGetCall(
      url = "/api/offenders/$nomsNumber",
      responseStatus = 400,
    )

    // Call 4 - 400 refresh cache
    //
    // The next call after successSoftTtlSeconds should make an upstream request and
    // replace the original cached value, attempt number in metadata should increase to 2
    every { Instant.now() } returns sixSecondsLaterInstant

    assertStatusCodeFailure(
      preemptivelyCachedClient.getInmateDetailsWithCall(nomsNumber),
      HttpStatus.BAD_REQUEST,
    )
    assertMetadataAttemptCount("$preemptiveCacheKeyPrefix-inmateDetails-$nomsNumber-metadata", MarshallableHttpMethod.GET, 2)
    assertCallCount("/api/offenders/$nomsNumber", 2)

    // Call 5 - 400 read from cache
    //
    // The next call should not make an upstream request and return the cached value
    assertStatusCodeFailure(
      preemptivelyCachedClient.getInmateDetailsWithCall(nomsNumber),
      HttpStatus.BAD_REQUEST,
    )

    assertCallCount("/api/offenders/$nomsNumber", 2)
  }

  private fun assertStatusCodeFailure(result: ClientResult<*>, expectedStatus: HttpStatus) {
    assertThat(result is ClientResult.Failure.StatusCode).isTrue
    assertThat((result as ClientResult.Failure.StatusCode).status).isEqualTo(expectedStatus)
  }

  private fun assertCallCount(
    url: String,
    expectedCount: Int,
  ) {
    wiremockServer.verify(exactly(expectedCount), getRequestedFor(urlEqualTo(url)))
  }

  private fun assertMetadataAttemptCount(
    key: String,
    method: MarshallableHttpMethod,
    attempts: Int,
  ) {
    val metadata = objectMapper.readValue<WebClientCache.PreemptiveCacheMetadata>(
      redisTemplate.boundValueOps(key).get()!!,
    )

    assertThat(metadata.attempt).isEqualTo(attempts)
    assertThat(metadata.method).isEqualTo(method)
  }

  @Test
  fun `Making a request with isPreemptiveCall = false behaves correctly`() {
    val nomsNumber = "ABCD1234"

    val offenderDetailsResponse = InmateDetailFactory()
      .withOffenderNo(nomsNumber)
      .produce()

    mockSuccessfulGetCallWithJsonResponse(
      url = "/api/offenders/$nomsNumber",
      responseBody = offenderDetailsResponse,
    )

    // The first call should return a ClientResult.Failure.PreemptiveCacheTimeout as no cache entry is present in Redis
    val firstResult = preemptivelyCachedClient.getInmateDetailsWithWait(nomsNumber)
    assertThat(firstResult is ClientResult.Failure.PreemptiveCacheTimeout).isTrue

    Thread {
      Thread.sleep(500)
      val qualifiedMetadataKey = "inmateDetails-$nomsNumber-metadata"
      val qualifiedDataKey = "inmateDetails-$nomsNumber-metadata"

      val cacheEntry = WebClientCache.PreemptiveCacheMetadata(
        httpStatus = HttpStatus.OK,
        refreshableAfter = Instant.now().plusSeconds(10),
        method = null,
        path = null,
        hasResponseBody = true,
        attempt = null,
      )

      redisTemplate.boundValueOps(qualifiedMetadataKey).set(
        objectMapper.writeValueAsString(cacheEntry),
        Duration.ofSeconds(10),
      )

      redisTemplate.boundValueOps(qualifiedDataKey).set(
        objectMapper.writeValueAsString(offenderDetailsResponse),
        Duration.ofSeconds(10),
      )
    }.start()

    // The second call should return a ClientResult.Success after a wait as the cache entry appears in Redis
    val secondResult = preemptivelyCachedClient.getInmateDetailsWithCall(nomsNumber)
    assertThat(secondResult is ClientResult.Success).isTrue
    assertThat((secondResult as ClientResult.Success).body).isEqualTo(offenderDetailsResponse)
  }

  @Test
  fun `A cache failure due to missing data is handled correctly`() {
    val firstCallInstant = Instant.parse("2023-04-25T11:35:00+01:00")
    val fourSecondsLaterInstant = Instant.parse("2023-04-25T11:35:04+01:00")

    val nomsNumber = "ABCD1234"

    val offenderDetailsResponse = InmateDetailFactory()
      .withOffenderNo(nomsNumber)
      .produce()

    mockSuccessfulGetCallWithJsonResponse(
      url = "/api/offenders/$nomsNumber",
      responseBody = offenderDetailsResponse,
    )

    every { Instant.now() } returns firstCallInstant

    // The first call should make an upstream request
    val firstResult = preemptivelyCachedClient.getInmateDetailsWithCall(nomsNumber)
    assertThat(firstResult is ClientResult.Success).isTrue
    assertThat((firstResult as ClientResult.Success).body).isEqualTo(offenderDetailsResponse)
    wiremockServer.verify(exactly(1), getRequestedFor(urlEqualTo("/api/offenders/$nomsNumber")))

    // Subsequent calls up to successSoftTtlSeconds should return the cached value without making an upstream request
    every { Instant.now() } returns fourSecondsLaterInstant

    val secondResult = preemptivelyCachedClient.getInmateDetailsWithCall(nomsNumber)
    assertThat(secondResult is ClientResult.Success).isTrue
    assertThat((secondResult as ClientResult.Success).body).isEqualTo(offenderDetailsResponse)
    wiremockServer.verify(exactly(1), getRequestedFor(urlEqualTo("/api/offenders/$nomsNumber")))

    // Simulate https://ministryofjustice.sentry.io/issues/4479884804 by deleting the data key from the cache while
    // preserving the metadata key.
    val keys = CacheKeyResolver(preemptiveCacheKeyPrefix, CACHE_NAME, nomsNumber)
    redisTemplate.delete(keys.dataKey)

    val thirdResult = preemptivelyCachedClient.getInmateDetailsWithCall(nomsNumber)
    assertThat(thirdResult is ClientResult.Failure.CachedValueUnavailable).isTrue
    assertThat((thirdResult as ClientResult.Failure.CachedValueUnavailable).cacheKey).isEqualTo(keys.dataKey)
    wiremockServer.verify(exactly(1), getRequestedFor(urlEqualTo("/api/offenders/$nomsNumber")))
  }
}

@Component
class PreemptivelyCachedClient(
  @Qualifier("prisonsApiWebClient") private val webClient: WebClientConfig,
  objectMapper: ObjectMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClient, objectMapper, webClientCache) {
  private val cacheConfig = WebClientCache.PreemptiveCacheConfig(
    cacheName = CACHE_NAME,
    successSoftTtlSeconds = 5,
    successSoftTtlJitterSeconds = 2,
    failureSoftTtlBackoffSeconds = listOf(5, 10, 20),
    hardTtlSeconds = 30,
  )

  fun getInmateDetailsWithCall(nomsNumber: String) = getRequest<InmateDetail> {
    path = "/api/offenders/$nomsNumber"
    isPreemptiveCall = true
    preemptiveCacheKey = nomsNumber
    preemptiveCacheConfig = cacheConfig
  }

  fun getInmateDetailsWithWait(nomsNumber: String) = getRequest<InmateDetail> {
    isPreemptiveCall = false
    preemptiveCacheKey = nomsNumber
    preemptiveCacheConfig = cacheConfig
    preemptiveCacheTimeoutMs = 10_000
  }
}
