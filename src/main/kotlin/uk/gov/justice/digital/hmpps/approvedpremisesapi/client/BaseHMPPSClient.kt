package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.sentry.Sentry
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Duration
import java.time.Instant

abstract class BaseHMPPSClient(
  private val webClient: WebClient,
  private val objectMapper: ObjectMapper,
  private val redisTemplate: RedisTemplate<String, String>,
  private val preemptiveCacheKeyPrefix: String,
) {
  protected inline fun <reified ResponseType : Any> getRequest(noinline requestBuilderConfiguration: HMPPSRequestConfiguration.() -> Unit): ClientResult<ResponseType> =
    request(HttpMethod.GET, requestBuilderConfiguration)

  protected inline fun <reified ResponseType : Any> postRequest(noinline requestBuilderConfiguration: HMPPSRequestConfiguration.() -> Unit): ClientResult<ResponseType> =
    request(HttpMethod.POST, requestBuilderConfiguration)

  protected inline fun <reified ResponseType : Any> putRequest(noinline requestBuilderConfiguration: HMPPSRequestConfiguration.() -> Unit): ClientResult<ResponseType> =
    request(HttpMethod.PUT, requestBuilderConfiguration)

  protected inline fun <reified ResponseType : Any> deleteRequest(noinline requestBuilderConfiguration: HMPPSRequestConfiguration.() -> Unit): ClientResult<ResponseType> =
    request(HttpMethod.DELETE, requestBuilderConfiguration)

  protected inline fun <reified ResponseType : Any> patchRequest(noinline requestBuilderConfiguration: HMPPSRequestConfiguration.() -> Unit): ClientResult<ResponseType> =
    request(HttpMethod.PATCH, requestBuilderConfiguration)

  protected fun checkPreemptiveCacheStatus(cacheConfig: PreemptiveCacheConfig, key: String): PreemptiveCacheEntryStatus {
    val cacheKeySet = CacheKeySet(preemptiveCacheKeyPrefix, cacheConfig.cacheName, key)

    val cacheEntryMetadata = getCacheEntryMetadataIfExists(cacheKeySet.metadataKey)

    return when {
      cacheEntryMetadata == null -> PreemptiveCacheEntryStatus.MISS
      cacheEntryMetadata.refreshableAfter < Instant.now() -> PreemptiveCacheEntryStatus.REQUIRES_REFRESH
      else -> PreemptiveCacheEntryStatus.EXISTS
    }
  }

  protected inline fun <reified ResponseType : Any> request(method: HttpMethod, noinline requestBuilderConfiguration: HMPPSRequestConfiguration.() -> Unit): ClientResult<ResponseType> {
    val clazz = if (ResponseType::class.java.typeParameters.any()) null else ResponseType::class.java
    val typeReference = if (ResponseType::class.java.typeParameters.any()) object : TypeReference<ResponseType>() {} else null

    return doRequest(clazz, typeReference, method, requestBuilderConfiguration)
  }

  fun <ResponseType : Any> doRequest(clazz: Class<ResponseType>?, typeReference: TypeReference<ResponseType>?, method: HttpMethod, requestBuilderConfiguration: HMPPSRequestConfiguration.() -> Unit): ClientResult<ResponseType> {
    val requestBuilder = HMPPSRequestConfiguration()
    requestBuilderConfiguration(requestBuilder)

    val cacheConfig = requestBuilder.preemptiveCacheConfig

    var attempt = 1

    try {
      if (cacheConfig != null) {
        val cacheKeySet = getCacheKeySet(requestBuilder, cacheConfig)

        if (!requestBuilder.isPreemptiveCall) {
          val pollingStart = System.currentTimeMillis()

          do {
            val cacheEntryMetadata = getCacheEntryMetadataIfExists(cacheKeySet.metadataKey)

            if (cacheEntryMetadata == null) {
              Thread.sleep(500)
              continue
            }

            return resultFromCacheMetadata(cacheEntryMetadata, cacheKeySet, typeReference, clazz)
          } while (System.currentTimeMillis() - pollingStart < requestBuilder.preemptiveCacheTimeoutMs)

          return ClientResult.Failure.PreemptiveCacheTimeout(cacheConfig.cacheName, cacheKeySet.metadataKey, requestBuilder.preemptiveCacheTimeoutMs)
        }

        val cacheEntry = getCacheEntryMetadataIfExists(cacheKeySet.metadataKey)

        attempt = cacheEntry?.attempt?.plus(1) ?: 1

        if (cacheEntry != null && cacheEntry.refreshableAfter.isAfter(Instant.now())) {
          return resultFromCacheMetadata(cacheEntry, cacheKeySet, typeReference, clazz)
        }
      }

      val request = webClient.method(method)
        .uri(requestBuilder.path ?: "")
        .headers { it.addAll(requestBuilder.headers) }

      if (requestBuilder.body != null) {
        request.bodyValue(requestBuilder.body!!)
      }

      val result = request.retrieve().toEntity(String::class.java).block()!!

      val deserialized = if (typeReference != null) {
        objectMapper.readValue(result.body, typeReference)
      } else {
        objectMapper.readValue(result.body, clazz)
      }

      if (cacheConfig != null && requestBuilder.isPreemptiveCall) {
        val cacheKeySet = getCacheKeySet(requestBuilder, cacheConfig)

        val cacheEntry = PreemptiveCacheMetadata(
          httpStatus = result.statusCode,
          refreshableAfter = Instant.now().plusSeconds(cacheConfig.successSoftTtlSeconds.toLong()),
          method = null,
          path = null,
          hasResponseBody = result.body != null,
          attempt = null,
        )

        writeToRedis(cacheKeySet, cacheEntry, result.body, cacheConfig.hardTtlSeconds.toLong())
      }

      return ClientResult.Success(result.statusCode, deserialized, false)
    } catch (exception: WebClientResponseException) {
      if (cacheConfig != null && requestBuilder.isPreemptiveCall) {
        val qualifiedKey = getCacheKeySet(requestBuilder, cacheConfig)

        val body = exception.responseBodyAsString

        val backoffSeconds = if (attempt <= cacheConfig.failureSoftTtlBackoffSeconds.size) {
          cacheConfig.failureSoftTtlBackoffSeconds[attempt - 1].toLong()
        } else {
          cacheConfig.failureSoftTtlBackoffSeconds.last().toLong()
        }

        val cacheEntry = PreemptiveCacheMetadata(
          httpStatus = exception.statusCode,
          refreshableAfter = Instant.now().plusSeconds(backoffSeconds),
          method = method,
          path = requestBuilder.path ?: "",
          hasResponseBody = body != null,
          attempt = attempt,
        )

        if (attempt >= 4) {
          Sentry.captureException(RuntimeException("Unable to make upstream request after $attempt attempts", exception))
        }

        writeToRedis(qualifiedKey, cacheEntry, body, cacheConfig.hardTtlSeconds.toLong())
      }

      return ClientResult.Failure.StatusCode(method, requestBuilder.path ?: "", exception.statusCode, exception.responseBodyAsString, false)
    } catch (exception: Exception) {
      return ClientResult.Failure.Other(method, requestBuilder.path ?: "", exception)
    }
  }

  private fun getCacheKeySet(requestBuilder: HMPPSRequestConfiguration, cacheConfig: PreemptiveCacheConfig): CacheKeySet {
    val key = requestBuilder.preemptiveCacheKey ?: throw RuntimeException("Must provide a preemptiveCacheKey")
    return CacheKeySet(preemptiveCacheKeyPrefix, cacheConfig.cacheName, key)
  }

  private fun writeToRedis(cacheKeySet: CacheKeySet, cacheEntry: PreemptiveCacheMetadata, body: String?, hardTtlSeconds: Long) {
    redisTemplate.boundValueOps(cacheKeySet.metadataKey).set(
      objectMapper.writeValueAsString(cacheEntry),
      Duration.ofSeconds(hardTtlSeconds),
    )

    if (body != null) {
      redisTemplate.boundValueOps(cacheKeySet.dataKey).set(
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

  private fun getCacheEntryBody(dataKey: String): String {
    return redisTemplate.boundValueOps(dataKey).get()
      ?: throw RuntimeException("No Redis entry exists for $dataKey")
  }

  private fun <ResponseType> resultFromCacheMetadata(cacheEntry: PreemptiveCacheMetadata, cacheKeySet: CacheKeySet, typeReference: TypeReference<ResponseType>?, clazz: Class<ResponseType>?): ClientResult<ResponseType> {
    val cachedBody = if (cacheEntry.hasResponseBody) {
      getCacheEntryBody(cacheKeySet.dataKey)
    } else {
      null
    }

    if (cacheEntry.httpStatus.is2xxSuccessful) {
      return ClientResult.Success(
        status = cacheEntry.httpStatus,
        body = if (typeReference != null) {
          objectMapper.readValue(cachedBody, typeReference)
        } else {
          objectMapper.readValue(cachedBody, clazz)
        },
        isPreemptivelyCachedResponse = true,
      )
    }

    return ClientResult.Failure.StatusCode(
      status = cacheEntry.httpStatus,
      body = cachedBody,
      method = cacheEntry.method!!,
      path = cacheEntry.path!!,
      isPreemptivelyCachedResponse = true,
    )
  }

  class HMPPSRequestConfiguration {
    internal var path: String? = null
    internal var body: Any? = null
    internal var headers = HttpHeaders()
    internal var preemptiveCacheConfig: PreemptiveCacheConfig? = null
    internal var isPreemptiveCall = false
    internal var preemptiveCacheKey: String? = null
    internal var preemptiveCacheTimeoutMs: Int = 10000

    fun withHeader(key: String, value: String) = headers.add(key, value)
  }

  data class PreemptiveCacheConfig(
    val cacheName: String,
    val successSoftTtlSeconds: Int,
    val failureSoftTtlBackoffSeconds: List<Int>,
    val hardTtlSeconds: Int,
  )

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class PreemptiveCacheMetadata(
    val httpStatus: HttpStatus,
    val refreshableAfter: Instant,
    val method: HttpMethod?,
    val path: String?,
    val hasResponseBody: Boolean,
    val attempt: Int?,
  )
}

sealed interface ClientResult<ResponseType> {
  class Success<ResponseType>(val status: HttpStatus, val body: ResponseType, val isPreemptivelyCachedResponse: Boolean = false) : ClientResult<ResponseType>
  sealed interface Failure<ResponseType> : ClientResult<ResponseType> {
    fun throwException(): Nothing

    class StatusCode<ResponseType>(val method: HttpMethod, val path: String, val status: HttpStatus, val body: String?, val isPreemptivelyCachedResponse: Boolean = false) : Failure<ResponseType> {
      override fun throwException(): Nothing {
        throw RuntimeException("Unable to complete $method request to $path: $status")
      }

      inline fun <reified ResponseType> deserializeTo(): ResponseType = jacksonObjectMapper().readValue(body, ResponseType::class.java)
    }

    class PreemptiveCacheTimeout<ResponseType>(val cacheName: String, val cacheKey: String, val timeoutMs: Int) : Failure<ResponseType> {
      override fun throwException(): Nothing {
        throw RuntimeException("Timed out after ${timeoutMs}ms waiting for $cacheKey on pre-emptive cache $cacheName")
      }
    }

    class Other<ResponseType>(val method: HttpMethod, val path: String, val exception: Exception) : Failure<ResponseType> {
      override fun throwException(): Nothing {
        throw RuntimeException("Unable to complete $method request to $path", exception)
      }
    }
  }
}

class CacheKeySet(
  private val prefix: String,
  private val cacheName: String,
  private val key: String,
) {
  val metadataKey: String
    get() { return "$prefix-$cacheName-$key-metadata" }

  val dataKey: String
    get() { return "$prefix-$cacheName-$key-data" }
}

enum class PreemptiveCacheEntryStatus {
  MISS, REQUIRES_REFRESH, EXISTS
}
