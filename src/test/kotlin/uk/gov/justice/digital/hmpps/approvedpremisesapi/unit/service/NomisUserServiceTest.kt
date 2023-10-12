package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.NomisUserRolesApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService

class NomisUserServiceTest {
  private val mockHttpAuthService = mockk<HttpAuthService>()
  private val mockNomisUserRolesApiClient = mockk<NomisUserRolesApiClient>()
  private val mockUserRepository = mockk<NomisUserRepository>()

  private val userService = NomisUserService(
    mockHttpAuthService,
    mockNomisUserRolesApiClient,
    mockUserRepository,
  )

  @Nested
  inner class GetUserForRequest {
    @Test
    fun `returns existing User when exists, does not call Nomis-User-Roles API or save`
    () {
      val username = "SOMEPERSON"
      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

      every { mockHttpAuthService.getNomisPrincipalOrThrow() } returns mockPrincipal
      every { mockPrincipal.name } returns username

      val user = NomisUserEntityFactory().produce()

      every { mockUserRepository.findByNomisUsername(username) } returns user

      assertThat(userService.getUserForRequest()).isEqualTo(user)

      verify(exactly = 0) { mockNomisUserRolesApiClient.getUserDetails(username) }
      verify(exactly = 0) { mockUserRepository.save(any()) }
    }

    @Test
    fun `returns new User when one does not already exist, does call Nomis-User-Roles API and save`() {
      val username = "SOMEPERSON"
      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

      every { mockHttpAuthService.getNomisPrincipalOrThrow() } returns mockPrincipal
      every { mockPrincipal.name } returns username

      every { mockUserRepository.findByNomisUsername(username) } returns null
      every { mockUserRepository.save(any()) } answers { it.invocation.args[0] as NomisUserEntity }

      every { mockNomisUserRolesApiClient.getUserDetails(username) } returns ClientResult.Success(
        HttpStatus.OK,
        NomisUserDetailFactory()
          .withUsername(username)
          .withFirstName("Jim")
          .withLastName("Jimmerson")
          .withStaffId(5678)
          .produce(),
      )

      assertThat(userService.getUserForRequest()).matches {
        it.name == "Jim Jimmerson"
        it.nomisStaffId.toInt() == 5678
        it.nomisUsername == "SOMEPERSON"
      }

      verify(exactly = 1) { mockNomisUserRolesApiClient.getUserDetails(username) }
      verify(exactly = 1) { mockUserRepository.save(any()) }
    }
  }
}
