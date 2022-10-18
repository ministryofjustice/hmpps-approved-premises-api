package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService

class UserServiceTest {
  private val mockHttpAuthService = mockk<HttpAuthService>()
  private val mockCommunityApiClient = mockk<CommunityApiClient>()
  private val mockUserRepository = mockk<UserRepository>()

  private val userService = UserService(mockHttpAuthService, mockCommunityApiClient, mockUserRepository)

  @Test
  fun `getUserForRequest returns existing User when exists, does not call Community API or save`() {
    val username = "SOMEPERSON"
    val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

    every { mockHttpAuthService.getDeliusPrincipalOrThrow() } returns mockPrincipal
    every { mockPrincipal.name } returns username

    val user = UserEntityFactory().produce()

    every { mockUserRepository.findByDeliusUsername(username) } returns user

    assertThat(userService.getUserForRequest()).isEqualTo(user)

    verify(exactly = 0) { mockCommunityApiClient.getStaffUserDetails(username) }
    verify(exactly = 0) { mockUserRepository.save(any()) }
  }

  @Test
  fun `getUserForRequest returns new User when one does not already exist, does call Community API and save`() {
    val username = "SOMEPERSON"
    val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

    every { mockHttpAuthService.getDeliusPrincipalOrThrow() } returns mockPrincipal
    every { mockPrincipal.name } returns username

    every { mockUserRepository.findByDeliusUsername(username) } returns null
    every { mockUserRepository.save(any()) } answers { it.invocation.args[0] as UserEntity }

    every { mockCommunityApiClient.getStaffUserDetails(username) } returns ClientResult.Success(
      HttpStatus.OK,
      StaffUserDetailsFactory()
        .withUsername(username)
        .withForenames("Jim")
        .withSurname("Jimmerson")
        .withStaffIdentifier(5678)
        .produce()
    )

    assertThat(userService.getUserForRequest()).matches {
      it.name == "Jim Jimmerson"
    }

    verify(exactly = 1) { mockCommunityApiClient.getStaffUserDetails(username) }
    verify(exactly = 1) { mockUserRepository.save(any()) }
  }
}
