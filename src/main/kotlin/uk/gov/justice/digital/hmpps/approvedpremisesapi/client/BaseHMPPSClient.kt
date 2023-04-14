package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
  private val redisTemplate: RedisTemplate<String, String>
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

  protected inline fun <reified ResponseType : Any> request(method: HttpMethod, noinline requestBuilderConfiguration: HMPPSRequestConfiguration.() -> Unit): ClientResult<ResponseType> {
    val clazz = if (ResponseType::class.java.typeParameters.any()) null else ResponseType::class.java
    val typeReference = if (ResponseType::class.java.typeParameters.any()) object : TypeReference<ResponseType>() {} else null

    return doRequest(clazz, typeReference, method, requestBuilderConfiguration)
  }

  fun <ResponseType : Any> doRequest(clazz: Class<ResponseType>?, typeReference: TypeReference<ResponseType>?, method: HttpMethod, requestBuilderConfiguration: HMPPSRequestConfiguration.() -> Unit): ClientResult<ResponseType> {
    val requestBuilder = HMPPSRequestConfiguration()
    requestBuilderConfiguration(requestBuilder)

    val cacheConfig = requestBuilder.preemptiveCacheConfig

    try {
      if (cacheConfig != null) {
        val qualifiedKey = getQualifiedKey(requestBuilder, cacheConfig)

        if (!requestBuilder.isPreemptiveCall) {
          val pollingStart = System.currentTimeMillis()

          do {
            val cacheEntry = getCacheEntryIfExists(qualifiedKey)

            if (cacheEntry == null) {
              Thread.sleep(500)
              continue
            }

            return resultFromCacheEntry(cacheEntry, typeReference, clazz)
          } while (System.currentTimeMillis() - pollingStart < requestBuilder.preemptiveCacheTimeoutMs)

          return ClientResult.Failure.PreemptiveCacheTimeout(cacheConfig.cacheName, qualifiedKey, requestBuilder.preemptiveCacheTimeoutMs)
        }

        val cacheEntry = getCacheEntryIfExists(qualifiedKey)

        if (cacheEntry != null && cacheEntry.refreshableAfter.isAfter(Instant.now())) {
          return resultFromCacheEntry(cacheEntry, typeReference, clazz)
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
        val qualifiedKey = getQualifiedKey(requestBuilder, cacheConfig)

        val cacheEntry = PreemptiveCacheEntry(
          httpStatus = result.statusCode,
          refreshableAfter = Instant.now().plusSeconds(cacheConfig.successSoftTtlSeconds.toLong()),
          body = result.body,
          method = null,
          path = null
        )

        writeToRedis(qualifiedKey, cacheEntry, cacheConfig.hardTtlSeconds.toLong())
      }

      return ClientResult.Success(result.statusCode, deserialized)
    } catch (exception: WebClientResponseException) {
      if (cacheConfig != null && requestBuilder.isPreemptiveCall) {
        val qualifiedKey = getQualifiedKey(requestBuilder, cacheConfig)

        val cacheEntry = PreemptiveCacheEntry(
          httpStatus = exception.statusCode,
          refreshableAfter = Instant.now().plusSeconds(cacheConfig.successSoftTtlSeconds.toLong()),
          body = exception.responseBodyAsString,
          method = method,
          path = requestBuilder.path ?: ""
        )

        writeToRedis(qualifiedKey, cacheEntry, cacheConfig.hardTtlSeconds.toLong())
      }

      return ClientResult.Failure.StatusCode(method, requestBuilder.path ?: "", exception.statusCode, exception.responseBodyAsString)
    } catch (exception: Exception) {
      return ClientResult.Failure.Other(method, requestBuilder.path ?: "", exception)
    }
  }

  private fun getQualifiedKey(requestBuilder: HMPPSRequestConfiguration, cacheConfig: PreemptiveCacheConfig): String {
    val key = requestBuilder.preemptiveCacheKey ?: throw RuntimeException("Must provide a preemptiveCacheKey")
    return "${cacheConfig.cacheName}-$key"
  }

  private fun writeToRedis(qualifiedKey: String, cacheEntry: PreemptiveCacheEntry, hardTtlSeconds: Long) {
    redisTemplate.boundValueOps(qualifiedKey).set(
      objectMapper.writeValueAsString(cacheEntry),
      Duration.ofSeconds(hardTtlSeconds)
    )
  }

  private fun getCacheEntryIfExists(qualifiedKey: String): PreemptiveCacheEntry? {
    if (redisTemplate.hasKey(qualifiedKey)) {
      return objectMapper.readValue<PreemptiveCacheEntry>(
        redisTemplate.boundValueOps(qualifiedKey).get()!!
      )
    }

    return null
  }

  private fun <ResponseType> resultFromCacheEntry(cacheEntry: PreemptiveCacheEntry, typeReference: TypeReference<ResponseType>?, clazz: Class<ResponseType>?): ClientResult<ResponseType> {
    if (cacheEntry.httpStatus.is2xxSuccessful) {
      return ClientResult.Success(
        status = cacheEntry.httpStatus,
        body = if (typeReference != null) {
          objectMapper.readValue(cacheEntry.body, typeReference)
        } else {
          objectMapper.readValue(cacheEntry.body, clazz)
        }
      )
    }

    return ClientResult.Failure.StatusCode(
      status = cacheEntry.httpStatus,
      body = cacheEntry.body,
      method = cacheEntry.method!!,
      path = cacheEntry.path!!
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
    val failureSoftTtlSeconds: Int,
    val hardTtlSeconds: Int
  )

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class PreemptiveCacheEntry(
    val httpStatus: HttpStatus,
    val refreshableAfter: Instant,
    val body: String?,
    val method: HttpMethod?,
    val path: String?
  )
}

sealed interface ClientResult<ResponseType> {
  class Success<ResponseType>(val status: HttpStatus, val body: ResponseType) : ClientResult<ResponseType>
  sealed interface Failure<ResponseType> : ClientResult<ResponseType> {
    fun throwException(): Nothing

    class StatusCode<ResponseType>(val method: HttpMethod, val path: String, val status: HttpStatus, val body: String?) : Failure<ResponseType> {
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
