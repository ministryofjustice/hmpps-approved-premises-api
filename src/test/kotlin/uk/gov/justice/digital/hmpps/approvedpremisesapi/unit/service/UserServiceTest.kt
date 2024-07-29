package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationAreaProbationRegionMappingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserTeamMembershipFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserQualificationAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationAreaProbationRegionMappingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.KeyValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.RequestContextService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserMappingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.getTeamCodes
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as APIUserQualification

class UserServiceTest {
  private val mockRequestContextService = mockk<RequestContextService>()
  private val mockHttpAuthService = mockk<HttpAuthService>()
  private val mockOffenderService = mockk<OffenderService>()
  private val mockCommunityApiClient = mockk<CommunityApiClient>()
  private val mockUserRepository = mockk<UserRepository>()
  private val mockUserRoleAssignmentRepository = mockk<UserRoleAssignmentRepository>()
  private val mockUserQualificationAssignmentRepository = mockk<UserQualificationAssignmentRepository>()
  private val mockProbationRegionRepository = mockk<ProbationRegionRepository>()
  private val mockProbationAreaProbationRegionMappingRepository = mockk<ProbationAreaProbationRegionMappingRepository>()
  private val mockProbationDeliveryUnitRepository = mockk<ProbationDeliveryUnitRepository>()
  private val mockCas1UserMappingService = mockk<Cas1UserMappingService>()
  private val mockFeatureFlagService = mockk<FeatureFlagService>()

  private val userService = UserService(
    false,
    mockRequestContextService,
    mockHttpAuthService,
    mockOffenderService,
    mockCommunityApiClient,
    mockUserRepository,
    mockUserRoleAssignmentRepository,
    mockUserQualificationAssignmentRepository,
    mockProbationRegionRepository,
    mockProbationAreaProbationRegionMappingRepository,
    mockCas1UserMappingService,
    mockProbationDeliveryUnitRepository,
    mockFeatureFlagService,
  )

  @Nested
  inner class GetExistingUserOrCreate {

    @Test
    fun `getExistingUserOrCreate calls overloaded function with throwExceptionOnStaffRecordNotFound parameter set false`() {
      val username = "SOMEPERSON"

      val user = UserEntityFactory()
        .withDefaults()
        .produce()

      every { mockUserRepository.findByDeliusUsername(username) } returns user

      assertThat(userService.getExistingUserOrCreate(username)).isEqualTo(user)
      verify(exactly = 1) { userService.getExistingUserOrCreate(username, false) }
    }

    @Test
    fun `getExistingUserOrCreate when user has no delius staff record and throwExceptionOnStaffRecordNotFound is false does not throw error`() {
      val username = "SOMEPERSON"

      every { mockUserRepository.findByDeliusUsername(username) } returns null
      every { mockCommunityApiClient.getStaffUserDetails(username) } returns ClientResult.Failure.StatusCode(
        HttpMethod.GET,
        "/secure/staff/username",
        HttpStatus.NOT_FOUND,
        body = null,
      )

      val result = userService.getExistingUserOrCreate(username, throwExceptionOnStaffRecordNotFound = false)

      assertThat(result.staffRecordFound).isFalse()
      assertThat(result.user).isNull()
    }

    @Test
    fun `getExistingUserOrCreate when user has no delius staff record and throwExceptionOnStaffRecordNotFound is true throws error`() {
      val username = "SOMEPERSON"

      every { mockUserRepository.findByDeliusUsername(username) } returns null
      every { mockCommunityApiClient.getStaffUserDetails(username) } returns ClientResult.Failure.StatusCode(
        HttpMethod.GET,
        "/secure/staff/username",
        HttpStatus.NOT_FOUND,
        body = null,
      )

      assertThrows<RuntimeException> { userService.getExistingUserOrCreate(username, true) }
    }

    @Test
    fun `getExistingUserOrCreate when user has no delius staff record and throwExceptionOnStaffRecordNotFound is true and clientResult is failure throws error`() {
      val username = "SOMEPERSON"

      every { mockUserRepository.findByDeliusUsername(username) } returns null
      every { mockCommunityApiClient.getStaffUserDetails(username) } returns ClientResult.Failure.PreemptiveCacheTimeout("", "", 0)

      assertThrows<RuntimeException> { userService.getExistingUserOrCreate(username, true) }
    }

    @Test
    fun `getExistingUserOrCreate returns existing user`() {
      val username = "SOMEPERSON"

      val user = UserEntityFactory()
        .withDefaults()
        .produce()

      every { mockUserRepository.findByDeliusUsername(username) } returns user

      assertThat(userService.getExistingUserOrCreate(username)).isEqualTo(user)

      verify(exactly = 0) { mockUserRepository.save(any()) }
    }

    @Test
    fun `getExistingUserOrCreate creates new user`() {
      val username = "SOMEPERSON"
      val pduDeliusCode = randomStringMultiCaseWithNumbers(7)
      val brought = KeyValue(
        code = pduDeliusCode,
        description = randomStringMultiCaseWithNumbers(10),
      )
      every { mockUserRepository.findByDeliusUsername(username) } returns null
      every { mockUserRepository.save(any()) } answers { it.invocation.args[0] as UserEntity }

      val deliusUser = StaffUserDetailsFactory()
        .withUsername(username)
        .withForenames("Jim")
        .withSurname("Jimmerson")
        .withStaffIdentifier(5678)
        .withProbationAreaCode("AREACODE")
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory().withCode("TC1").withBorough(brought).produce(),
            StaffUserTeamMembershipFactory().withCode("TC2").withBorough(brought).produce(),
          ),
        )
        .produce()

