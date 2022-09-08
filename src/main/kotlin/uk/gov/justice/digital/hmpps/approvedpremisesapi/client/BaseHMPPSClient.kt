package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

abstract class BaseHMPPSClient(
  private val webClient: WebClient,
  private val objectMapper: ObjectMapper
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

    try {
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

      return ClientResult.Success(result.statusCode, deserialized)
    } catch (exception: WebClientResponseException) {
      return ClientResult.StatusCodeFailure(exception.statusCode, exception.responseBodyAsString)
    } catch (exception: Exception) {
      return ClientResult.OtherFailure(exception)
    }
  }

  class HMPPSRequestConfiguration {
    internal var path: String? = null
    internal var body: Any? = null
    internal var headers = HttpHeaders()

    fun withHeader(key: String, value: String) = headers.add(key, value)
  }
}

interface ClientResult<ResponseType> {
  class Success<ResponseType>(val status: HttpStatus, val body: ResponseType) : ClientResult<ResponseType>
  interface Failure<ResponseType> : ClientResult<ResponseType> {
    fun throwException(): Nothing
  }
  class StatusCodeFailure<ResponseType>(val status: HttpStatus, val body: String?) : Failure<ResponseType> {
    override fun throwException(): Nothing {
      throw RuntimeException("Unable to complete request: $status")
    }
  }
  class OtherFailure<ResponseType>(val exception: Exception) : Failure<ResponseType> {
    override fun throwException(): Nothing {
      throw RuntimeException("Unable to complete request", exception)
    }
  }
}

// Mocking sealed interfaces is currently broken in mockk, so the else branch is need until this is resolved: https://github.com/mockk/mockk/issues/832
fun shouldNotBeReached(): Nothing = throw RuntimeException("This branch should not be reached as only ClientResult.Success & ClientResult.Failure are returned from clients")
