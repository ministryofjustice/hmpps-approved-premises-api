package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService

class UserServiceTest {
  private val mockHttpAuthService = mockk<HttpAuthService>()
  private val mockUserRepository = mockk<UserRepository>()

  private val userService = UserService(mockHttpAuthService, mockUserRepository)

  @Test
  fun `getUserForRequest returns existing User when exists`() {
    val username = "SOMEPERSON"
    val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

    every { mockHttpAuthService.getDeliusPrincipalOrThrow() } returns mockPrincipal
    every { mockPrincipal.name } returns username

    val user = UserEntityFactory().produce()

    every { mockUserRepository.findByDistinguishedName(username) } returns user

    assertThat(userService.getUserForRequest()).isEqualTo(user)
  }

  @Test
  fun `getUserForRequest returns new User when one does not already exist`() {
    val username = "SOMEPERSON"
    val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

    every { mockHttpAuthService.getDeliusPrincipalOrThrow() } returns mockPrincipal
    every { mockPrincipal.name } returns username

    every { mockUserRepository.findByDistinguishedName(username) } returns null
    every { mockUserRepository.save(any()) } answers { it.invocation.args[0] as UserEntity }

    assertThat(userService.getUserForRequest()).matches {
      it.name == username
    }
  }
}