      every { mockCommunityApiClient.getStaffUserDetails(username) } returns ClientResult.Success(
        HttpStatus.OK,
        deliusUser,
      )

      val probationRegion = ProbationRegionEntityFactory()
        .withApArea(ApAreaEntityFactory().produce())
        .produce()

      every { mockProbationAreaProbationRegionMappingRepository.findByProbationAreaDeliusCode("AREACODE") } returns ProbationAreaProbationRegionMappingEntityFactory()
        .withProbationRegion(probationRegion)
        .withProbationAreaDeliusCode("AREACODE")
        .produce()

      val apArea = ApAreaEntityFactory().produce()

      every { mockCas1UserMappingService.determineApArea(probationRegion, deliusUser) } returns apArea

      every { mockProbationDeliveryUnitRepository.findByDeliusCode(pduDeliusCode) } returns ProbationDeliveryUnitEntityFactory()
        .withProbationRegion(probationRegion)
        .withDeliusCode(pduDeliusCode)
        .produce()

      val result = userService.getExistingUserOrCreate(username)

      assertThat(result.name).isEqualTo("Jim Jimmerson")
      assertThat(result.teamCodes).isEqualTo(listOf("TC1", "TC2"))
      assertThat(result.apArea).isEqualTo(apArea)
      assertThat(result.probationDeliveryUnit?.deliusCode).isEqualTo(pduDeliusCode)
      assertThat(result.createdAt).isWithinTheLastMinute()

