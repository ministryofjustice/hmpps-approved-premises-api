package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ExternalUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ExternalUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ExternalUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService

class ExternalUserServiceTest {
  private val mockHttpAuthService = mockk<HttpAuthService>()
  private val mockUserRepository = mockk<ExternalUserRepository>()
  private val mockManageUsersApiClient = mockk<ManageUsersApiClient>()

  private val userService = ExternalUserService(
    mockHttpAuthService,
    mockUserRepository,
    mockManageUsersApiClient,
  )

  @Nested
  inner class GetUserForRequest {
    @Test
    fun `returns existing User when exists, does not call Manage-Users API or save`() {
      val username = "JIM_JIMMERSON"
      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
      val mockToken = mockk<Jwt>()

      every { mockPrincipal.token } returns mockToken
      every { mockToken.tokenValue } returns "JWT"

      every { mockHttpAuthService.getCas2AuthenticatedPrincipalOrThrow() } returns mockPrincipal
      every { mockPrincipal.name } returns username

      val user = ExternalUserEntityFactory().produce()

      every { mockUserRepository.findByUsername(username) } returns user

      assertThat(userService.getUserForRequest()).isEqualTo(user)

      verify(exactly = 0) { mockManageUsersApiClient.getExternalUserDetails(username, "JWT") }
      verify(exactly = 0) { mockUserRepository.save(any()) }
    }

    @Test
    fun `returns new User when one does not already exist, does call Manage-Users API and save`() {
      val username = "JIM_JIMMERSON"
      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
      val mockToken = mockk<Jwt>()

      every { mockPrincipal.token } returns mockToken
      every { mockToken.tokenValue } returns "JWT"

      every { mockHttpAuthService.getCas2AuthenticatedPrincipalOrThrow() } returns mockPrincipal
      every { mockPrincipal.name } returns username

      every { mockUserRepository.findByUsername(username) } returns null
      every { mockUserRepository.save(any()) } answers { it.invocation.args[0] as ExternalUserEntity }

      every { mockManageUsersApiClient.getExternalUserDetails(username, "JWT") } returns ClientResult.Success(
        HttpStatus.OK,
        ExternalUserDetailsFactory()
          .withUsername(username)
          .withFirstName("Jim")
          .withLastName("Jimmerson")
          .withEmail("jim@external.example.com")
          .produce(),
      )

      assertThat(userService.getUserForRequest()).matches {
        it.name == "Jim Jimmerson"
        it.email == "jim@external.example.com"
        it.username == "JIM_JIMMERSON"
        it.origin == "NACRO"
      }

      verify(exactly = 1) { mockManageUsersApiClient.getExternalUserDetails(username, "JWT") }
      verify(exactly = 1) { mockUserRepository.save(any()) }
    }
  }
}
