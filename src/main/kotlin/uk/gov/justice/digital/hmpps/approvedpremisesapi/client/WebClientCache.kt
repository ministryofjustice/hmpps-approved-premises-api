package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.minusRandomSeconds
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

@Component
class WebClientCache(
  private val objectMapper: ObjectMapper,
  private val redisTemplate: RedisTemplate<String, String>,
  @Value("\${preemptive-cache-key-prefix}") private val preemptiveCacheKeyPrefix: String,
  private val sentryService: SentryService,
) {

  fun checkPreemptiveCacheStatus(cacheConfig: PreemptiveCacheConfig, key: String): PreemptiveCacheEntryStatus {
    val cacheKeyResolver = CacheKeyResolver(preemptiveCacheKeyPrefix, cacheConfig.cacheName, key)

    val cacheEntryMetadata = getCacheEntryMetadataIfExists(cacheKeyResolver.metadataKey)
      ?: return PreemptiveCacheEntryStatus.MISS

    val refreshableAfter = cacheEntryMetadata.refreshableAfter
    val refreshableAfterWithJitter = refreshableAfter.minusRandomSeconds(cacheConfig.successSoftTtlJitterSeconds)

    if (refreshableAfterWithJitter < Instant.now()) {
      return PreemptiveCacheEntryStatus.REQUIRES_REFRESH
    } else {
      return PreemptiveCacheEntryStatus.EXISTS
    }
  }

  fun <ResponseType : Any> tryGetCachedValue(
    typeReference: TypeReference<ResponseType>,
    requestBuilder: BaseHMPPSClient.HMPPSRequestConfiguration,
    cacheConfig: PreemptiveCacheConfig,
    attempt: AtomicInteger,
  ): ClientResult<ResponseType>? {
    val cacheKeySet = getCacheKeySet(requestBuilder, cacheConfig)

    if (!requestBuilder.isPreemptiveCall) {
      return pollCacheWithBlockingWait(cacheKeySet, typeReference, requestBuilder, cacheConfig)
    }

    val cacheEntry = getCacheEntryMetadataIfExists(cacheKeySet.metadataKey)

    attempt.set(cacheEntry?.attempt?.plus(1) ?: 1)

    if (cacheEntry != null && cacheEntry.refreshableAfter.isAfter(Instant.now())) {
      return resultFromCacheMetadata(cacheEntry, cacheKeySet, typeReference)
    }

    return null
  }

  fun cacheFailedWebClientResponse(
    requestBuilder: BaseHMPPSClient.HMPPSRequestConfiguration,
    cacheConfig: PreemptiveCacheConfig,
    exception: WebClientResponseException,
    attempt: Int,
    method: HttpMethod,
  ) {
    val qualifiedKey = getCacheKeySet(requestBuilder, cacheConfig)

    val body: String? = exception.responseBodyAsString

    val backoffSeconds = if (attempt <= cacheConfig.failureSoftTtlBackoffSeconds.size) {
      cacheConfig.failureSoftTtlBackoffSeconds[attempt - 1].toLong()
    } else {
      cacheConfig.failureSoftTtlBackoffSeconds.last().toLong()
    }

    val path = requestBuilder.path ?: ""

    val cacheEntry = PreemptiveCacheMetadata(
      httpStatus = exception.statusCode.toHttpStatus(),
      refreshableAfter = Instant.now().plusSeconds(backoffSeconds),
      method = MarshallableHttpMethod.fromHttpMethod(method),
      path = path,
      hasResponseBody = body != null,
      attempt = attempt,
    )

    if (attempt >= FAILED_ATTEMPT_WARN_THRESHOLD) {
      sentryService.captureException(
        RuntimeException(
          "Unable to make upstream request to refresh cache after $attempt attempts. Path is $path",
          exception,
        ),
      )
    }

    writeToRedis(qualifiedKey, cacheEntry, body, cacheConfig.hardTtlSeconds.toLong())
  }

  fun cacheSuccessfulWebClientResponse(
    requestBuilder: BaseHMPPSClient.HMPPSRequestConfiguration,
    cacheConfig: PreemptiveCacheConfig,
    result: ResponseEntity<String>,
  ) {
    val cacheKeySet = getCacheKeySet(requestBuilder, cacheConfig)

    val cacheEntry = PreemptiveCacheMetadata(
      httpStatus = result.statusCode.toHttpStatus(),
      refreshableAfter = Instant.now().plusSeconds(cacheConfig.successSoftTtlSeconds.toLong()),
      method = null,
      path = null,
      hasResponseBody = result.body != null,
      attempt = null,
    )

    writeToRedis(cacheKeySet, cacheEntry, result.body, cacheConfig.hardTtlSeconds.toLong())
  }

  private fun <ResponseType : Any> pollCacheWithBlockingWait(
    cacheKeyResolver: CacheKeyResolver,
    typeReference: TypeReference<ResponseType>,
    requestBuilder: BaseHMPPSClient.HMPPSRequestConfiguration,
    cacheConfig: PreemptiveCacheConfig,
  ): ClientResult<ResponseType> {
    val pollingStart = System.currentTimeMillis()

    do {
      val cacheEntryMetadata = getCacheEntryMetadataIfExists(cacheKeyResolver.metadataKey)

      if (cacheEntryMetadata == null) {
        Thread.sleep(POLL_CACHE_WAIT_DURATION_BEFORE_RETRY_MS)
        continue
      }

      return resultFromCacheMetadata(cacheEntryMetadata, cacheKeyResolver, typeReference)
    } while (System.currentTimeMillis() - pollingStart < requestBuilder.preemptiveCacheTimeoutMs)

    return ClientResult.Failure.PreemptiveCacheTimeout(
      cacheConfig.cacheName,
      cacheKeyResolver.metadataKey,
      requestBuilder.preemptiveCacheTimeoutMs,
    )
  }

  private fun getCacheKeySet(requestBuilder: BaseHMPPSClient.HMPPSRequestConfiguration, cacheConfig: PreemptiveCacheConfig): CacheKeyResolver {
    val key = requestBuilder.preemptiveCacheKey ?: throw RuntimeException("Must provide a preemptiveCacheKey")
    return CacheKeyResolver(preemptiveCacheKeyPrefix, cacheConfig.cacheName, key)
  }

  private fun writeToRedis(
    cacheKeyResolver: CacheKeyResolver,
    cacheEntry: PreemptiveCacheMetadata,
    body: String?,
    hardTtlSeconds: Long,
  ) {
    redisTemplate.boundValueOps(cacheKeyResolver.metadataKey).set(
      objectMapper.writeValueAsString(cacheEntry),
      Duration.ofSeconds(hardTtlSeconds),
    )

    if (body != null) {
      redisTemplate.boundValueOps(cacheKeyResolver.dataKey).set(
        body,
        Duration.ofSeconds(hardTtlSeconds),
      )
    }
  }

  private fun getCacheEntryMetadataIfExists(metaDataKey: String): PreemptiveCacheMetadata? {
    val stringValue = redisTemplate.boundValueOps(metaDataKey).get()
      ?: return null

    return objectMapper.readValue<PreemptiveCacheMetadata>(
      stringValue,
    )
  }

  private fun getCacheEntryBody(dataKey: String): String? = redisTemplate.boundValueOps(dataKey).get()

  private fun <ResponseType> resultFromCacheMetadata(
    metadataEntry: PreemptiveCacheMetadata,
    cacheKeyResolver: CacheKeyResolver,
    typeReference: TypeReference<ResponseType>,
  ): ClientResult<ResponseType> {
    val cachedBody = if (metadataEntry.hasResponseBody) {
      val dataKey = cacheKeyResolver.dataKey
      val metadataKey = cacheKeyResolver.metadataKey
      val cachedBody = getCacheEntryBody(cacheKeyResolver.dataKey)

      if (cachedBody == null) {
        sentryService.captureErrorMessage(
          "Could not find data entry in cache for key '$dataKey', despite the metadata saying the body was available. " +
            "Will remove the corrupt metadata entry '$metadataKey'. This is possibly happening due to Redis evicting entries prematurely due to running out of space",
        )
        redisTemplate.delete(metadataKey)
        return ClientResult.Failure.CachedValueUnavailable(
          cacheKey = cacheKeyResolver.dataKey,
        )
      }

      cachedBody
    } else {
      null
    }

    if (metadataEntry.httpStatus.is2xxSuccessful) {
      return ClientResult.Success(
        status = metadataEntry.httpStatus,
        body = objectMapper.readValue(cachedBody!!, typeReference),
        isPreemptivelyCachedResponse = true,
      )
    }

    return ClientResult.Failure.StatusCode(
      status = metadataEntry.httpStatus,
      body = cachedBody,
      method = metadataEntry.method!!.toHttpMethod(),
      path = metadataEntry.path!!,
      isPreemptivelyCachedResponse = true,
    )
  }

  companion object {
    private const val FAILED_ATTEMPT_WARN_THRESHOLD: Int = 4
    private const val POLL_CACHE_WAIT_DURATION_BEFORE_RETRY_MS: Long = 500
  }

  data class PreemptiveCacheConfig(
    val cacheName: String,
    /**
     * The maximum TTL at which the redis entry will always be considered for refresh
     *
     * Also see successSoftTtlJitterSeconds
     */
    val successSoftTtlSeconds: Int,
    /**
     * Jitter applied when checking a records soft TTL
     *
     * successSoftTtlSeconds is the ceiling from which a random number between
     * 0 and this configuration is subtracted.
     *
     * For example, if successSoftTtlJitterSeconds is 10, and successSoftTtlSeconds is 30
     * the calculated soft TTL is between 20 and 30 seconds
     */
    val successSoftTtlJitterSeconds: Long,
    val failureSoftTtlBackoffSeconds: List<Int>,
    /**
     * Used to determine the expiry value of the redis record
     */
    val hardTtlSeconds: Int,

  )

  /**
   * This is written into the cache using the key defined by [CacheKeyResolver.metadataKey]
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class PreemptiveCacheMetadata(
    val httpStatus: HttpStatus,
    val refreshableAfter: Instant,
    val method: MarshallableHttpMethod?,
    val path: String?,
    /**
     * If true, there should be a body cached using the key defined by [CacheKeyResolver.dataKey]
     */
    val hasResponseBody: Boolean,
    val attempt: Int?,
  )
}

