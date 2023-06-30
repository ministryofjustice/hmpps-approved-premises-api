package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApplicationTeamCodeEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserTeamMembershipFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApprovedPremisesApplicationAccessLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.TemporaryAccommodationApplicationAccessLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addRoleForUnitTest
import java.time.OffsetDateTime
import java.util.UUID
import javax.servlet.http.HttpServletRequest

class UserAccessServiceTest {
  private val userService = mockk<UserService>()
  private val offenderService = mockk<OffenderService>()
  private val currentRequest = mockk<HttpServletRequest>()
  private val communityApiClient = mockk<CommunityApiClient>()

  private val userAccessService = UserAccessService(
    userService,
    offenderService,
    currentRequest,
    communityApiClient,
  )

  private val probationRegionId = UUID.randomUUID()
  private val probationRegion = ProbationRegionEntityFactory()
    .withId(probationRegionId)
    .withApArea(
      ApAreaEntityFactory()
        .produce(),
    )
    .produce()

  private val anotherProbationRegion = ProbationRegionEntityFactory()
    .withApArea(
      ApAreaEntityFactory()
        .produce(),
    )
    .produce()

  private val user = UserEntityFactory()
    .withProbationRegion(probationRegion)
    .produce()

  private val anotherUserInRegion = UserEntityFactory()
    .withProbationRegion(probationRegion)
    .produce()

  private val anotherUserNotInRegion = UserEntityFactory()
    .withProbationRegion(anotherProbationRegion)
    .produce()

  val approvedPremises = ApprovedPremisesEntityFactory()
    .withProbationRegion(probationRegion)
    .withLocalAuthorityArea(
      LocalAuthorityEntityFactory()
        .produce(),
    )
    .produce()

  val temporaryAccommodationPremisesInUserRegion = TemporaryAccommodationPremisesEntityFactory()
    .withProbationRegion(probationRegion)
    .withLocalAuthorityArea(
      LocalAuthorityEntityFactory()
        .produce(),
    )
    .produce()

  val temporaryAccommodationPremisesNotInUserRegion = TemporaryAccommodationPremisesEntityFactory()
    .withProbationRegion(anotherProbationRegion)
    .withLocalAuthorityArea(
      LocalAuthorityEntityFactory()
        .produce(),
    )
    .produce()

  private fun currentRequestIsFor(service: ServiceName) {
    every { currentRequest.getHeader("X-Service-Name") } returns service.value
  }

  private fun currentRequestIsForArbitraryService() {
    every { currentRequest.getHeader("X-Service-Name") } returns "arbitrary-value"
  }

  private fun communityApiStaffUserDetailsReturnsTeamCodes(vararg teamCodes: String) {
    every { communityApiClient.getStaffUserDetails(any()) } returns ClientResult.Success(
      HttpStatus.OK,
      StaffUserDetailsFactory()
        .withTeams(
          teamCodes.map {
            StaffUserTeamMembershipFactory()
              .withCode(it)
              .produce()
          },
        )
        .produce(),
    )
  }

  @BeforeEach
  fun setup() {
    every { userService.getUserForRequest() } returns user
  }

