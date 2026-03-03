package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import io.jsonwebtoken.Jwts
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.security.interfaces.RSAPublicKey
import java.time.Duration
import java.util.UUID

@Component
class JwtAuthHelper {
  private val keyPair = Jwts.SIG.RS256.keyPair().build()

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

  @Bean
  @Primary
  fun jwtDecoder(): JwtDecoder = NimbusJwtDecoder.withPublicKey(keyPair.public as RSAPublicKey).build()

  internal fun createValidClientCredentialsJwt(role: String) = createClientCredentialsJwt(
    expiryTime = Duration.ofMinutes(2),
    roles = listOf(role),
  )

  internal fun createExpiredClientCredentialsJwt() = createClientCredentialsJwt(
    expiryTime = Duration.ofMinutes(-2),
    roles = listOf("ROLE_COMMUNITY"),
  )

  internal fun createClientCredentialsJwt(
    username: String? = null,
    scope: List<String>? = listOf(),
    roles: List<String>? = listOf(),
    authSource: String = if (username == null) "none" else "delius",
    expiryTime: Duration = Duration.ofHours(1),
    jwtId: String = UUID.randomUUID().toString(),
  ): String = jwtAuthHelper.createJwtAccessToken(
    grantType = "client_credentials",
    clientId = "integration-test-client-id",
    username = username,
    scope = scope,
    roles = roles,
    authSource = authSource,
    expiryTime = expiryTime,
    jwtId = jwtId,
  )

  internal fun createValidNomisAuthorisationCodeJwt(username: String = "username", roles: List<String>? = listOf("ROLE_POM")) = createAuthorizationCodeJwt(
    subject = username,
    authSource = "nomis",
    roles = roles,
  )

  internal fun createValidCas2v2NomisAuthorisationCodeJwt(username: String = "username", roles: List<String>? = listOf("ROLE_CAS2_PRISON_BAIL_REFERRER")) = createAuthorizationCodeJwt(
    subject = username,
    authSource = "nomis",
    roles = roles,
  )

  internal fun createValidDeliusAuthorisationCodeJwt(username: String = "username", roles: List<String>? = listOf("ROLE_CAS2_COURT_BAIL_REFERRER")) = createAuthorizationCodeJwt(
    subject = username,
    authSource = "delius",
    roles = roles,
  )

  internal fun createValidExternalAuthorisationCodeJwt(username: String = "username") = createAuthorizationCodeJwt(
    subject = username,
    authSource = "auth",
    roles = listOf("ROLE_CAS2_ASSESSOR"),
  )

  internal fun createValidAdminAuthorisationCodeJwt(username: String = "username") = createAuthorizationCodeJwt(
    subject = username,
    authSource = "nomis",
    roles = listOf("ROLE_CAS2_ADMIN"),
  )

  internal fun createValidAuthorizationCodeJwt(username: String = "username") = createAuthorizationCodeJwt(
    subject = username,
    authSource = "delius",
    roles = listOf("ROLE_PROBATION"),
  )

  internal fun createAuthorizationCodeJwt(
    subject: String,
    scope: List<String>? = listOf(),
    roles: List<String>? = listOf(),
    authSource: String = "delius",
    expiryTime: Duration = Duration.ofHours(1),
    jwtId: String = UUID.randomUUID().toString(),
  ): String =
    jwtAuthHelper.createJwtAccessToken(
      grantType = "authorization_code",
      clientId = subject,
      scope = scope,
      roles = roles,
      authSource = authSource,
      expiryTime = expiryTime,
      jwtId = jwtId,
    )
}