/*
Before the Spring Boot 3 Upgrade, we would directly marshal Spring's HttpMethod class into JSON for use in the Cache.
As of Spring Boot 3 this class has been changed such that it can't be unmarshalled back into Java. As to avoid modifying
entries in the cache during the Spring Boot 2 to 3, we have introduced our own version of Spring's HttpMethod that can
be marshalled into JSON and back into Java.
*/
enum class MarshallableHttpMethod {
  GET,
  HEAD,
  POST,
  PUT,
  PATCH,
  DELETE,
  OPTIONS,
  TRACE,
  ;

  companion object {
    fun fromHttpMethod(value: HttpMethod) = MarshallableHttpMethod.valueOf(value.name())
  }

  fun toHttpMethod() = HttpMethod.valueOf(this.name)
}

class CacheKeyResolver(
  private val prefix: String,
  private val cacheName: String,
  private val key: String,
) {

  /**
   * Stores an instance of [WebClientCache.PreemptiveCacheMetadata]
   */
  val metadataKey: String
    get() {
      return "$prefix-$cacheName-$key-metadata"
    }

  /**
   * Stores the actual response payload, if [WebClientCache.PreemptiveCacheMetadata.hasResponseBody] is true
   */
  val dataKey: String
    get() {
      return "$prefix-$cacheName-$key-data"
    }
}
