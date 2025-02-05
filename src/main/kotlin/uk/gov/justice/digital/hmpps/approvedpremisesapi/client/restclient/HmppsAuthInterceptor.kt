package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.restclient

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.core.OAuth2AuthenticationException

class HmppsAuthInterceptor(
  private val clientManager: OAuth2AuthorizedClientManager,
  private val registrationId: String,
) : ClientHttpRequestInterceptor {
  override fun intercept(
    request: HttpRequest,
    body: ByteArray,
    execution: ClientHttpRequestExecution,
  ): ClientHttpResponse {
    request.headers[HttpHeaders.AUTHORIZATION] = "Bearer ${getToken()}"
    return execution.execute(request, body)
  }

  private fun getToken(): String {
    val authentication = SecurityContextHolder.getContext().authentication ?: AnonymousAuthenticationToken(
      "hmpps-auth",
      "anonymous",
      AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"),
    )
    val request = OAuth2AuthorizeRequest
      .withClientRegistrationId(registrationId)
      .principal(authentication)
      .build()
    return clientManager.authorize(request)?.accessToken?.tokenValue
      ?: throw OAuth2AuthenticationException("Unable to retrieve access token")
  }
}
