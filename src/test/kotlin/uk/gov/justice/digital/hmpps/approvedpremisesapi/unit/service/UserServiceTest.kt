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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationAreaProbationRegionMappingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addQualificationForUnitTest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addRoleForUnitTest
import java.util.UUID

class UserServiceTest {
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
  fun `getUserForAssessmentAllocation adds LAO qualification to requirements when application is for an LAO CRN`() {
    val createdByUser = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val userForAllocation = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .addQualificationForUnitTest(UserQualification.LAO)

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(createdByUser)
      .produce()

    every { mockOffenderService.isLao(application.crn) } returns true

    every {
      mockUserRepository.findQualifiedAssessorWithLeastPendingOrCompletedInLastWeekAssessments(
        requiredQualifications = listOf(UserQualification.LAO.toString()),
        totalRequiredQualifications = 1,
        excludedUserIds = any(),
      )
    } returns userForAllocation

    assertThat(userService.getUserForAssessmentAllocation(application)).isEqualTo(userForAllocation)
  }

  @Test
  fun `getUserForAssessmentAllocation does not select user with qualifications when none are required to assess the application`() {
    val createdByUser = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val userWithQualifications = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .addQualificationForUnitTest(UserQualification.LAO)

    val userWithoutQualifications = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(createdByUser)
      .produce()

    every { mockOffenderService.isLao(application.crn) } returns false

    every {
      mockUserRepository.findQualifiedAssessorWithLeastPendingOrCompletedInLastWeekAssessments(
        requiredQualifications = emptyList(),
        totalRequiredQualifications = 0,
        excludedUserIds = any(),
      )
    } returns userWithQualifications andThen userWithoutQualifications

    assertThat(userService.getUserForAssessmentAllocation(application)).isEqualTo(userWithoutQualifications)
  }

  @Test
  fun `getUserForAssessmentAllocation does not select user with CAS1_EXCLUDED_FROM_ASSESS_ALLOCATION role`() {
    val createdByUser = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val userWithExclusionRole = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .addRoleForUnitTest(UserRole.CAS1_EXCLUDED_FROM_ASSESS_ALLOCATION)

    val userWithoutExclusionRole = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(createdByUser)
      .produce()

    every { mockOffenderService.isLao(application.crn) } returns false

    every {
      mockUserRepository.findQualifiedAssessorWithLeastPendingOrCompletedInLastWeekAssessments(
        requiredQualifications = emptyList(),
        totalRequiredQualifications = 0,
        excludedUserIds = any(),
      )
    } returns userWithExclusionRole andThen userWithoutExclusionRole

    assertThat(userService.getUserForAssessmentAllocation(application)).isEqualTo(userWithoutExclusionRole)
  }

  @Test
  fun `getUserForPlacementRequestAllocation adds LAO qualification to requirements when application is for an LAO CRN`() {
    val userForAllocation = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .addQualificationForUnitTest(UserQualification.LAO)

    val crn = "CRN123"

    every { mockOffenderService.isLao(crn) } returns true

    every {
      mockUserRepository.findQualifiedMatcherWithLeastPendingOrCompletedInLastWeekPlacementRequests(
        requiredQualifications = listOf(UserQualification.LAO.toString()),
        totalRequiredQualifications = 1,
        excludedUserIds = any(),
      )
    } returns userForAllocation

    assertThat(userService.getUserForPlacementRequestAllocation(crn)).isEqualTo(userForAllocation)
  }

  @Test
  fun `getUserForPlacementRequestAllocation does not select user with CAS1_EXCLUDED_FROM_MATCH_ALLOCATION role`() {
    val userWithExclusionRole = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .addRoleForUnitTest(UserRole.CAS1_EXCLUDED_FROM_MATCH_ALLOCATION)

    val userWithoutExclusionRole = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val crn = "CRN123"

    every { mockOffenderService.isLao(crn) } returns false

    every {
      mockUserRepository.findQualifiedMatcherWithLeastPendingOrCompletedInLastWeekPlacementRequests(
        requiredQualifications = emptyList(),
        totalRequiredQualifications = 0,
        excludedUserIds = any(),
      )
    } returns userWithExclusionRole andThen userWithoutExclusionRole

    assertThat(userService.getUserForPlacementRequestAllocation(crn)).isEqualTo(userWithoutExclusionRole)
  }

