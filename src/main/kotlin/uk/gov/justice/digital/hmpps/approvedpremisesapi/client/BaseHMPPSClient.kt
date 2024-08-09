package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.atomic.AtomicInteger

abstract class BaseHMPPSClient(
  private val webClient: WebClient,
  private val objectMapper: ObjectMapper,
  private val webClientCache: WebClientCache,
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

  protected fun checkPreemptiveCacheStatus(cacheConfig: WebClientCache.PreemptiveCacheConfig, key: String): PreemptiveCacheEntryStatus =
    webClientCache.checkPreemptiveCacheStatus(cacheConfig, key)

  protected inline fun <reified ResponseType : Any> request(method: HttpMethod, noinline requestBuilderConfiguration: HMPPSRequestConfiguration.() -> Unit): ClientResult<ResponseType> {
    val typeReference = object : TypeReference<ResponseType>() {}

    return doRequest(typeReference, method, requestBuilderConfiguration)
  }

  fun <ResponseType : Any> doRequest(typeReference: TypeReference<ResponseType>, method: HttpMethod, requestBuilderConfiguration: HMPPSRequestConfiguration.() -> Unit): ClientResult<ResponseType> {
    val requestBuilder = HMPPSRequestConfiguration()
    requestBuilderConfiguration(requestBuilder)

    val cacheConfig = requestBuilder.preemptiveCacheConfig

    val attempt = AtomicInteger(1)

    try {
      if (cacheConfig != null) {
        webClientCache.tryGetCachedValue(typeReference, requestBuilder, cacheConfig, attempt)?.let {
          return it
        }
      }

      val (statusCode, body) = performRequest(typeReference, method, requestBuilder)

      if (cacheConfig != null && requestBuilder.isPreemptiveCall) {
        webClientCache.cacheSuccessfulWebClientResponse(requestBuilder, cacheConfig, statusCode, body)
      }

      return ClientResult.Success(statusCode, body, false)
    } catch (exception: WebClientResponseException) {
      if (cacheConfig != null && requestBuilder.isPreemptiveCall) {
        webClientCache.cacheFailedWebClientResponse(requestBuilder, cacheConfig, exception, attempt.get(), method)
      }

      if (!exception.statusCode.is2xxSuccessful) {
        return ClientResult.Failure.StatusCode(method, requestBuilder.path ?: "", exception.statusCode, exception.responseBodyAsString, false)
      } else {
        throw exception
      }
    } catch (exception: Exception) {
      return ClientResult.Failure.Other(method, requestBuilder.path ?: "", exception)
    }
  }

  private fun <ResponseType : Any> performRequest(typeReference: TypeReference<ResponseType>, method: HttpMethod, requestBuilder: HMPPSRequestConfiguration): Pair<HttpStatus, ResponseType> {
    val request = webClient.method(method)
      .uri(requestBuilder.path ?: "")
      .headers { it.addAll(requestBuilder.headers) }

    if (requestBuilder.body != null) {
      request.bodyValue(requestBuilder.body!!)
    }

    val result = request
      .retrieve()
      .toEntityFlux(DataBuffer::class.java)
      .block()!!

    val pipedOutputStream = PipedOutputStream()
    val pipedInputStream = PipedInputStream(pipedOutputStream)

    val body = result
      .body
      ?.doOnError { pipedInputStream.close() }
      ?.doFinally { pipedOutputStream.close() }

    DataBufferUtils
      .write(body, pipedOutputStream)
      .subscribe(DataBufferUtils.releaseConsumer())

    val deserialized = objectMapper.readValue(pipedInputStream, typeReference)

    return result.statusCode to deserialized
  }

  class HMPPSRequestConfiguration {
    internal var path: String? = null
    internal var body: Any? = null
    internal var headers = HttpHeaders()
    internal var preemptiveCacheConfig: WebClientCache.PreemptiveCacheConfig? = null
    internal var isPreemptiveCall = false
    internal var preemptiveCacheKey: String? = null
    internal var preemptiveCacheTimeoutMs: Int = 10000

    fun withHeader(key: String, value: String) = headers.add(key, value)
  }
}

sealed interface ClientResult<ResponseType> {
  fun <TargetType> map(transform: (ResponseType) -> TargetType): ClientResult<TargetType>

  data class Success<ResponseType>(
    val status: HttpStatus,
    val body: ResponseType,
    val isPreemptivelyCachedResponse: Boolean = false,
  ) : ClientResult<ResponseType> {
    fun <TargetType> copyWithBody(body: TargetType) = Success(this.status, body, this.isPreemptivelyCachedResponse)

    override fun <TargetType> map(transform: (ResponseType) -> TargetType): ClientResult<TargetType> =
      Success(this.status, transform(body), this.isPreemptivelyCachedResponse)
  }

  sealed interface Failure<ResponseType> : ClientResult<ResponseType> {
    fun throwException(): Nothing = throw toException()
    fun toException(): Throwable

    data class StatusCode<ResponseType>(
      val method: HttpMethod,
      val path: String,
      val status: HttpStatus,
      val body: String?,
      val isPreemptivelyCachedResponse: Boolean = false,
    ) : Failure<ResponseType> {

      @Suppress("UNCHECKED_CAST") // Safe as this variant contains nothing of type `ResponseType`.
      override fun <TargetType> map(transform: (ResponseType) -> TargetType): ClientResult<TargetType> =
        this as StatusCode<TargetType>

      override fun toException(): Throwable = RuntimeException("Unable to complete $method request to $path: $status")

      inline fun <reified ResponseType> deserializeTo(): ResponseType = jacksonObjectMapper()
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
        .readValue(body, ResponseType::class.java)
    }

    data class PreemptiveCacheTimeout<ResponseType>(
      val cacheName: String,
      val cacheKey: String,
      val timeoutMs: Int,
    ) : Failure<ResponseType> {

      @Suppress("UNCHECKED_CAST") // Safe as this variant contains nothing of type `ResponseType`.
      override fun <TargetType> map(transform: (ResponseType) -> TargetType): ClientResult<TargetType> =
        this as PreemptiveCacheTimeout<TargetType>

      override fun toException(): Throwable = RuntimeException("Timed out after ${timeoutMs}ms waiting for $cacheKey on pre-emptive cache $cacheName")
    }

    data class CachedValueUnavailable<ResponseType>(val cacheKey: String) : Failure<ResponseType> {

      @Suppress("UNCHECKED_CAST") // Safe as this variant contains nothing of type `ResponseType`.
      override fun <TargetType> map(transform: (ResponseType) -> TargetType): ClientResult<TargetType> =
        this as CachedValueUnavailable<TargetType>

      override fun toException(): Throwable = RuntimeException("No Redis entry exists for $cacheKey")
    }

    data class Other<ResponseType>(
      val method: HttpMethod,
      val path: String,
      val exception: Exception,
    ) : Failure<ResponseType> {

      @Suppress("UNCHECKED_CAST") // Safe as this variant contains nothing of type `ResponseType`.
      override fun <TargetType> map(transform: (ResponseType) -> TargetType): ClientResult<TargetType> =
        this as Other<TargetType>

      override fun toException(): Throwable = RuntimeException("Unable to complete $method request to $path", exception)
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
  /**
   * No corresponding entry in the cache
   */
  MISS,

  /**
   * An entry exists in the cache, but it needs a refresh
   */
  REQUIRES_REFRESH,

  /**
   * An entry exists in the cache and it doesn't need a refresh
   */
  EXISTS,
}