  @Test
  fun `userHasAllRegionsAccess returns false if the current request has 'X-Service-Name' header with value 'temporary-accommodation'`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userHasAllRegionsAccess(user)).isFalse
  }

  @Test
  fun `userHasAllRegionsAccess returns true if the current request has 'X-Service-Name' header with value 'approved-premises'`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.userHasAllRegionsAccess(user)).isTrue
  }

  @Test
  fun `userHasAllRegionsAccess returns true by default`() {
    currentRequestIsForArbitraryService()

    assertThat(userAccessService.userHasAllRegionsAccess(user)).isTrue
  }

  @Test
  fun `currentUserHasAllRegionsAccess returns false if the current request has 'X-Service-Name' header with value 'temporary-accommodation'`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserHasAllRegionsAccess()).isFalse
  }

  @Test
  fun `currentUserHasAllRegionsAccess returns true if the current request has 'X-Service-Name' header with value 'approved-premises'`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.currentUserHasAllRegionsAccess()).isTrue
  }

  @Test
  fun `currentUserHasAllRegionsAccess returns true by default`() {
    currentRequestIsForArbitraryService()

    assertThat(userAccessService.currentUserHasAllRegionsAccess()).isTrue
  }

  @Test
  fun `userCanAccessRegion returns false if the current user does not have all regions access and their probation region ID does not equal the specified ID`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanAccessRegion(user, UUID.randomUUID())).isFalse
  }

  @Test
  fun `userCanAccessRegion returns true if the current user has all regions access`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.userCanAccessRegion(user, UUID.randomUUID())).isTrue
  }

  @Test
  fun `userCanAccessRegion returns true if the current user's probation region ID is equal to the specified ID`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanAccessRegion(user, probationRegionId)).isTrue
  }

  @Test
  fun `currentUserCanAccessRegion returns false if the current user does not have all regions access and their probation region ID does not equal the specified ID`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserCanAccessRegion(UUID.randomUUID())).isFalse
  }

  @Test
  fun `currentUserCanAccessRegion returns true if the current user has all regions access`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.currentUserCanAccessRegion(UUID.randomUUID())).isTrue
  }

  @Test
  fun `currentUserCanAccessRegion returns true if the current user's probation region ID is equal to the specified ID`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserCanAccessRegion(probationRegionId)).isTrue
  }

  @Test
  fun `userCanViewPremises returns true if the given premises is an Approved Premises`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.userCanViewPremises(user, approvedPremises)).isTrue
  }

  @Test
  fun `userCanViewPremises returns true if the given premises is a Temporary Accommodation premises and the user can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanViewPremises(user, temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `userCanViewPremises returns false if the given premises is a Temporary Accommodation premises and the user cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanViewPremises(user, temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @Test
  fun `currentUserCanViewPremises returns true if the given premises is an Approved Premises`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.currentUserCanViewPremises(approvedPremises)).isTrue
  }

  @Test
  fun `currentUserCanViewPremises returns true if the given premises is a Temporary Accommodation premises and the current user can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanViewPremises(user, temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `currentUserCanViewPremises returns false if the given premises is a Temporary Accommodation premises and the current user cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanViewPremises(user, temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @Test
  fun `userCanManagePremises returns true if the given premises is an Approved Premises`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.userCanManagePremises(user, approvedPremises)).isTrue
  }

  @Test
  fun `userCanManagePremises returns true if the given premises is a Temporary Accommodation premises and the user can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanManagePremises(user, temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `userCanManagePremises returns false if the given premises is a Temporary Accommodation premises and the user cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanManagePremises(user, temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @Test
  fun `currentUserCanManagePremises returns true if the given premises is an Approved Premises`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.currentUserCanManagePremises(approvedPremises)).isTrue
  }

  @Test
  fun `currentUserCanManagePremises returns true if the given premises is a Temporary Accommodation premises and the current user can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserCanManagePremises(temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `currentUserCanManagePremises returns false if the given premises is a Temporary Accommodation premises and the current user cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserCanManagePremises(temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "CAS1_MANAGER", "CAS1_MATCHER" ])
  fun `userCanManagePremisesBookings returns true if the given premises is an Approved Premises and the user has either the MANAGER or MATCHER user role`(role: UserRole) {
    currentRequestIsFor(ServiceName.approvedPremises)

    user.addRoleForUnitTest(role)

    assertThat(userAccessService.userCanManagePremisesBookings(user, approvedPremises)).isTrue
  }

  @Test
  fun `userCanManagePremisesBookings returns false if the given premises is an Approved Premises and the user has no suitable role`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.userCanManagePremisesBookings(user, approvedPremises)).isFalse
  }

  @Test
  fun `userCanManagePremisesBookings returns true if the given premises is a Temporary Accommodation premises and the user can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanManagePremisesBookings(user, temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `userCanManagePremisesBookings returns false if the given premises is a Temporary Accommodation premises and the user cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanManagePremisesBookings(user, temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "CAS1_MANAGER", "CAS1_MATCHER" ])
  fun `currentUserCanManagePremisesBookings returns true if the given premises is an Approved Premises and the current user has either the MANAGER or MATCHER user role`(role: UserRole) {
    currentRequestIsFor(ServiceName.approvedPremises)

    user.addRoleForUnitTest(role)

    assertThat(userAccessService.currentUserCanManagePremisesBookings(approvedPremises)).isTrue
  }

  @Test
  fun `currentUserCanManagePremisesBookings returns false if the given premises is an Approved Premises and the current user has no suitable role`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.currentUserCanManagePremisesBookings(approvedPremises)).isFalse
  }

  @Test
  fun `currentUserCanManagePremisesBookings returns true if the given premises is a Temporary Accommodation premises and the current user can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserCanManagePremisesBookings(temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `currentUserCanManagePremisesBookings returns false if the given premises is a Temporary Accommodation premises and the current user cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserCanManagePremisesBookings(temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "CAS1_MANAGER", "CAS1_MATCHER" ])
  fun `userCanManagePremisesLostBeds returns true if the given premises is an Approved Premises and the user has either the MANAGER or MATCHER user role`(role: UserRole) {
    currentRequestIsFor(ServiceName.approvedPremises)

    user.addRoleForUnitTest(role)

    assertThat(userAccessService.userCanManagePremisesLostBeds(user, approvedPremises)).isTrue
  }

  @Test
  fun `userCanManagePremisesLostBeds returns false if the given premises is an Approved Premises and the user has no suitable role`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.userCanManagePremisesLostBeds(user, approvedPremises)).isFalse
  }

  @Test
  fun `userCanManagePremisesLostBeds returns true if the given premises is a Temporary Accommodation premises and the user can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanManagePremisesLostBeds(user, temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `userCanManagePremisesLostBeds returns false if the given premises is a Temporary Accommodation premises and the user cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanManagePremisesLostBeds(user, temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "CAS1_MANAGER", "CAS1_MATCHER" ])
  fun `currentUserCanManagePremisesLostBeds returns true if the given premises is an Approved Premises and the current user has either the MANAGER or MATCHER user role`(role: UserRole) {
    currentRequestIsFor(ServiceName.approvedPremises)

    user.addRoleForUnitTest(role)

    assertThat(userAccessService.currentUserCanManagePremisesLostBeds(approvedPremises)).isTrue
  }

  @Test
  fun `currentUserCanManagePremisesLostBeds returns false if the given premises is an Approved Premises and the current user has no suitable role`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.currentUserCanManagePremisesLostBeds(approvedPremises)).isFalse
  }

  @Test
  fun `currentUserCanManagePremisesLostBeds returns true if the given premises is a Temporary Accommodation premises and the current user can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserCanManagePremisesLostBeds(temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `currentUserCanManagePremisesLostBeds returns false if the given premises is a Temporary Accommodation premises and the current user cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserCanManagePremisesLostBeds(temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "CAS1_MANAGER", "CAS1_MATCHER" ])
  fun `userCanViewPremisesCapacity returns true if the given premises is an Approved Premises and the user has either the MANAGER or MATCHER user role`(role: UserRole) {
    currentRequestIsFor(ServiceName.approvedPremises)

    user.addRoleForUnitTest(role)

    assertThat(userAccessService.userCanViewPremisesCapacity(user, approvedPremises)).isTrue
  }

  @Test
  fun `userCanViewPremisesCapacity returns false if the given premises is an Approved Premises and the user has no suitable role`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.userCanViewPremisesCapacity(user, approvedPremises)).isFalse
  }

  @Test
  fun `userCanViewPremisesCapacity returns true if the given premises is a Temporary Accommodation premises and the user can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanViewPremisesCapacity(user, temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `userCanViewPremisesCapacity returns false if the given premises is a Temporary Accommodation premises and the user cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanViewPremisesCapacity(user, temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "CAS1_MANAGER", "CAS1_MATCHER" ])
  fun `currentUserCanViewPremisesCapacity returns true if the given premises is an Approved Premises and the current user has either the MANAGER or MATCHER user role`(role: UserRole) {
    currentRequestIsFor(ServiceName.approvedPremises)

    user.addRoleForUnitTest(role)

    assertThat(userAccessService.currentUserCanViewPremisesCapacity(approvedPremises)).isTrue
  }

  @Test
  fun `currentUserCanViewPremisesCapacity returns false if the given premises is an Approved Premises and the current user has no suitable role`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.currentUserCanViewPremisesCapacity(approvedPremises)).isFalse
  }

  @Test
  fun `currentUserCanViewPremisesCapacity returns true if the given premises is a Temporary Accommodation premises and the current user can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserCanViewPremisesCapacity(temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `currentUserCanViewPremisesCapacity returns false if the given premises is a Temporary Accommodation premises and the current user cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserCanViewPremisesCapacity(temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "CAS1_MANAGER", "CAS1_MATCHER" ])
  fun `userCanViewPremisesStaff returns true if the given premises is an Approved Premises and the user has either the MANAGER or MATCHER user role`(role: UserRole) {
    currentRequestIsFor(ServiceName.approvedPremises)

    user.addRoleForUnitTest(role)

    assertThat(userAccessService.userCanViewPremisesStaff(user, approvedPremises)).isTrue
  }

  @Test
  fun `userCanViewPremisesStaff returns false if the given premises is an Approved Premises and the user has no suitable role`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.userCanViewPremisesStaff(user, approvedPremises)).isFalse
  }

  @Test
  fun `userCanViewPremisesStaff returns true if the given premises is a Temporary Accommodation premises and the user can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanViewPremisesStaff(user, temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `userCanViewPremisesStaff returns false if the given premises is a Temporary Accommodation premises and the user cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanViewPremisesStaff(user, temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "CAS1_MANAGER", "CAS1_MATCHER" ])
  fun `currentUserCanViewPremisesStaff returns true if the given premises is an Approved Premises and the current user has either the MANAGER or MATCHER user role`(role: UserRole) {
    currentRequestIsFor(ServiceName.approvedPremises)

    user.addRoleForUnitTest(role)

    assertThat(userAccessService.currentUserCanViewPremisesStaff(approvedPremises)).isTrue
  }

  @Test
  fun `currentUserCanViewPremisesStaff returns false if the given premises is an Approved Premises and the current user has no suitable role`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.currentUserCanViewPremisesStaff(approvedPremises)).isFalse
  }

  @Test
  fun `currentUserCanViewPremisesStaff returns true if the given premises is a Temporary Accommodation premises and the current user can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserCanViewPremisesStaff(temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `currentUserCanViewPremisesStaff returns false if the given premises is a Temporary Accommodation premises and the current user cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserCanViewPremisesStaff(temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_ASSESSOR", "CAS1_MATCHER", "CAS1_MANAGER"])
  fun `getApprovedPremisesApplicationAccessLevelForUser returns ALL if the user has one of the CAS1_WORKFLOW_MANAGER, CAS1_ASSESSOR, CAS1_MATCHER, or CAS1_MANAGER roles`(role: UserRole) {
    user.addRoleForUnitTest(role)

    assertThat(userAccessService.getApprovedPremisesApplicationAccessLevelForUser(user)).isEqualTo(ApprovedPremisesApplicationAccessLevel.ALL)
  }

  @Test
  fun `getApprovedPremisesApplicationAccessLevelForUser returns TEAM if the user has no suitable role`() {
    assertThat(userAccessService.getApprovedPremisesApplicationAccessLevelForUser(user)).isEqualTo(ApprovedPremisesApplicationAccessLevel.TEAM)
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_ASSESSOR", "CAS1_MATCHER", "CAS1_MANAGER"])
  fun `getApprovedPremisesApplicationAccessLevelForCurrentUser returns ALL if the user has one of the CAS1_WORKFLOW_MANAGER, CAS1_ASSESSOR, CAS1_MATCHER, or CAS1_MANAGER roles`(role: UserRole) {
    user.addRoleForUnitTest(role)

    assertThat(userAccessService.getApprovedPremisesApplicationAccessLevelForCurrentUser()).isEqualTo(ApprovedPremisesApplicationAccessLevel.ALL)
  }

  @Test
  fun `getApprovedPremisesApplicationAccessLevelForCurrentUser returns TEAM if the user has no suitable role`() {
    assertThat(userAccessService.getApprovedPremisesApplicationAccessLevelForCurrentUser()).isEqualTo(ApprovedPremisesApplicationAccessLevel.TEAM)
  }

  @Test
  fun `getTemporaryAccommodationApplicationAccessLevelForUser returns SUBMITTED_IN_REGION if the user has the CAS3_ASSESSOR role`() {
    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.getTemporaryAccommodationApplicationAccessLevelForUser(user)).isEqualTo(TemporaryAccommodationApplicationAccessLevel.SUBMITTED_IN_REGION)
  }

  @Test
  fun `getTemporaryAccommodationApplicationAccessLevelForUser returns SELF if the user has the CAS3_REFERRER role`() {
    user.addRoleForUnitTest(UserRole.CAS3_REFERRER)

    assertThat(userAccessService.getTemporaryAccommodationApplicationAccessLevelForUser(user)).isEqualTo(TemporaryAccommodationApplicationAccessLevel.SELF)
  }

  @Test
  fun `getTemporaryAccommodationApplicationAccessLevelForUser returns NONE if the user has no suitable role`() {
    assertThat(userAccessService.getTemporaryAccommodationApplicationAccessLevelForUser(user)).isEqualTo(TemporaryAccommodationApplicationAccessLevel.NONE)
  }

  @Test
  fun `getTemporaryAccommodationApplicationAccessLevelForCurrentUser returns SUBMITTED_IN_REGION if the user has the CAS3_ASSESSOR role`() {
    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.getTemporaryAccommodationApplicationAccessLevelForCurrentUser()).isEqualTo(TemporaryAccommodationApplicationAccessLevel.SUBMITTED_IN_REGION)
  }

  @Test
  fun `getTemporaryAccommodationApplicationAccessLevelForCurrentUser returns SELF if the user has the CAS3_REFERRER role`() {
    user.addRoleForUnitTest(UserRole.CAS3_REFERRER)

    assertThat(userAccessService.getTemporaryAccommodationApplicationAccessLevelForCurrentUser()).isEqualTo(TemporaryAccommodationApplicationAccessLevel.SELF)
  }

  @Test
  fun `getTemporaryAccommodationApplicationAccessLevelForCurrentUser returns NONE if the user has no suitable role`() {
    assertThat(userAccessService.getTemporaryAccommodationApplicationAccessLevelForCurrentUser()).isEqualTo(TemporaryAccommodationApplicationAccessLevel.NONE)
  }

  @Test
  fun `userCanViewApplication returns true if the user created the application for Approved Premises`() {
    val newestJsonSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory()
      .withSchema("{}")
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withApplicationSchema(newestJsonSchema)
      .produce()

    assertThat(userAccessService.userCanViewApplication(user, application)).isTrue
  }

  @Test
  fun `userCanViewApplication returns true if the application is an Approved Premises application and the user can access the Offender (LAO)`() {
    val newestJsonSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory()
      .withSchema("{}")
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(anotherUserInRegion)
      .withApplicationSchema(newestJsonSchema)
      .withTeamCodes(mutableListOf())
      .produce()

    application.teamCodes.add(
      ApplicationTeamCodeEntityFactory()
        .withTeamCode("TEAM1")
        .withApplication(application)
        .produce(),
    )

    communityApiStaffUserDetailsReturnsTeamCodes("TEAM1")

    every { offenderService.getOffenderByCrn(application.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    assertThat(userAccessService.userCanViewApplication(user, application)).isTrue
  }

  @Test
  fun `userCanViewApplication returns false if the application is an Approved Premises application and the user cannot access the Offender (LAO)`() {
    val newestJsonSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory()
      .withSchema("{}")
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(anotherUserInRegion)
      .withApplicationSchema(newestJsonSchema)
      .withTeamCodes(mutableListOf())
      .produce()

    application.teamCodes.add(
      ApplicationTeamCodeEntityFactory()
        .withTeamCode("TEAM1")
        .withApplication(application)
        .produce(),
    )

    communityApiStaffUserDetailsReturnsTeamCodes("TEAM1")

    every { offenderService.getOffenderByCrn(application.crn, user.deliusUsername) } returns AuthorisableActionResult.Unauthorised()

    assertThat(userAccessService.userCanViewApplication(user, application)).isFalse
  }

  @Test
  fun `userCanViewApplication returns true if the user created the application for CAS2`() {
    val newestJsonSchema = Cas2ApplicationJsonSchemaEntityFactory()
      .withSchema("{}")
      .produce()

    val application = Cas2ApplicationEntityFactory()
      .withCreatedByUser(user)
      .withApplicationSchema(newestJsonSchema)
      .produce()

    assertThat(userAccessService.userCanViewApplication(user, application)).isTrue
  }

  @Test
  fun `userCanViewApplication returns false otherwise for CAS2`() {
    val newestJsonSchema = Cas2ApplicationJsonSchemaEntityFactory()
      .withSchema("{}")
      .produce()

    val application = Cas2ApplicationEntityFactory()
      .withCreatedByUser(anotherUserInRegion)
      .withApplicationSchema(newestJsonSchema)
      .produce()

    assertThat(userAccessService.userCanViewApplication(user, application)).isFalse
  }

  @Test
  fun `userCanViewApplication returns true if the user created the application for Temporary Accommodation`() {
    val newestJsonSchema = TemporaryAccommodationApplicationJsonSchemaEntityFactory()
      .withSchema("{}")
      .produce()

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(user)
      .withApplicationSchema(newestJsonSchema)
      .withProbationRegion(probationRegion)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    assertThat(userAccessService.userCanViewApplication(user, application)).isTrue
  }

  @Test
  fun `userCanViewApplication returns false if the user has the CAS3_ASSESSOR role but the application is not in their region for Temporary Accommodation`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    val newestJsonSchema = TemporaryAccommodationApplicationJsonSchemaEntityFactory()
      .withSchema("{}")
      .produce()

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(anotherUserNotInRegion)
      .withApplicationSchema(newestJsonSchema)
      .withProbationRegion(anotherProbationRegion)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    assertThat(userAccessService.userCanViewApplication(user, application)).isFalse
  }

  @Test
  fun `userCanViewApplication returns false if the user has the CAS3_ASSESSOR role and the application is in their region but it has not been submitted for Temporary Accommodation`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    val newestJsonSchema = TemporaryAccommodationApplicationJsonSchemaEntityFactory()
      .withSchema("{}")
      .produce()

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(anotherUserInRegion)
      .withApplicationSchema(newestJsonSchema)
      .withProbationRegion(probationRegion)
      .withSubmittedAt(null)
      .produce()

    assertThat(userAccessService.userCanViewApplication(user, application)).isFalse
  }

  @Test
  fun `userCanViewApplication returns true if the application has been submitted, is in the user's region, and the user has the CAS3_ASSESSOR role for Temporary Accommodation`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    val newestJsonSchema = TemporaryAccommodationApplicationJsonSchemaEntityFactory()
      .withSchema("{}")
      .produce()

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(anotherUserInRegion)
      .withApplicationSchema(newestJsonSchema)
      .withProbationRegion(probationRegion)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    assertThat(userAccessService.userCanViewApplication(user, application)).isTrue
  }

  @Test
  fun `userCanViewApplication returns false otherwise for Temporary Accommodation`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    val newestJsonSchema = TemporaryAccommodationApplicationJsonSchemaEntityFactory()
      .withSchema("{}")
      .produce()

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(anotherUserInRegion)
      .withApplicationSchema(newestJsonSchema)
      .withProbationRegion(probationRegion)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    assertThat(userAccessService.userCanViewApplication(user, application)).isFalse
  }
}
