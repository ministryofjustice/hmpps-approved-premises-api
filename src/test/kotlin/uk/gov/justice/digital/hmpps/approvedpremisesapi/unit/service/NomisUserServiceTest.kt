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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.NomisUserRolesForRequesterApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisGeneralAccountFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisStaffInformationFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService

class NomisUserServiceTest {
  private val mockHttpAuthService = mockk<HttpAuthService>()
  private val mockNomisUserRolesApiClient = mockk<NomisUserRolesApiClient>()
  private val mockNomisUserRolesForRequesterApiClient = mockk<NomisUserRolesForRequesterApiClient>()
  private val mockUserRepository = mockk<NomisUserRepository>()

  private val userService = NomisUserService(
    mockHttpAuthService,
    mockNomisUserRolesApiClient,
    mockNomisUserRolesForRequesterApiClient,
    mockUserRepository,
  )

  @Nested
  inner class GetUserForRequest {

    @Nested
    inner class WhenExistingUser {

      @Test
      fun `does not update user if Nomis-User-Roles API returns same email and activeCaseLoadId`() {
        val username = "SOMEPERSON"
        // setup auth service
        val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
        every { mockHttpAuthService.getNomisPrincipalOrThrow() } returns mockPrincipal
        every { mockPrincipal.token.tokenValue } returns "abc123"
        every { mockPrincipal.name } returns username

        // setup nomis roles api
        val oldUserData = NomisUserEntityFactory()
          .withNomisUsername(username)
          .withName("Bob Robson")
          .withEmail("same@example.com")
          .withActiveCaseloadId("123")
          .produce()

        val newUserData = NomisUserDetailFactory()
          .withFirstName("Jim")
          .withLastName("Jimmerson")
          .withEmail("same@example.com")
          .withActiveCaseloadId("123")
          .produce()

        every { mockNomisUserRolesForRequesterApiClient.getUserDetailsForMe("abc123") } returns ClientResult.Success(
          HttpStatus.OK,
          newUserData,
        )
        // setup repository
        every { mockUserRepository.findByNomisUsername(username) } returns oldUserData
        verify(exactly = 0) { mockUserRepository.save(any()) }

        assertThat(userService.getUserForRequest()).matches {
          it.nomisUsername == username &&
            it.email == "same@example.com" &&
            it.activeCaseloadId == "123"
        }
      }

      @Test
      fun `updates user if Nomis-User-Roles API returns new data`() {
        val username = "SOMEPERSON"
        // setup auth service
        val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
        every { mockHttpAuthService.getNomisPrincipalOrThrow() } returns mockPrincipal
        every { mockPrincipal.token.tokenValue } returns "abc123"
        every { mockPrincipal.name } returns username

        // setup nomis roles api
        val oldUserData = NomisUserEntityFactory()
          .withNomisUsername(username)
          .withEmail("old@example.com")
          .withActiveCaseloadId("DEF")
          .produce()
        val newUserData = NomisUserDetailFactory()
          .withUsername(username)
          .withEmail("new@example.com")
          .withActiveCaseloadId("ABC")
          .produce()

        every { mockNomisUserRolesForRequesterApiClient.getUserDetailsForMe("abc123") } returns ClientResult.Success(
          HttpStatus.OK,
          newUserData,
        )
        // setup repository
        every { mockUserRepository.findByNomisUsername(username) } returns oldUserData
        every { mockUserRepository.save(any()) } answers { it.invocation.args[0] as NomisUserEntity }

        assertThat(userService.getUserForRequest()).matches {
          it.nomisUsername == username &&
            it.email == "new@example.com" &&
            it.activeCaseloadId == "ABC"
        }
      }
    }

    @Nested
    inner class WhenNewUser {
      @Test
      fun `saves and returns new User with details from Nomis-User-Roles API`() {
        val username = "SOMEPERSON"
        // setup auth service
        val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
        every { mockHttpAuthService.getNomisPrincipalOrThrow() } returns mockPrincipal
        every { mockPrincipal.token.tokenValue } returns "abc123"
        every { mockPrincipal.name } returns username

        // setup nomis roles api
        val newUserData = NomisUserDetailFactory()
          .withUsername(username)
          .withFirstName("Jim")
          .withLastName("Jimmerson")
          .withStaffId(5678)
          .withAccountType("CLOSED")
          .withEmail("example@example.com")
          .withEnabled(false)
          .withActive(false)
          .withActiveCaseloadId("456")
          .produce()

        every { mockNomisUserRolesForRequesterApiClient.getUserDetailsForMe("abc123") } returns ClientResult.Success(
          HttpStatus.OK,
          newUserData,
        )
        // setup repository
        every { mockUserRepository.findByNomisUsername(username) } returns null
        every { mockUserRepository.save(any()) } answers { it.invocation.args[0] as NomisUserEntity }

        assertThat(userService.getUserForRequest()).matches {
          it.nomisUsername == username &&
            it.name == "Jim Jimmerson" &&
            it.accountType == "CLOSED" &&
            it.nomisStaffId.toInt() == 5678 &&
            it.email == "example@example.com" &&
            !it.isEnabled &&
            !it.isActive &&
            it.activeCaseloadId == "456"
        }
      }
    }
  }

  @Nested
  inner class GetUserByStaffId {

    @Nested
    inner class WhenExistingUser {

      @Test
      fun `returns user from database and does not create new user as already in database`() {
        val username = "SOMEPERSON"

        val user = NomisUserEntityFactory()
          .withNomisUsername(username)
          .withName("Bob Robson")
          .withEmail("same@example.com")
          .withActiveCaseloadId("123")
          .produce()

        every { mockUserRepository.findByNomisStaffId(user.nomisStaffId) } returns user
        verify(exactly = 0) { mockUserRepository.save(any()) }

        assertThat(userService.getUserByStaffId(user.nomisStaffId)).isEqualTo(user)
      }
    }

    @Nested
    inner class WhenNewUser {
      @Test
      fun `saves and returns new User with details from Nomis-User-Roles API`() {
        val username = "SOMEPERSON"

        val newUserData = NomisUserDetailFactory()
          .withUsername(username)
          .withFirstName("Jim")
          .withLastName("Jimmerson")
          .withStaffId(5678)
          .withAccountType("CLOSED")
          .withEmail("example@example.com")
          .withEnabled(false)
          .withActive(false)
          .withActiveCaseloadId("456")
          .produce()
        val generalAccount = NomisGeneralAccountFactory()
          .withUsername(username)
          .produce()
        val nomisStaffInformation = NomisStaffInformationFactory()
          .withNomisGeneralAccount(generalAccount)
          .produce()

        every { mockUserRepository.findByNomisStaffId(eq(newUserData.staffId)) } returns null
        every { mockNomisUserRolesApiClient.getUserStaffInformation(eq(newUserData.staffId)) } returns ClientResult.Success(
          HttpStatus.OK,
          nomisStaffInformation,
        )
        every { mockNomisUserRolesApiClient.getUserDetails(eq(username)) } returns ClientResult.Success(
          HttpStatus.OK,
          newUserData,
        )
        every { mockUserRepository.save(any()) } answers { it.invocation.args[0] as NomisUserEntity }

        assertThat(userService.getUserByStaffId(newUserData.staffId)).matches {
          it.nomisUsername == username &&
            it.name == "Jim Jimmerson" &&
            it.accountType == "CLOSED" &&
            it.nomisStaffId.toInt() == 5678 &&
            it.email == "example@example.com" &&
            !it.isEnabled &&
            !it.isActive &&
            it.activeCaseloadId == "456"
        }
      }
    }
  }
}
