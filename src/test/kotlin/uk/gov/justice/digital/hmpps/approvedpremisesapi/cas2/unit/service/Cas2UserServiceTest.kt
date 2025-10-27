package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.service

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.NomisUserRolesApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.NomisUserRolesForRequesterApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.PersonName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.NomisGeneralAccountFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.NomisStaffInformationFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.NomisUserDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService

@ExtendWith(MockKExtension::class)
class Cas2UserServiceTest {
  private val mockHttpAuthService = mockk<HttpAuthService>()
  private val mockNomisUserRolesApiClient = mockk<NomisUserRolesApiClient>()
  private val mockNomisUserRolesForRequesterApiClient = mockk<NomisUserRolesForRequesterApiClient>()
  private val mockUserRepository = mockk<NomisUserRepository>()
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val mockManageUsersApiClient = mockk<ManageUsersApiClient>()
  private val mockCas2UserRepository = mockk<Cas2UserRepository>()

  private val cas2UserService = Cas2UserService(
    mockHttpAuthService,
    mockNomisUserRolesApiClient,
    mockNomisUserRolesForRequesterApiClient,
    mockUserRepository,
    mockApDeliusContextApiClient,
    mockManageUsersApiClient,
    mockCas2UserRepository,
  )

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

  @Nested
  inner class GetNomisUserForRequest {

    @Nested
    inner class WhenExistingUser {

      @Test
      fun `does not update user if Nomis-User-Roles API returns same email and activeCaseLoadId`() {
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

        assertThat(cas2UserService.getUserForRequest()).matches {
          it.nomisUsername == username &&
            it.email == "same@example.com" &&
            it.activeCaseloadId == "123"
        }
      }

      @Test
      fun `updates user if Nomis-User-Roles API returns new data`() {
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

        assertThat(cas2UserService.getUserForRequest()).matches {
          it.nomisUsername == username &&
            it.email == "new@example.com" &&
            it.activeCaseloadId == "ABC"
        }
      }

      @Test
      fun `throws error if user has no email address`() {
        // setup auth service
        val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
        every { mockHttpAuthService.getNomisPrincipalOrThrow() } returns mockPrincipal
        every { mockPrincipal.token.tokenValue } returns "abc123"
        every { mockPrincipal.name } returns username

        val newUserData = NomisUserDetailFactory()
          .withUsername(username)
          .withEmail(null)
          .withActiveCaseloadId("ABC")
          .produce()

        val jwt = "abc123"
        every { mockNomisUserRolesForRequesterApiClient.getUserDetailsForMe(jwt) } returns ClientResult.Success(
          HttpStatus.OK,
          newUserData,
        )

        val error = assertThrows<IllegalStateException> { cas2UserService.getNomisUserForUsername(username, jwt) }
        assertThat(error).hasMessage("User $username does not have a primary email set in NOMIS")
      }
    }

    @Nested
    inner class WhenNewUser {

      @Test
      fun `saves and returns new User with details from Nomis-User-Roles API`() {
        // setup auth service
        val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
        every { mockHttpAuthService.getNomisPrincipalOrThrow() } returns mockPrincipal
        every { mockPrincipal.token.tokenValue } returns "abc123"
        every { mockPrincipal.name } returns username

        every { mockNomisUserRolesForRequesterApiClient.getUserDetailsForMe("abc123") } returns ClientResult.Success(
          HttpStatus.OK,
          newUserData,
        )
        // setup repository
        every { mockUserRepository.findByNomisUsername(username) } returns null
        every { mockUserRepository.save(any()) } answers { it.invocation.args[0] as NomisUserEntity }

        assertThat(cas2UserService.getUserForRequest()).matches {
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
  inner class GetCas2UserForRequest {

    @Nested
    inner class WhenExistingUser {

      @Test
      fun `get user and doesn't update details for nomis sources`() {
        val username = "SOMEPERSON"

        val existingCas2User = Cas2UserEntityFactory()
          .withUserType(Cas2UserType.NOMIS)
          .withName("This Should Not Be Updated")
          .withEmail("same@example.com")
          .withActiveNomisCaseloadId("123")
          .produce()

        // setup auth service
        val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
        every { mockHttpAuthService.getPrincipalOrThrow(listOf("nomis", "auth", "delius")) } returns mockPrincipal
        every { mockPrincipal.token.tokenValue } returns "abc123"
        every { mockPrincipal.authenticationSource() } returns "nomis"
        every { mockPrincipal.name } returns username
        every { mockCas2UserRepository.findByUsernameAndUserTypeAndServiceOrigin(username, Cas2UserType.NOMIS, existingCas2User.serviceOrigin) } returns existingCas2User
        every { mockCas2UserRepository.save(any()) } answers { it.invocation.args[0] as Cas2UserEntity }

        val existingNomisUser = NomisUserDetailFactory()
          .withUsername(username)
          .withFirstName("Bob")
          .withLastName("Robson")
          .withEmail("new.email@example.com")
          .withActiveCaseloadId("456")
          .produce()
        every { mockNomisUserRolesForRequesterApiClient.getUserDetailsForMe("abc123") } returns ClientResult.Success(
          HttpStatus.OK,
          existingNomisUser,
        )

        assertThat(cas2UserService.getCas2UserForRequest(serviceOrigin = existingCas2User.serviceOrigin)).matches {
          it.id == existingCas2User.id &&
            it.name == "This Should Not Be Updated" &&
            it.email == "new.email@example.com" &&
            it.activeNomisCaseloadId == "456"
        }
      }

      @Test
      fun `get user and doesn't update details for delius sources`() {
        val username = "SOMEPERSON"

        val existingCas2User = Cas2UserEntityFactory()
          .withUserType(Cas2UserType.NOMIS)
          .withName("This Should Not Be Updated")
          .withEmail("same@example.com")
          .withActiveNomisCaseloadId("123")
          .withServiceOrigin(Cas2ServiceOrigin.BAIL)
          .withDeliusTeamCodes(
            listOf(
              StaffDetailFactory.team().code,
              StaffDetailFactory.team().code,
            ),
          )
          .produce()

        // setup auth service
        val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
        every { mockHttpAuthService.getPrincipalOrThrow(listOf("nomis", "auth", "delius")) } returns mockPrincipal
        every { mockPrincipal.token.tokenValue } returns "abc123"
        every { mockPrincipal.authenticationSource() } returns "delius"
        every { mockPrincipal.name } returns username
        every { mockCas2UserRepository.findByUsernameAndUserTypeAndServiceOrigin(username, Cas2UserType.DELIUS, Cas2ServiceOrigin.BAIL) } returns existingCas2User
        every { mockCas2UserRepository.save(any()) } answers { it.invocation.args[0] as Cas2UserEntity }

        val existingDeliusUser = StaffDetailFactory.staffDetail(
          email = "new.email@example.com",
          teams = listOf(StaffDetailFactory.team()),
        )
        every { mockApDeliusContextApiClient.getStaffDetail(username) } returns ClientResult.Success(
          HttpStatus.OK,
          existingDeliusUser,
        )

        val cas2user = cas2UserService.getCas2UserForRequest(existingCas2User.serviceOrigin)
        assertThat(cas2user).isNotNull()
        assertThat(cas2user.id).isEqualTo(existingCas2User.id)
        assertThat(cas2user.name).isEqualTo("This Should Not Be Updated")
        assertThat(cas2user.email).isEqualTo("new.email@example.com")
      }
    }

    @Nested
    inner class WhenNewUser {
      @Test
      fun `saves and returns new nomis user`() {
        val username = "SOMEPERSON"

        // setup auth service
        val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
        every { mockHttpAuthService.getPrincipalOrThrow(listOf("nomis", "auth", "delius")) } returns mockPrincipal
        every { mockPrincipal.token.tokenValue } returns "abc123"
        every { mockPrincipal.authenticationSource() } returns "nomis"
        every { mockPrincipal.name } returns username
        every { mockCas2UserRepository.findByUsernameAndUserTypeAndServiceOrigin(username, Cas2UserType.NOMIS, Cas2ServiceOrigin.HDC) } returns null
        every { mockCas2UserRepository.save(any()) } answers { it.invocation.args[0] as Cas2UserEntity }

        val existingNomisUser = NomisUserDetailFactory()
          .withUsername(username)
          .withFirstName("Bob")
          .withLastName("Robson")
          .withEmail("new.email@example.com")
          .withActiveCaseloadId("456")
          .produce()
        every { mockNomisUserRolesForRequesterApiClient.getUserDetailsForMe("abc123") } returns ClientResult.Success(
          HttpStatus.OK,
          existingNomisUser,
        )

        assertThat(cas2UserService.getCas2UserForRequest(Cas2ServiceOrigin.HDC)).matches {
          it.name == "Bob Robson" &&
            it.email == "new.email@example.com" &&
            it.activeNomisCaseloadId == "456"
        }
      }

      @Test
      fun `saves and returns new delius user`() {
        val username = "SOMEPERSON"

        // setup auth service
        val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
        every { mockHttpAuthService.getPrincipalOrThrow(listOf("nomis", "auth", "delius")) } returns mockPrincipal
        every { mockPrincipal.token.tokenValue } returns "abc123"
        every { mockPrincipal.authenticationSource() } returns "delius"
        every { mockPrincipal.name } returns username
        every { mockCas2UserRepository.findByUsernameAndUserTypeAndServiceOrigin(username, Cas2UserType.DELIUS, Cas2ServiceOrigin.BAIL) } returns null
        every { mockCas2UserRepository.save(any()) } answers { it.invocation.args[0] as Cas2UserEntity }

        val existingDeliusUser = StaffDetailFactory.staffDetail(
          name = PersonName("Bob", "Robson"),
          email = "new.email@example.com",
          teams = listOf(StaffDetailFactory.team()),
        )
        every { mockApDeliusContextApiClient.getStaffDetail(username) } returns ClientResult.Success(
          HttpStatus.OK,
          existingDeliusUser,
        )

        val cas2user = cas2UserService.getCas2UserForRequest(Cas2ServiceOrigin.BAIL)
        assertThat(cas2user).isNotNull()
        assertThat(cas2user.name).isEqualTo("Bob Robson")
        assertThat(cas2user.email).isEqualTo("new.email@example.com")
      }
    }
  }

  @Nested
  inner class GetUserByStaffId {

    @BeforeEach
    fun setup() {
      every { mockNomisUserRolesApiClient.getUserStaffInformation(eq(newUserData.staffId)) } returns ClientResult.Success(
        HttpStatus.OK,
        nomisStaffInformation,
      )
      every { mockNomisUserRolesApiClient.getUserDetails(eq(username)) } returns ClientResult.Success(
        HttpStatus.OK,
        newUserData,
      )
    }

    @Nested
    inner class WhenExistingUser {

      @Test
      fun `returns user from database and does not create new user as already in database`() {
        val user = NomisUserEntityFactory()
          .withNomisUsername(username)
          .withName("Bob Robson")
          .withEmail("same@example.com")
          .withActiveCaseloadId("123")
          .produce()

        every { mockUserRepository.findByNomisStaffId(user.nomisStaffId) } returns user
        verify(exactly = 0) { mockUserRepository.save(any()) }

        assertThat(cas2UserService.getUserByStaffId(user.nomisStaffId)).isEqualTo(user)
      }
    }

    @Nested
    inner class WhenNewUser {
      @Test
      fun `saves and returns new User with details from Nomis-User-Roles API`() {
        every { mockUserRepository.findByNomisStaffId(eq(newUserData.staffId)) } returns null

        every { mockUserRepository.save(any()) } answers { it.invocation.args[0] as NomisUserEntity }
        every { mockUserRepository.findByNomisUsername(username) } returns null

        assertThat(cas2UserService.getUserByStaffId(newUserData.staffId)).matches {
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

      @Test
      fun `does not save when existing`() {
        every { mockUserRepository.findByNomisStaffId(eq(newUserData.staffId)) } returns null

        val userEntity = NomisUserEntityFactory().produce()
        every { mockUserRepository.findByNomisUsername(username) } returns userEntity

        val result = cas2UserService.getUserByStaffId(newUserData.staffId)
        verify(exactly = 0) { mockUserRepository.save(any()) }
        assertThat(result).isEqualTo(userEntity)
      }
    }

    @Test
    fun `returns gracefully if the username is existing`() {
      val userEntity = NomisUserEntityFactory().produce()
      every { mockUserRepository.findByNomisStaffId(newUserData.staffId) } returns null
      every { mockUserRepository.save(any()) } throws DataIntegrityViolationException("DataIntegrityViolationException")
      every { mockUserRepository.findByNomisUsername(username) } returns null andThen userEntity

      val result = cas2UserService.getUserByStaffId(newUserData.staffId)
      verify(exactly = 1) { mockUserRepository.save(any()) }
      verify(exactly = 2) { mockUserRepository.findByNomisUsername(newUserData.username) }
      assertThat(result).isEqualTo(userEntity)
    }

    @Test
    fun `still throws exception when username is not existing`() {
      every { mockUserRepository.findByNomisUsername(any()) } returns null
      every { mockUserRepository.save(any()) } throws DataIntegrityViolationException("DataIntegrityViolationException")
      every { mockUserRepository.findByNomisStaffId(newUserData.staffId) } returns null

      assertThrows<IllegalStateException> { cas2UserService.getUserByStaffId(newUserData.staffId) }
      verify(exactly = 1) { mockUserRepository.save(any()) }
      verify(exactly = 2) { mockUserRepository.findByNomisUsername(newUserData.username) }
    }
  }
}
