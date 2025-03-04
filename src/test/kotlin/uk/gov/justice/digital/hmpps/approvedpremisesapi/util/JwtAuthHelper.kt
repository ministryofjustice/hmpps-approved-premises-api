package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component
import java.security.interfaces.RSAPublicKey
import java.time.Duration
import java.util.Date
import java.util.UUID

@Component
class JwtAuthHelper {
  private val keyPair = Jwts.SIG.RS256.keyPair().build()

  @Bean
  @Primary
  fun jwtDecoder(): JwtDecoder = NimbusJwtDecoder.withPublicKey(keyPair.public as RSAPublicKey).build()

  internal fun createValidClientCredentialsJwt() = createClientCredentialsJwt(
    expiryTime = Duration.ofMinutes(2),
    roles = listOf("ROLE_COMMUNITY"),
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
  ): String = mutableMapOf<String, Any>()
    .also { it["user_name"] = username ?: "integration-test-client-id" }
    .also { it["client_id"] = "integration-test-client-id" }
    .also { it["grant_type"] = "client_credentials" }
    .also { it["auth_source"] = authSource }
    .also { roles?.let { roles -> it["authorities"] = roles } }
    .also { scope?.let { scope -> it["scope"] = scope } }
    .let {
      Jwts.builder()
        .id(jwtId)
        .subject(username ?: "integration-test-client-id")
        .claims(it.toMap())
        .expiration(Date(System.currentTimeMillis() + expiryTime.toMillis()))
        .signWith(keyPair.private, Jwts.SIG.RS256)
        .compact()
    }

  internal fun createValidNomisAuthorisationCodeJwt(username: String = "username", roles: List<String>? = listOf("ROLE_POM")) = createAuthorizationCodeJwt(
    subject = username,
    authSource = "nomis",
    roles = roles,
  )

  internal fun createValidDeliusAuthorisationCodeJwt(username: String = "username", roles: List<String>? = listOf("ROLE_POM")) = createAuthorizationCodeJwt(
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
  ): String = mutableMapOf<String, Any>()
    .also { it["auth_source"] = authSource }
    .also { it["user_id"] = UUID.randomUUID().toString() }
    .also { roles?.let { roles -> it["authorities"] = roles } }
    .also { scope?.let { scope -> it["scope"] = scope } }
    .let {
      Jwts.builder()
        .setId(jwtId)
        .setSubject(subject)
        .addClaims(it.toMap())
        .setExpiration(Date(System.currentTimeMillis() + expiryTime.toMillis()))
        .signWith(SignatureAlgorithm.RS256, keyPair.private)
        .compact()
    }
}
