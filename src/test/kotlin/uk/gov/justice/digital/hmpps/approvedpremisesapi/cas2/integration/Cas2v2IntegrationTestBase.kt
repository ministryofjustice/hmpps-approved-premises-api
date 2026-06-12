package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration

import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.Cas2v2Constants.Cas2v2Role
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.hmpps.kotlin.auth.AuthSource

class Cas2v2IntegrationTestBase : IntegrationTestBase() {

  fun <S : WebTestClient.RequestHeadersSpec<S>> WebTestClient.RequestHeadersSpec<S>.addJwtForRoleAndAuthSource(
    role: RoleAndAuthSource,
  ): S {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = role.authSource.source,
      roles = listOf(role.name),
    )

    return this.header("Authorization", "Bearer $jwt")
  }

  companion object {

    @JvmStatic
    fun cas2v2NonReferrerRoles(): List<Arguments> = listOf(Cas2v2Role.ADMIN, Cas2v2Role.ASSESSOR, Cas2v2Role.MI)
      .flatMap {
        listOf(
          Arguments.of(RoleAndAuthSource(it, AuthSource.DELIUS)),
          Arguments.of(RoleAndAuthSource(it, AuthSource.NOMIS)),
        )
      }

    @JvmStatic
    fun cas2v2ReferrerRoles(): List<Arguments> = listOf(Cas2v2Role.COURT_BAIL_REFERRER, Cas2v2Role.PRISON_BAIL_REFERRER)
      .flatMap {
        listOf(
          Arguments.of(RoleAndAuthSource(it, AuthSource.DELIUS)),
          Arguments.of(RoleAndAuthSource(it, AuthSource.NOMIS)),
        )
      }
  }
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.integration.Cas2v2IntegrationTestBase#cas2v2NonReferrerRoles")
annotation class Cas2v2NonReferrerRoles

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.integration.Cas2v2IntegrationTestBase#cas2v2ReferrerRoles")
annotation class Cas2v2ReferrerRoles

data class RoleAndAuthSource(
  val name: String,
  val authSource: AuthSource,
)
