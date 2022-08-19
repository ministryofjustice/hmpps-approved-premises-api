package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.hmppsauth.GetTokenResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import java.net.URI

@Component
class HMPPSAuthClient(
  private val restTemplate: RestTemplate,
  @Value("\${hmpps.auth.url}") private val hmppsAuthBaseUrl: String,
  @Value("\${hmpps.auth.client-id}") private val clientId: String,
  @Value("\${hmpps.auth.client-secret}") private val clientSecret: String
) {
  fun getClientCredentialsJwt(): String {
    val headers = HttpHeaders()
    headers.setBasicAuth(clientId, clientSecret)

    val requestEntity = RequestEntity<GetTokenResponse>(headers, HttpMethod.POST, URI.create("$hmppsAuthBaseUrl/oauth/token?grant_type=client_credentials"))

    val result = restTemplate.exchange<GetTokenResponse>(requestEntity)

    if (result.statusCode == HttpStatus.OK) {
      return result.body!!.accessToken
    }

    throw InternalServerErrorProblem("Unable to authenticate to make upstream request")
  }

  fun getClientCredentialsWithUsernameJwt(): String {
    val headers = HttpHeaders()
    headers.setBasicAuth(clientId, clientSecret)

    val principal = SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken

    if (principal.token.claims["auth_source"] != "delius") {
      throw RuntimeException("Cannot fetch client_credentials JWT with username where request JWT is not for a Delius login")
    }

    val requestEntity = RequestEntity<GetTokenResponse>(headers, HttpMethod.POST, URI.create("$hmppsAuthBaseUrl/oauth/token?grant_type=client_credentials&username=${principal.name}"))

    val result = restTemplate.exchange<GetTokenResponse>(requestEntity)

    if (result.statusCode == HttpStatus.OK) {
      return result.body!!.accessToken
    }

    throw InternalServerErrorProblem("Unable to authenticate to make upstream request")
  }
}
