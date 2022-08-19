package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.web.client.RestTemplate
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.net.URI

abstract class BaseHMPPSClient(private val restTemplate: RestTemplate, private val objectMapper: ObjectMapper, private val baseUrl: String) {
  protected inline fun <reified ResponseType : Any> getRequest(noinline requestBuilderConfiguration: HMPPSRequestConfiguration.() -> Unit): ClientResult<ResponseType?> =
    request(HttpMethod.GET, requestBuilderConfiguration)

  protected inline fun <reified ResponseType : Any> postRequest(noinline requestBuilderConfiguration: HMPPSRequestConfiguration.() -> Unit): ClientResult<ResponseType?> =
    request(HttpMethod.POST, requestBuilderConfiguration)

  protected inline fun <reified ResponseType : Any> putRequest(noinline requestBuilderConfiguration: HMPPSRequestConfiguration.() -> Unit): ClientResult<ResponseType?> =
    request(HttpMethod.PUT, requestBuilderConfiguration)

  protected inline fun <reified ResponseType : Any> deleteRequest(noinline requestBuilderConfiguration: HMPPSRequestConfiguration.() -> Unit): ClientResult<ResponseType?> =
    request(HttpMethod.DELETE, requestBuilderConfiguration)

  protected inline fun <reified ResponseType : Any> patchRequest(noinline requestBuilderConfiguration: HMPPSRequestConfiguration.() -> Unit): ClientResult<ResponseType?> =
    request(HttpMethod.PATCH, requestBuilderConfiguration)

  protected inline fun <reified ResponseType : Any> request(method: HttpMethod, noinline requestBuilderConfiguration: HMPPSRequestConfiguration.() -> Unit): ClientResult<ResponseType?> {
    val clazz = if (ResponseType::class.java.typeParameters.any()) null else ResponseType::class.java
    val typeReference = if (ResponseType::class.java.typeParameters.any()) object : TypeReference<ResponseType>() {} else null

    return doRequest(clazz, typeReference, method, requestBuilderConfiguration)
  }

  fun <ResponseType : Any> doRequest(clazz: Class<ResponseType>?, typeReference: TypeReference<ResponseType>?, method: HttpMethod, requestBuilderConfiguration: HMPPSRequestConfiguration.() -> Unit): ClientResult<ResponseType?> {
    val requestBuilder = HMPPSRequestConfiguration()
    requestBuilderConfiguration(requestBuilder)

    when (requestBuilder.authType) {
      HMPPSAuthType.None -> null
      HMPPSAuthType.PassThroughJwtFromRequest -> getPassThroughJwt()
      HMPPSAuthType.ClientCredentials -> getClientCredentialsJwt()
      HMPPSAuthType.ClientCredentialsWithUsernameFromRequestJwt -> getClientCredentialsWithUsernameJwt()
    }?.let { requestBuilder.withHeader("Authorization", "Bearer $it") }

    val requestEntity = if (requestBuilder.body == null) {
      RequestEntity<Unit>(requestBuilder.headers, method, URI.create("$baseUrl${requestBuilder.path ?: ""}"))
    } else {
      RequestEntity<Any?>(requestBuilder.body, requestBuilder.headers, method, URI.create("$baseUrl/${requestBuilder.path ?: ""}"))
    }

    try {
      val result = restTemplate.exchange(requestEntity, String::class.java)

      if (result.statusCode.is2xxSuccessful) {
        val deserialized = if (result.body.isNullOrEmpty()) {
          null
        } else if (typeReference != null) {
          objectMapper.readValue(result.body, typeReference)
        } else {
          objectMapper.readValue(result.body, clazz)
        }

        return ClientResult.Success(result.statusCode, deserialized)
      }

      return ClientResult.StatusCodeFailure(result.statusCode, result.body)
    } catch (exception: Exception) {
      return ClientResult.OtherFailure(exception)
    }
  }

  private fun getPassThroughJwt() = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request?.getHeader("Authorization")?.replace("Bearer ", "")
    ?: throw RuntimeException("Could not get Authorization header from request")

  private fun getClientCredentialsJwt(): String {
    TODO()
  }

  private fun getClientCredentialsWithUsernameJwt(): String {
    TODO()
  }

  class HMPPSRequestConfiguration {
    internal var path: String? = null
    internal var body: Any? = null
    internal var authType: HMPPSAuthType = HMPPSAuthType.None
    internal var headers = HttpHeaders()

    fun withHeader(key: String, value: String) = headers.add(key, value)
  }

  internal enum class HMPPSAuthType {
    None,
    PassThroughJwtFromRequest,
    ClientCredentials,
    ClientCredentialsWithUsernameFromRequestJwt
  }
}

sealed interface ClientResult<ResponseType> {
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