  @Test
  fun `getUserForPlacementApplicationAllocation adds LAO qualification to requirements when application is for an LAO CRN`() {
    val userForAllocation = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .addQualificationForUnitTest(UserQualification.LAO)

    val crn = "CRN123"

    every { mockOffenderService.isLao(crn) } returns true

    every {
      mockUserRepository.findQualifiedMatcherWithLeastPendingOrCompletedInLastWeekPlacementApplications(
        requiredQualifications = listOf(UserQualification.LAO.toString()),
        totalRequiredQualifications = 1,
        excludedUserIds = any(),
      )
    } returns userForAllocation

    assertThat(userService.getUserForPlacementApplicationAllocation(crn)).isEqualTo(userForAllocation)
  }

  @Test
  fun `getUserForPlacementApplicationAllocation does not select user with CAS1_EXCLUDED_FROM_MATCH_ALLOCATION role`() {
    val userWithExclusionRole = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
      .addRoleForUnitTest(UserRole.CAS1_EXCLUDED_FROM_PLACEMENT_APPLICATION_ALLOCATION)

    val userWithoutExclusionRole = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val crn = "CRN123"

    every { mockOffenderService.isLao(crn) } returns false

    every {
      mockUserRepository.findQualifiedMatcherWithLeastPendingOrCompletedInLastWeekPlacementApplications(
        requiredQualifications = emptyList(),
        totalRequiredQualifications = 0,
        excludedUserIds = any(),
      )
    } returns userWithExclusionRole andThen userWithoutExclusionRole

    assertThat(userService.getUserForPlacementApplicationAllocation(crn)).isEqualTo(userWithoutExclusionRole)
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
  class UpdateUserFromCommunityApiById {
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
      mockHttpAuthService,
      mockOffenderService,
      mockCommunityApiClient,
      mockUserRepository,
      mockUserRoleAssignmentRepository,
      mockUserQualificationAssignmentRepository,
      mockProbationRegionRepository,
      mockProbationAreaProbationRegionMappingRepository,
    )

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
    fun `it returns the user's details from the Community API and saves the email address, telephone number and staff code`() {
      val user = userFactory.produce()
      val deliusUser = staffUserDetailsFactory
        .withEmail("foo@example.com")
        .withTelephoneNumber("0123456789")
        .withStaffCode("STAFF1")
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
      assertThat(entity.name).isEqualTo("$forename $surname")
      assertThat(entity.deliusUsername).isEqualTo(user.deliusUsername)
      assertThat(entity.email).isEqualTo(deliusUser.email)
      assertThat(entity.telephoneNumber).isEqualTo(deliusUser.telephoneNumber)
      assertThat(entity.deliusStaffCode).isEqualTo(deliusUser.staffCode)

      verify(exactly = 1) { mockCommunityApiClient.getStaffUserDetails(username) }
      verify(exactly = 1) { mockUserRepository.save(any()) }
    }

    @Test
    fun `it stores a null email address if missing from Community API`() {
      val user = userFactory.produce()
      val deliusUser = staffUserDetailsFactory
        .withTelephoneNumber("0123456789")
        .withoutEmail()
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
      assertThat(entity.name).isEqualTo("$forename $surname")
      assertThat(entity.deliusUsername).isEqualTo(user.deliusUsername)
      assertThat(entity.email).isEqualTo("null")
      assertThat(entity.telephoneNumber).isEqualTo(deliusUser.telephoneNumber)

      verify(exactly = 1) { mockCommunityApiClient.getStaffUserDetails(username) }
      verify(exactly = 1) { mockUserRepository.save(any()) }
    }

    @Test
    fun `it does not save the object if the email, telephone number and staff code are the same as Delius`() {
      val email = "foo@example.com"
      val telephoneNumber = "0123456789"
      val staffCode = "STAFF1"

      val user = userFactory
        .withName("$forename $surname")
        .withEmail(email)
        .withTelephoneNumber(telephoneNumber)
        .withDeliusStaffCode(staffCode)
        .produce()

      val deliusUser = staffUserDetailsFactory
        .withForenames(forename)
        .withSurname(surname)
        .withEmail(email)
        .withTelephoneNumber(telephoneNumber)
        .withStaffCode(staffCode)
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
}
