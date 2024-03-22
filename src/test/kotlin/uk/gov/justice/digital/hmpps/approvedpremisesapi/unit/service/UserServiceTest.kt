package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationAreaProbationRegionMappingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationAreaProbationRegionMappingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.util.UUID
import javax.servlet.http.HttpServletRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as APIUserQualification

class UserServiceTest {
  private val mockCurrentRequest = mockk<HttpServletRequest>()
  private val mockHttpAuthService = mockk<HttpAuthService>()
  private val mockOffenderService = mockk<OffenderService>()
  private val mockCommunityApiClient = mockk<CommunityApiClient>()
  private val mockUserRepository = mockk<UserRepository>()
  private val mockUserRoleAssignmentRepository = mockk<UserRoleAssignmentRepository>()
  private val mockUserQualificationAssignmentRepository = mockk<UserQualificationAssignmentRepository>()
  private val mockProbationRegionRepository = mockk<ProbationRegionRepository>()
  private val mockProbationAreaProbationRegionMappingRepository = mockk<ProbationAreaProbationRegionMappingRepository>()

  private val userService = UserService(
    false,
    mockCurrentRequest,
    mockHttpAuthService,
    mockOffenderService,
    mockCommunityApiClient,
    mockUserRepository,
    mockUserRoleAssignmentRepository,
    mockUserQualificationAssignmentRepository,
    mockProbationRegionRepository,
    mockProbationAreaProbationRegionMappingRepository,
  )

  @Test
  fun `getUserForRequest returns existing User when exists, does not call Community API or save`() {
    val username = "SOMEPERSON"
    val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

    every { mockCurrentRequest.getHeader("X-Service-Name") } returns "approved-premises"

    every { mockHttpAuthService.getDeliusPrincipalOrThrow() } returns mockPrincipal
    every { mockPrincipal.name } returns username

    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()

    every { mockUserRepository.findByDeliusUsername(username) } returns user

    assertThat(userService.getUserForRequest()).isEqualTo(user)

    verify(exactly = 0) { mockCommunityApiClient.getStaffUserDetails(username) }
    verify(exactly = 0) { mockUserRepository.save(any()) }
  }

  @Test
  fun `getUserForRequest returns new User when one does not already exist, does call Community API and save`() {
    val username = "SOMEPERSON"
    val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

    every { mockCurrentRequest.getHeader("X-Service-Name") } returns "approved-premises"

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
        .withProbationAreaCode("AREACODE")
        .produce(),
    )

    every { mockProbationAreaProbationRegionMappingRepository.findByProbationAreaDeliusCode("AREACODE") } returns ProbationAreaProbationRegionMappingEntityFactory()
      .withProbationRegion(
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce(),
      )
      .withProbationAreaDeliusCode("AREACODE")
      .produce()

    assertThat(userService.getUserForRequest()).matches {
      it.name == "Jim Jimmerson"
    }