      verify(exactly = 1) { mockCommunityApiClient.getStaffUserDetails(username) }
      verify(exactly = 1) { mockUserRepository.save(any()) }
      verify(exactly = 1) { mockProbationAreaProbationRegionMappingRepository.findByProbationAreaDeliusCode(any()) }
    }

    @Test
    fun `getExistingUserOrCreate throws intenal server error problem if can't resolve region`() {
      val username = "SOMEPERSON"
      val pduDeliusCode = randomStringMultiCaseWithNumbers(7)
      val brought = KeyValue(
        code = pduDeliusCode,
        description = randomStringMultiCaseWithNumbers(10),
      )
      every { mockUserRepository.findByDeliusUsername(username) } returns null
      every { mockUserRepository.save(any()) } answers { it.invocation.args[0] as UserEntity }

      val deliusUser = StaffUserDetailsFactory()
        .withUsername(username)
        .withForenames("Jim")
        .withSurname("Jimmerson")
        .withStaffIdentifier(5678)
        .withProbationAreaCode("AREACODE")
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory().withCode("TC1").withBorough(brought).produce(),
            StaffUserTeamMembershipFactory().withCode("TC2").withBorough(brought).produce(),
          ),
        )
        .produce()

      every { mockCommunityApiClient.getStaffUserDetails(username) } returns ClientResult.Success(
        HttpStatus.OK,
        deliusUser,
      )

      every { mockProbationAreaProbationRegionMappingRepository.findByProbationAreaDeliusCode("AREACODE") } returns null

      assertThatThrownBy {
        userService.getExistingUserOrCreate(username)
      }
        .hasMessage("Unknown probation region code 'AREACODE' for user 'SOMEPERSON'")
        .isInstanceOf(RuntimeException::class.java)
    }
  }

  @Nested
  inner class GetUserForRequest {

    @Test
    fun `getUserForRequest returns existing User when exists`() {
      val username = "SOMEPERSON"
      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

      every { mockRequestContextService.getServiceForRequest() } returns ServiceName.approvedPremises

      every { mockHttpAuthService.getDeliusPrincipalOrThrow() } returns mockPrincipal
      every { mockPrincipal.name } returns username

      val user = UserEntityFactory()
        .withDefaultProbationRegion()
        .produce()

      every { mockUserRepository.findByDeliusUsername(username) } returns user

      assertThat(userService.getUserForRequest()).isEqualTo(user)

      verify(exactly = 0) { mockCommunityApiClient.getStaffUserDetails(username) }
      verify(exactly = 0) { mockUserRepository.save(any()) }
    }

    @Test
    fun `getUserForRequest returns new persisted User when one does not already exist`() {
      val username = "SOMEPERSON"
      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

      every { mockRequestContextService.getServiceForRequest() } returns ServiceName.approvedPremises

      every { mockHttpAuthService.getDeliusPrincipalOrThrow() } returns mockPrincipal
      every { mockPrincipal.name } returns username

      every { mockUserRepository.findByDeliusUsername(username) } returns null
      every { mockUserRepository.save(any()) } answers { it.invocation.args[0] as UserEntity }

      val deliusUser = StaffUserDetailsFactory()
        .withUsername(username)
        .withForenames("Jim")
        .withSurname("Jimmerson")
        .withStaffIdentifier(5678)
        .withProbationAreaCode("AREACODE")
        .produce()
      every { mockCommunityApiClient.getStaffUserDetails(username) } returns ClientResult.Success(
        HttpStatus.OK,
        deliusUser,
      )

      val probationRegion = ProbationRegionEntityFactory()
        .withApArea(ApAreaEntityFactory().produce())
        .produce()

      every { mockProbationAreaProbationRegionMappingRepository.findByProbationAreaDeliusCode("AREACODE") } returns ProbationAreaProbationRegionMappingEntityFactory()
        .withProbationRegion(probationRegion)
        .withProbationAreaDeliusCode("AREACODE")
        .produce()

      val apArea = ApAreaEntityFactory().produce()

      every { mockCas1UserMappingService.determineApArea(probationRegion, deliusUser) } returns apArea

      assertThat(userService.getUserForRequest()).matches {
        it.name == "Jim Jimmerson"
      }

      verify(exactly = 1) { mockCommunityApiClient.getStaffUserDetails(username) }
      verify(exactly = 1) { mockUserRepository.save(any()) }
      verify(exactly = 1) { mockProbationAreaProbationRegionMappingRepository.findByProbationAreaDeliusCode(any()) }
    }

    @Test
    fun `getUserForRequest assigns a default role of CAS3_REFERRER for CAS3 if the user has no Temporary Accommodation roles`() {
      val username = "SOMEPERSON"
      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

      every { mockRequestContextService.getServiceForRequest() } returns ServiceName.temporaryAccommodation

      every { mockHttpAuthService.getDeliusPrincipalOrThrow() } returns mockPrincipal
      every { mockPrincipal.name } returns username

      val expectedUser = UserEntityFactory()
        .withDefaults()
        .produce()

      val roleAssignment = UserRoleAssignmentEntityFactory()
        .withUser(expectedUser)
        .withRole(UserRole.CAS1_WORKFLOW_MANAGER)
        .produce()

      expectedUser.roles += roleAssignment

      every { mockUserRepository.findByDeliusUsername(username) } returns expectedUser

      every { mockUserRoleAssignmentRepository.save(any()) } returnsArgument 0

      val actualUser = userService.getUserForRequest()

      assertThat(actualUser).isEqualTo(expectedUser)
      assertThat(actualUser.hasRole(UserRole.CAS3_REFERRER)).isTrue

      verify(exactly = 1) {
        mockUserRoleAssignmentRepository.save(
          match {
            it.user == actualUser &&
              it.role == UserRole.CAS3_REFERRER
          },
        )
      }
    }

    @Test
    fun `getUserForRequest does not assign a default role for CAS3 if the user already has a Temporary Accommodation role`() {
      val username = "SOMEPERSON"
      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

      every { mockRequestContextService.getServiceForRequest() } returns ServiceName.temporaryAccommodation

      every { mockHttpAuthService.getDeliusPrincipalOrThrow() } returns mockPrincipal
      every { mockPrincipal.name } returns username

      val expectedUser = UserEntityFactory()
        .withDefaults()
        .produce()

      val roleAssignment = UserRoleAssignmentEntityFactory()
        .withUser(expectedUser)
        .withRole(UserRole.CAS3_ASSESSOR)
        .produce()

      expectedUser.roles += roleAssignment

      every { mockUserRepository.findByDeliusUsername(username) } returns expectedUser

      every { mockUserRoleAssignmentRepository.save(any()) } returnsArgument 0

      val actualUser = userService.getUserForRequest()

      assertThat(actualUser).isEqualTo(expectedUser)
      assertThat(actualUser.hasRole(UserRole.CAS3_ASSESSOR)).isTrue()

      verify(exactly = 0) {
        mockUserRoleAssignmentRepository.save(any())
      }
    }

    @ParameterizedTest
    @EnumSource(value = ServiceName::class, mode = EnumSource.Mode.EXCLUDE, names = ["temporaryAccommodation"])
    fun `getUserForRequest does not assign a default role if not CAS3`(serviceName: ServiceName) {
      val username = "SOMEPERSON"
      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

      every { mockRequestContextService.getServiceForRequest() } returns serviceName

      every { mockHttpAuthService.getDeliusPrincipalOrThrow() } returns mockPrincipal
      every { mockPrincipal.name } returns username

      val expectedUser = UserEntityFactory()
        .withDefaults()
        .produce()

      every { mockUserRepository.findByDeliusUsername(username) } returns expectedUser

      every { mockUserRoleAssignmentRepository.save(any()) } returnsArgument 0

      val actualUser = userService.getUserForRequest()

      assertThat(actualUser).isEqualTo(expectedUser)
      assertThat(actualUser.roles).isEmpty()

      verify(exactly = 0) {
        mockUserRoleAssignmentRepository.save(any())
      }
    }
  }

  @Nested
  inner class GetUserForRequestOrNull {

    @Test
    fun `getUserForRequestOrNull returns User when exists, does not call Community API or save`() {
      val username = "SOMEPERSON"
      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

      every { mockHttpAuthService.getDeliusPrincipalOrNull() } returns mockPrincipal
      every { mockPrincipal.name } returns username

      val user = UserEntityFactory()
        .withDefaults()
        .produce()

      every { mockUserRepository.findByDeliusUsername(username) } returns user

      assertThat(userService.getUserForRequestOrNull()).isEqualTo(user)

      verify(exactly = 0) { mockCommunityApiClient.getStaffUserDetails(username) }
      verify(exactly = 0) { mockUserRepository.save(any()) }
    }

    @Test
    fun `getUserForRequestOrNull returns null when User does not already exist, does not call Community API or save`() {
      val username = "SOMEPERSON"
      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

      every { mockHttpAuthService.getDeliusPrincipalOrNull() } returns mockPrincipal
      every { mockPrincipal.name } returns username

      every { mockUserRepository.findByDeliusUsername(username) } returns null

      assertThat(userService.getUserForRequestOrNull()).isNull()

      verify(exactly = 0) { mockCommunityApiClient.getStaffUserDetails(username) }
      verify(exactly = 0) { mockUserRepository.save(any()) }
    }

    @Test
    fun `getUserForRequestOrNull returns null when no principal is available`() {
      every { mockHttpAuthService.getDeliusPrincipalOrNull() } returns null

      assertThat(userService.getUserForRequestOrNull()).isNull()
    }
  }

  @Nested
  inner class UpdateUserPduFromCommunityApiById {

    val user = UserEntityFactory().withDefaults().produce()

    @BeforeEach
    fun setup() {
      every { mockUserRepository.findByIdOrNull(user.id) } returns user
    }

    @Test
    fun `Throw exception if can't determine PDU, no teams`() {
      val deliusUser = StaffUserDetailsFactory()
        .withUsername("theusername")
        .withTeams(emptyList())
        .produce()

      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        deliusUser,
      )

      assertThatThrownBy {
        userService.updateUserPduFromCommunityApiById(user.id)
      }.hasMessage("PDU could not be determined for user theusername. Considered 0 teams")
    }

    @Test
    fun `Throw exception if can't determine PDU, no teams without end date`() {
      val deliusUser = StaffUserDetailsFactory()
        .withUsername("theusername")
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .withEndDate(LocalDate.now())
              .produce(),
          ),
        )
        .produce()

      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        deliusUser,
      )

      assertThatThrownBy {
        userService.updateUserPduFromCommunityApiById(user.id)
      }.hasMessage("PDU could not be determined for user theusername. Considered 0 teams")
    }

    @Test
    fun `Throw exception if no mapping for only team's borough`() {
      val deliusUser = StaffUserDetailsFactory()
        .withUsername("theusername")
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .withCode("team1")
              .withDescription("team 1")
              .withBorough(KeyValue("boroughcode1", "borough1"))
              .withStartDate(LocalDate.of(2024, 1, 1))
              .withEndDate(null)
              .produce(),
          ),
        )
        .produce()

      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        deliusUser,
      )

      every { mockProbationDeliveryUnitRepository.findByDeliusCode("boroughcode1") } returns null

      assertThatThrownBy {
        userService.updateUserPduFromCommunityApiById(user.id)
      }.hasMessage("PDU could not be determined for user theusername. Considered 1 teams team 1 (team1) with borough borough1 (boroughcode1)")
    }

    @Test
    fun `Throw exception if no mapping for any active team's borough`() {
      val deliusUser = StaffUserDetailsFactory()
        .withUsername("theusername")
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .withCode("team3")
              .withDescription("team 3")
              .withBorough(KeyValue("boroughcode3", "borough3"))
              .withStartDate(LocalDate.of(2024, 1, 3))
              .withEndDate(LocalDate.of(2024, 1, 4))
              .produce(),
            StaffUserTeamMembershipFactory()
              .withCode("team2")
              .withDescription("team 2")
              .withBorough(KeyValue("boroughcode2", "borough2"))
              .withStartDate(LocalDate.of(2024, 1, 2))
              .withEndDate(null)
              .produce(),
            StaffUserTeamMembershipFactory()
              .withCode("team1")
              .withDescription("team 1")
              .withBorough(KeyValue("boroughcode1", "borough1"))
              .withStartDate(LocalDate.of(2024, 1, 1))
              .withEndDate(null)
              .produce(),
          ),
        )
        .produce()

      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        deliusUser,
      )

      every { mockProbationDeliveryUnitRepository.findByDeliusCode("boroughcode2") } returns null
      every { mockProbationDeliveryUnitRepository.findByDeliusCode("boroughcode1") } returns null

      assertThatThrownBy {
        userService.updateUserPduFromCommunityApiById(user.id)
      }.hasMessage("PDU could not be determined for user theusername. Considered 2 teams team 2 (team2) with borough borough2 (boroughcode2), team 1 (team1) with borough borough1 (boroughcode1)")
    }

    @Test
    fun `Update PDU using latest team that has a mapping`() {
      val deliusUser = StaffUserDetailsFactory()
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .withBorough(KeyValue("boroughcode2", "borough2"))
              .withStartDate(LocalDate.of(2024, 1, 2))
              .withStartDate(LocalDate.of(2024, 1, 3))
              .withEndDate(LocalDate.of(2024, 1, 4))
              .produce(),
            StaffUserTeamMembershipFactory()
              .withBorough(KeyValue("nomapping", "nomapping"))
              .withStartDate(LocalDate.of(2024, 1, 3))
              .withEndDate(null)
              .produce(),
            StaffUserTeamMembershipFactory()
              .withBorough(KeyValue("boroughcode2", "borough2"))
              .withStartDate(LocalDate.of(2024, 1, 2))
              .withEndDate(null)
              .produce(),
            StaffUserTeamMembershipFactory()
              .withBorough(KeyValue("boroughcode1", "borough1"))
              .withStartDate(LocalDate.of(2024, 1, 1))
              .withEndDate(null)
              .produce(),
          ),
        )
        .produce()

      every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        deliusUser,
      )

      every { mockProbationDeliveryUnitRepository.findByDeliusCode("nomapping") } returns null

      val pdu = ProbationDeliveryUnitEntityFactory()
        .withDefaults()
        .produce()
      every { mockProbationDeliveryUnitRepository.findByDeliusCode("boroughcode2") } returns pdu

      val persistedUser = slot<UserEntity>()
      every { mockUserRepository.save(capture(persistedUser)) } answers { it.invocation.args[0] as UserEntity }

      userService.updateUserPduFromCommunityApiById(user.id)

      assertThat(persistedUser.captured.probationDeliveryUnit).isEqualTo(pdu)
    }
  }

  @Nested
  inner class UpdateUserFromCommunityApiById {
    private val id = UUID.fromString("21b61d19-3a96-4b88-8df9-a5e89bc6fe73")
    private val email = "foo@example.com"
    private val telephoneNumber = "0123456789"
    private val username = "SOMEPERSON"
    private val forename = "Jim"
    private val surname = "Jimmerson"
    private val staffIdentifier = 5678
    private val staffCode = "STAFF1"

    private val probationRegion = ProbationRegionEntityFactory()
      .withDefaults()
      .produce()

    private val userFactory = UserEntityFactory()
      .withDefaults()
      .withDeliusUsername(username)
      .withName("$forename $surname")
      .withEmail(email)
      .withDeliusStaffIdentifier(staffIdentifier.toLong())
      .withTelephoneNumber(telephoneNumber)
      .withDeliusStaffCode(staffCode)
      .withProbationRegion(probationRegion)

    private val staffUserDetailsFactory = StaffUserDetailsFactory()
      .withUsername(username)
      .withForenames(forename)
      .withSurname(surname)
      .withEmail(email)
      .withTelephoneNumber(telephoneNumber)
      .withStaffCode(staffCode)
      .withProbationAreaCode(probationRegion.deliusCode)
      .withStaffIdentifier(staffIdentifier.toLong())

    @BeforeEach
    fun setup() {
      every { mockUserRepository.save(any()) } answers { it.invocation.args[0] as UserEntity }
    }

    @Test
    fun `it does not update the user entity if fields of interest are the same as delius`() {
      val user = userFactory.produce()
      val deliusUser = staffUserDetailsFactory.produce()

      every { mockUserRepository.findByIdOrNull(id) } returns user
      every { mockCommunityApiClient.getStaffUserDetails(username) } returns ClientResult.Success(
        HttpStatus.OK,
        deliusUser,
      )

      val result = userService.updateUserFromCommunityApiById(id, ServiceName.approvedPremises)

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      val entity = (result as AuthorisableActionResult.Success).entity

      assertThat(entity.id).isEqualTo(user.id)

      verify(exactly = 0) { mockUserRepository.save(any()) }
    }

    @ParameterizedTest
    @EnumSource(ServiceName::class)
    fun `it will update the user entity if fields of interest are the same as delius if force = true`(forService: ServiceName) {
      val user = userFactory.produce()
      val deliusUser = staffUserDetailsFactory.produce()

      assertUserUpdated(user, deliusUser, probationRegion, forService, force = true)
    }

    @ParameterizedTest
    @EnumSource(ServiceName::class)
    fun `it updates the user entity if the email has been updated in delius`(forService: ServiceName) {
      val user = userFactory.produce()

      val deliusUser = staffUserDetailsFactory
        .withEmail(email + "updated")
        .produce()

      assertUserUpdated(user, deliusUser, probationRegion, forService)
    }

    @ParameterizedTest
    @EnumSource(ServiceName::class)
    fun `it updates the user entity if the full name has been updated in delius`(forService: ServiceName) {
      val user = userFactory.produce()

      val deliusUser = staffUserDetailsFactory
        .withForenames(forename)
        .withSurname(surname + "updated")
        .produce()

      assertUserUpdated(user, deliusUser, probationRegion, forService)
    }

    @ParameterizedTest
    @EnumSource(ServiceName::class)
    fun `it updates the user entity if the telephone number has been updated in delius`(forService: ServiceName) {
      val user = userFactory.produce()

      val deliusUser = staffUserDetailsFactory
        .withTelephoneNumber(telephoneNumber + "updated")
        .produce()

      assertUserUpdated(user, deliusUser, probationRegion, forService)
    }

    @ParameterizedTest
    @EnumSource(ServiceName::class)
    fun `it updates the user entity if the staff code number has been updated in delius`(forService: ServiceName) {
      val user = userFactory.produce()

      val deliusUser = staffUserDetailsFactory
        .withStaffCode(staffCode + "updated")
        .produce()

      assertUserUpdated(user, deliusUser, probationRegion, forService)
    }

    @ParameterizedTest
    @EnumSource(ServiceName::class)
    fun `it updates the user entity if the probation area code has been updated in delius`(forService: ServiceName) {
      val newProbationRegion = ProbationRegionEntityFactory()
        .withDefaults()
        .produce()

      val user = userFactory.produce()

      val deliusUser = staffUserDetailsFactory
        .withProbationAreaCode(newProbationRegion.deliusCode)
        .produce()

      assertUserUpdated(user, deliusUser, newProbationRegion, forService)
    }

    @ParameterizedTest
    @EnumSource(ServiceName::class)
    fun `it updates the user entity if the team codes have been updated in delius`(forService: ServiceName) {
      val user = userFactory.produce()

      val deliusUser = staffUserDetailsFactory
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory().withCode("new1").produce(),
            StaffUserTeamMembershipFactory().withCode("new2").produce(),
          ),
        )
        .produce()

      assertUserUpdated(user, deliusUser, probationRegion, forService)
    }

    @ParameterizedTest
    @EnumSource(ServiceName::class)
    fun `it updates the user entity if the probation delivery unit have been updated in delius`(forService: ServiceName) {
      val user = userFactory.produce()

      val deliusUser = staffUserDetailsFactory
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory().withBorough(
              KeyValue(
                code = randomStringMultiCaseWithNumbers(7),
                description = randomStringMultiCaseWithNumbers(10),
              ),
            ).produce(),
          ),
        )
        .produce()

      assertUserUpdated(user, deliusUser, probationRegion, forService)
    }

    private fun assertUserUpdated(
      user: UserEntity,
      deliusUser: StaffUserDetails,
      probationRegion: ProbationRegionEntity,
      forService: ServiceName,
      force: Boolean = false,
    ) {
      every { mockUserRepository.findByIdOrNull(id) } returns user
      every { mockCommunityApiClient.getStaffUserDetails(username) } returns ClientResult.Success(
        HttpStatus.OK,
        deliusUser,
      )

      every {
        mockProbationAreaProbationRegionMappingRepository.findByProbationAreaDeliusCode(probationRegion.deliusCode)
      } returns ProbationAreaProbationRegionMappingEntityFactory()
        .withProbationRegion(probationRegion)
        .withProbationAreaDeliusCode(probationRegion.deliusCode)
        .produce()

      var pduId: UUID? = null
      val userBoroughCode = deliusUser.teams?.maxByOrNull { it.startDate }?.borough?.code
      if (userBoroughCode != null) {
        pduId = UUID.fromString("99bc8c9c-ce26-4f0e-b994-a3b566c57b61")
        every {
          userBoroughCode.let { mockProbationDeliveryUnitRepository.findByDeliusCode(it) }
        } returns ProbationDeliveryUnitEntityFactory()
          .withId(pduId)
          .withDeliusCode(randomStringMultiCaseWithNumbers(8))
          .withProbationRegion(probationRegion)
          .produce()
      }

      val newApAreaForCas1 = ApAreaEntityFactory().produce()
      if (forService == ServiceName.approvedPremises) {
        every { mockCas1UserMappingService.determineApArea(probationRegion, deliusUser) } returns newApAreaForCas1
      }

      val result = userService.updateUserFromCommunityApiById(id, forService, force)

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      val entity = (result as AuthorisableActionResult.Success).entity

      assertThat(entity.id).isEqualTo(user.id)
      assertThat(entity.name).isEqualTo(deliusUser.staff.fullName)
      assertThat(entity.deliusUsername).isEqualTo(user.deliusUsername)
      assertThat(entity.email).isEqualTo(deliusUser.email)
      assertThat(entity.telephoneNumber).isEqualTo(deliusUser.telephoneNumber)
      assertThat(entity.deliusStaffCode).isEqualTo(deliusUser.staffCode)
      assertThat(entity.probationRegion.name).isEqualTo(probationRegion.name)
      assertThat(entity.probationDeliveryUnit?.id).isEqualTo(pduId)
      assertThat(entity.teamCodes ?: emptyList()).isEqualTo(deliusUser.getTeamCodes())

      if (forService == ServiceName.approvedPremises) {
        assertThat(entity.apArea).isEqualTo(newApAreaForCas1)
      } else {
        assertThat(entity.apArea).isNull()
      }

      verify(exactly = 1) { mockCommunityApiClient.getStaffUserDetails(username) }
      verify(exactly = 1) { mockUserRepository.save(any()) }
    }

    @Test
    fun `it stores a null email address if missing from Community API`() {
      val user = userFactory
        .withUnitTestControlProbationRegion()
        .produce()

      val deliusUser = staffUserDetailsFactory
        .withTelephoneNumber("0123456789")
        .withoutEmail()
        .withProbationAreaCode(user.probationRegion.deliusCode)
        .produce()

      every { mockUserRepository.findByIdOrNull(id) } returns user
      every { mockCommunityApiClient.getStaffUserDetails(username) } returns ClientResult.Success(
        HttpStatus.OK,
        deliusUser,
      )
      every { mockProbationAreaProbationRegionMappingRepository.findByProbationAreaDeliusCode(user.probationRegion.deliusCode) } returns ProbationAreaProbationRegionMappingEntityFactory()
        .withProbationRegion(user.probationRegion)
        .withProbationAreaDeliusCode(user.probationRegion.deliusCode)
        .produce()

      val result = userService.updateUserFromCommunityApiById(id, ServiceName.temporaryAccommodation)

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      result as AuthorisableActionResult.Success

      var entity = result.entity

      assertThat(entity.email).isEqualTo("null")

      verify(exactly = 1) { mockCommunityApiClient.getStaffUserDetails(username) }
      verify(exactly = 1) { mockUserRepository.save(any()) }
    }

    @Test
    fun `it returns not found when there is no user for that ID`() {
      every { mockUserRepository.findByIdOrNull(id) } returns null

      val result = userService.updateUserFromCommunityApiById(id, ServiceName.approvedPremises)

      assertThat(result).isInstanceOf(AuthorisableActionResult.NotFound::class.java)
    }
  }

  @Nested
  inner class UpdateUserRolesAndQualifications {

    private val userService = mockk<UserService>()

    private val userFactory = UserEntityFactory()
      .withDefaults()

    @Test
    fun `updates a user with given role`() {
      every { userService.updateUserRolesAndQualificationsForUser(any(), any(), any()) } answers { callOriginal() }
      val user = userFactory.produce()

      val assessorRole = ApprovedPremisesUserRole.assessor
      val assessorRoleAdmin = ApprovedPremisesUserRole.roleAdmin

      val qualificationWomens = APIUserQualification.womens
      val qualificationPipe = APIUserQualification.pipe

      every { userService.clearRolesForService(user, ServiceName.approvedPremises) } returns Unit
      every { userService.clearQualifications(user) } returns Unit
      every { userService.addRoleToUser(user, any()) } returns Unit
      every { userService.addQualificationToUser(user, any()) } returns Unit

      val roles = listOf(assessorRole, assessorRoleAdmin)
      val qualifications = listOf(qualificationWomens, qualificationPipe)

      val result = userService.updateUserRolesAndQualificationsForUser(user, roles, qualifications)

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      result as AuthorisableActionResult.Success

      val entity = result.entity

      assertThat(entity.id).isEqualTo(user.id)

      verify(exactly = 1) { userService.clearRolesForService(user, ServiceName.approvedPremises) }
      verify(exactly = 1) { userService.clearQualifications(user) }
      verify(exactly = 2) { userService.addRoleToUser(user, any()) }
      verify(exactly = 1) { userService.addRoleToUser(user, UserRole.CAS1_ASSESSOR) }
      verify(exactly = 1) { userService.addRoleToUser(user, UserRole.CAS1_ADMIN) }
      verify(exactly = 2) { userService.addQualificationToUser(user, any()) }
      verify(exactly = 1) { userService.addQualificationToUser(user, UserQualification.WOMENS) }
      verify(exactly = 1) { userService.addQualificationToUser(user, UserQualification.PIPE) }
    }
  }

  @Nested
  inner class SearchUsersOnAPI {
    private val userFactory = UserEntityFactory()
      .withDefaults()

    val user1 = userFactory
      .withName("Johnny Red")
      .produce()

    val user2 = userFactory
      .withName("Johnny Blue")
      .produce()

    @Test
    fun `User search service correctly returns values given by user repo`() {
      every { mockUserRepository.findByNameContainingIgnoreCase("Johnny") } returns listOf(user1, user2)
      every { mockUserRepository.findByNameContainingIgnoreCase("Bob") } returns listOf()
      every { mockUserRepository.findByNameContainingIgnoreCase("Blue") } returns listOf(user2)

      val result = userService.getUsersByPartialName("Johnny")

      assertThat(result.count()).isEqualTo(2)

      val entity1 = result.first()
      val entity2 = result.last()
      assertThat(entity1.name).isEqualTo(user1.name)
      assertThat(entity2.name).isEqualTo(user2.name)

      val resultFail = userService.getUsersByPartialName("Bob")
      assertThat(resultFail.count()).isEqualTo(0)

      val resultBlue = userService.getUsersByPartialName("Blue")
      assertThat(resultBlue.count()).isEqualTo(1)
      val entity = resultBlue.first()
      assertThat(entity.name).isEqualTo(user2.name)
    }
  }

  @Nested
  inner class DeleteUsersOnAPI {
    private val userFactory = UserEntityFactory()
      .withDefaults()

    @Test
    fun `deleted user now has isActive set to false`() {
      val id = UUID.randomUUID()

      val user = userFactory
        .withId(id)
        .produce()

      every { mockUserRepository.findByIdOrNull(id) } answers { user }
      every { mockUserRepository.save(any()) } answers { user }
      userService.deleteUser(id)

      verify(exactly = 1) {
        mockUserRepository.save(
          match {
            it.isActive == false && it.id == user.id
          },
        )
      }
    }
  }

  @Nested
  inner class getAllocatableUsersForAllocationType {

    @Test
    fun `getAllocatableUsersForAllocationType asserts lao qualification if offender is lao`() {
      every { mockOffenderService.isLao(any()) } returns true

      val userWithLao = UserEntityFactory()
        .withDefaultProbationRegion()
        .produce()

      userWithLao.apply {
        qualifications += UserQualificationAssignmentEntityFactory()
          .withUser(this)
          .withQualification(UserQualification.LAO)
          .produce()
      }

      val userWithoutLao = UserEntityFactory()
        .withDefaultProbationRegion()
        .produce()

      every { mockUserRepository.findActiveUsersWithAtLeastOneRole(any()) } returns listOf(userWithLao, userWithoutLao)
      every { mockFeatureFlagService.getBooleanFlag(any()) } returns true

      val allocatableUser = userService.getAllocatableUsersForAllocationType(
        "crn",
        emptyList<UserQualification>(),
        UserPermission.CAS1_ASSESS_PLACEMENT_APPLICATION,
      )
      assertThat(allocatableUser.size).isEqualTo(1)
      assertThat(allocatableUser.first().id).isEqualTo(userWithLao.id)
    }

    @Test
    fun `getAllocatableUsersForAllocationType seeks correct role when process appeal permission is present `() {
      every { mockOffenderService.isLao(any()) } returns false

      val user = UserEntityFactory()
        .withDefaultProbationRegion()
        .produce()

      every { mockUserRepository.findActiveUsersWithAtLeastOneRole(any()) } returns listOf(user)
      every { mockFeatureFlagService.getBooleanFlag(any()) } returns true

      val allocatableUser = userService.getAllocatableUsersForAllocationType(
        "crn",
        emptyList<UserQualification>(),
        UserPermission.CAS1_ASSESS_APPEALED_APPLICATION,
      )

      verify(exactly = 1) { mockUserRepository.findActiveUsersWithAtLeastOneRole(listOf(UserRole.CAS1_ASSESSOR, UserRole.CAS1_APPEALS_MANAGER)) }
    }
  }
}
