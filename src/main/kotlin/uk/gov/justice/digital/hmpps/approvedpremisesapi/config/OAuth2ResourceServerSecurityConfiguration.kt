package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.config.web.servlet.invoke
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import java.util.Base64

@EnableWebSecurity
class OAuth2ResourceServerSecurityConfiguration {
  @Bean
  @Throws(Exception::class)
  fun securityFilterChain(http: HttpSecurity, @Autowired objectMapper: ObjectMapper): SecurityFilterChain {
    http {
      csrf { disable() }

      authorizeHttpRequests {
        authorize(HttpMethod.GET, "/health/**", permitAll)
        authorize(HttpMethod.GET, "/swagger-ui/**", permitAll)
        authorize(HttpMethod.GET, "/v3/api-docs/swagger-config", permitAll)
        authorize(HttpMethod.GET, "/api.yml", permitAll)
        authorize(HttpMethod.GET, "/domain-events-api.yml", permitAll)
        authorize(HttpMethod.GET, "/favicon.ico", permitAll)
        authorize(HttpMethod.GET, "/info", permitAll)
        authorize(HttpMethod.POST, "/seed", permitAll)
        authorize(HttpMethod.POST, "/migration-job", permitAll)
        authorize(HttpMethod.GET, "/events/**", hasAuthority("ROLE_APPROVED_PREMISES_EVENTS"))
        authorize(anyRequest, hasAuthority("ROLE_PROBATION"))
      }

      anonymous { disable() }

      oauth2ResourceServer {
        jwt { jwtAuthenticationConverter = AuthAwareTokenConverter() }

        authenticationEntryPoint = AuthenticationEntryPoint { _, response, _ ->
          response.apply {
            status = 401
            contentType = "application/problem+json"
            characterEncoding = "UTF-8"

            writer.write(
              objectMapper.writeValueAsString(
                object {
                  val title = "Unauthenticated"
                  val status = 401
                  val detail =
                    "A valid HMPPS Auth JWT must be supplied via bearer authentication to access this endpoint"
                }
              )
            )
          }
        }
      }

      sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
    }

    return http.build()
  }
}

class AuthAwareTokenConverter() : Converter<Jwt, AbstractAuthenticationToken> {
  private val jwtGrantedAuthoritiesConverter: Converter<Jwt, Collection<GrantedAuthority>> =
    JwtGrantedAuthoritiesConverter()

  override fun convert(jwt: Jwt): AbstractAuthenticationToken {
    val claims = jwt.claims
    val principal = findPrincipal(claims)
    val authorities = extractAuthorities(jwt)
    return AuthAwareAuthenticationToken(jwt, principal, authorities)
  }

  private fun findPrincipal(claims: Map<String, Any?>): String {
    return if (claims.containsKey(CLAIM_USERNAME)) {
      claims[CLAIM_USERNAME] as String
    } else if (claims.containsKey(CLAIM_USER_ID)) {
      claims[CLAIM_USER_ID] as String
    } else if (claims.containsKey(CLAIM_CLIENT_ID)) {
      claims[CLAIM_CLIENT_ID] as String
    } else {
      throw RuntimeException("Unable to find a claim to identify Subject by")
    }
  }

  private fun extractAuthorities(jwt: Jwt): Collection<GrantedAuthority> {
    val authorities = mutableListOf<GrantedAuthority>().apply { addAll(jwtGrantedAuthoritiesConverter.convert(jwt)!!) }
    if (jwt.claims.containsKey(CLAIM_AUTHORITY)) {
      @Suppress("UNCHECKED_CAST")
      val claimAuthorities = (jwt.claims[CLAIM_AUTHORITY] as Collection<String>).toList()
      authorities.addAll(claimAuthorities.map(::SimpleGrantedAuthority))
    }
    return authorities.toSet()
  }

  companion object {
    const val CLAIM_USERNAME = "user_name"
    const val CLAIM_USER_ID = "user_id"
    const val CLAIM_CLIENT_ID = "client_id"
    const val CLAIM_AUTHORITY = "authorities"
  }
}

class AuthAwareAuthenticationToken(
  jwt: Jwt,
  private val aPrincipal: String,
  authorities: Collection<GrantedAuthority>
) : JwtAuthenticationToken(jwt, authorities) {
  override fun getPrincipal(): String {
    return aPrincipal
  }
}

@Configuration
class AuthorizedClientServiceConfiguration(
  @Value("\${log-client-credentials-jwt-info}") private val logClintCredentialsJwtInfo: Boolean,
  private val clientRegistrationRepository: ClientRegistrationRepository,
  private val objectMapper: ObjectMapper
) {
  @Bean
  fun inMemoryOAuth2AuthorizedClientService(): OAuth2AuthorizedClientService {
    if (logClintCredentialsJwtInfo) return LoggingInMemoryOAuth2AuthorizedClientService(clientRegistrationRepository, objectMapper)

    return InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository)
  }
}

class LoggingInMemoryOAuth2AuthorizedClientService(clientRegistrationRepository: ClientRegistrationRepository, private val objectMapper: ObjectMapper) : OAuth2AuthorizedClientService {
  private val backingImplementation = InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository)
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun <T : OAuth2AuthorizedClient?> loadAuthorizedClient(
    clientRegistrationId: String?,
    principalName: String?
  ): T = backingImplementation.loadAuthorizedClient<T>(clientRegistrationId, principalName)

  override fun saveAuthorizedClient(authorizedClient: OAuth2AuthorizedClient?, principal: Authentication?) {
    val tokenValue = authorizedClient?.accessToken?.tokenValue

    if (tokenValue != null) {
      try {
        val tokenBodyBase64 = tokenValue.split(".")[1]
        val tokenBodyRaw = Base64.getDecoder().decode(tokenBodyBase64)
        val info = objectMapper.readValue(tokenBodyRaw, JwtLogInfo::class.java)
        log.info("Retrieved a client_credentials JWT for service->service calls for client ${authorizedClient.clientRegistration.clientId} with authorities: ${info.authorities}, scopes: ${info.scope}, expiry: ${info.exp}")
      } catch (exception: Exception) {
        // Deliberately not logging exception message
        log.error("Unable to get token info to log, exception of type: ${exception::class.java.name}")
      }
    }

    backingImplementation.saveAuthorizedClient(authorizedClient, principal)
  }

  override fun removeAuthorizedClient(clientRegistrationId: String?, principalName: String?) = backingImplementation.removeAuthorizedClient(clientRegistrationId, principalName)
}

data class JwtLogInfo(
  val authorities: List<String>,
  val scope: List<String>,
  val exp: Long
)