    verify(exactly = 1) { mockCommunityApiClient.getStaffUserDetails(username) }
    verify(exactly = 1) { mockUserRepository.save(any()) }
    verify(exactly = 1) { mockProbationAreaProbationRegionMappingRepository.findByProbationAreaDeliusCode(any()) }
  }

  @Test
  fun `getUserForRequest assigns a default role of CAS3_REFERRER for Temporary Accommodation if the user has no Temporary Accommodation roles`() {
    val username = "SOMEPERSON"
    val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

    every { mockCurrentRequest.getHeader("X-Service-Name") } returns "temporary-accommodation"

    every { mockHttpAuthService.getDeliusPrincipalOrThrow() } returns mockPrincipal
    every { mockPrincipal.name } returns username

    val expectedUser = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
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
  fun `getUserForRequest does not assign a default role for Temporary Accommodation if the user already has a Temporary Accommodation role`() {
    val username = "SOMEPERSON"
    val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

    every { mockCurrentRequest.getHeader("X-Service-Name") } returns "temporary-accommodation"

    every { mockHttpAuthService.getDeliusPrincipalOrThrow() } returns mockPrincipal
    every { mockPrincipal.name } returns username

    val expectedUser = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
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

  @Test
  fun `getUserForRequest does not assign a default role for Approved Premises`() {
    val username = "SOMEPERSON"
    val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

    every { mockCurrentRequest.getHeader("X-Service-Name") } returns "approved-premises"

    every { mockHttpAuthService.getDeliusPrincipalOrThrow() } returns mockPrincipal
    every { mockPrincipal.name } returns username

    val expectedUser = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
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

  @Test
  fun `getUserForRequestOrNull returns User when exists, does not call Community API or save`() {
    val username = "SOMEPERSON"
    val mockPrincipal = mockk<AuthAwareAuthenticationToken>()

    every { mockHttpAuthService.getDeliusPrincipalOrNull() } returns mockPrincipal
    every { mockPrincipal.name } returns username

    val user = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
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

  @Nested
  inner class UpdateUserFromCommunityApiById {

    private val id = UUID.fromString("21b61d19-3a96-4b88-8df9-a5e89bc6fe73")
    private val username = "SOMEPERSON"
    private val forename = "Jim"
    private val surname = "Jimmerson"
    private val staffIdentifier = 5678

    private val userFactory = UserEntityFactory()
      .withDeliusUsername(username)
      .withName("$forename $surname")
      .withDeliusStaffIdentifier(staffIdentifier.toLong())
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }

    private val staffUserDetailsFactory = StaffUserDetailsFactory()
      .withUsername(username)
      .withForenames(forename)
      .withSurname(surname)
      .withStaffIdentifier(staffIdentifier.toLong())

    @BeforeEach
    fun setup() {
      every { mockUserRepository.save(any()) } answers { it.invocation.args[0] as UserEntity }
    }

    @Test
    fun `it returns the user's details from the Community API and saves the email address, telephone, staff code and probation region`() {
      val newProbationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()

      val user = userFactory.produce()

      val deliusUser = staffUserDetailsFactory
        .withEmail("foo@example.com")
        .withTelephoneNumber("0123456789")
        .withStaffCode("STAFF1")
        .withProbationAreaCode(newProbationRegion.deliusCode)
        .produce()

      every { mockUserRepository.findByIdOrNull(id) } returns user
      every { mockCommunityApiClient.getStaffUserDetails(username) } returns ClientResult.Success(
        HttpStatus.OK,
        deliusUser,
      )
      every { mockProbationAreaProbationRegionMappingRepository.findByProbationAreaDeliusCode(newProbationRegion.deliusCode) } returns ProbationAreaProbationRegionMappingEntityFactory()
        .withProbationRegion(newProbationRegion)
        .withProbationAreaDeliusCode(newProbationRegion.deliusCode)
        .produce()

      val result = userService.updateUserFromCommunityApiById(id)

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      result as AuthorisableActionResult.Success

      var entity = result.entity

      assertThat(entity.id).isEqualTo(user.id)
      assertThat(entity.name).isEqualTo("$forename $surname")
      assertThat(entity.deliusUsername).isEqualTo(user.deliusUsername)
      assertThat(entity.email).isEqualTo(deliusUser.email)
      assertThat(entity.telephoneNumber).isEqualTo(deliusUser.telephoneNumber)
      assertThat(entity.deliusStaffCode).isEqualTo(deliusUser.staffCode)
      assertThat(entity.probationRegion.name).isEqualTo(newProbationRegion.name)

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

      val result = userService.updateUserFromCommunityApiById(id)

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      result as AuthorisableActionResult.Success

      var entity = result.entity

      assertThat(entity.email).isEqualTo("null")

      verify(exactly = 1) { mockCommunityApiClient.getStaffUserDetails(username) }
      verify(exactly = 1) { mockUserRepository.save(any()) }
    }

    @Test
    fun `it does not save the object if the email, telephone number, staff code and probation region are the same as Delius`() {
      val email = "foo@example.com"
      val telephoneNumber = "0123456789"
      val staffCode = "STAFF1"

      val user = userFactory
        .withName("$forename $surname")
        .withEmail(email)
        .withTelephoneNumber(telephoneNumber)
        .withDeliusStaffCode(staffCode)
        .withUnitTestControlProbationRegion()
        .produce()

      val deliusUser = staffUserDetailsFactory
        .withForenames(forename)
        .withSurname(surname)
        .withEmail(email)
        .withTelephoneNumber(telephoneNumber)
        .withStaffCode(staffCode)
        .withProbationAreaCode(user.probationRegion.deliusCode)
        .produce()

      every { mockUserRepository.findByIdOrNull(id) } returns user
      every { mockCommunityApiClient.getStaffUserDetails(username) } returns ClientResult.Success(
        HttpStatus.OK,
        deliusUser,
      )

      val result = userService.updateUserFromCommunityApiById(id)

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      result as AuthorisableActionResult.Success

      var entity = result.entity

      assertThat(entity.id).isEqualTo(user.id)
      assertThat(entity.name).isEqualTo(user.name)

      verify(exactly = 0) { mockUserRepository.save(any()) }
    }

    @Test
    fun `it returns not found when there is no user for that ID`() {
      every { mockUserRepository.findByIdOrNull(id) } returns null

      val result = userService.updateUserFromCommunityApiById(id)

      assertThat(result).isInstanceOf(AuthorisableActionResult.NotFound::class.java)
    }
  }

  @Nested
  inner class UpdateUserRolesAndQualificationsFromApiById {

    private val userService = mockk<UserService>()

    private val userFactory = UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }

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
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }

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
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }

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
}
