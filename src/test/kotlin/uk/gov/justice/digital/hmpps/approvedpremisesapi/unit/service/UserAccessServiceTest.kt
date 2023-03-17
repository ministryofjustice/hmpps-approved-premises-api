package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addRoleForUnitTest
import java.util.UUID
import jakarta.servlet.http.HttpServletRequest

class UserAccessServiceTest {
  private val userService = mockk<UserService>()
  private val currentRequest = mockk<HttpServletRequest>()

  private val userAccessService = UserAccessService(
    userService,
    currentRequest,
  )

  private val probationRegionId = UUID.randomUUID()
  private val probationRegion = ProbationRegionEntityFactory()
    .withId(probationRegionId)
    .withApArea(
      ApAreaEntityFactory()
        .produce()
    )
    .produce()

  private val user = UserEntityFactory()
    .withProbationRegion(probationRegion)
    .produce()

  val approvedPremises = ApprovedPremisesEntityFactory()
    .withProbationRegion(probationRegion)
    .withLocalAuthorityArea(
      LocalAuthorityEntityFactory()
        .produce()
    )
    .produce()

  val temporaryAccommodationPremisesInUserRegion = TemporaryAccommodationPremisesEntityFactory()
    .withProbationRegion(probationRegion)
    .withLocalAuthorityArea(
      LocalAuthorityEntityFactory()
        .produce()
    )
    .produce()

  val temporaryAccommodationPremisesNotInUserRegion = TemporaryAccommodationPremisesEntityFactory()
    .withProbationRegion(
      ProbationRegionEntityFactory()
        .withApArea(
          ApAreaEntityFactory()
            .produce()
        )
        .produce()
    )
    .withLocalAuthorityArea(
      LocalAuthorityEntityFactory()
        .produce()
    )
    .produce()

  private fun currentRequestIsFor(service: ServiceName) {
    every { currentRequest.getHeader("X-Service-Name") } returns service.value
  }

  private fun currentRequestIsForArbitraryService() {
    every { currentRequest.getHeader("X-Service-Name") } returns "arbitrary-value"
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
  @EnumSource(value = UserRole::class, names = [ "MANAGER", "MATCHER" ])
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
  @EnumSource(value = UserRole::class, names = [ "MANAGER", "MATCHER" ])
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
  @EnumSource(value = UserRole::class, names = [ "MANAGER", "MATCHER" ])
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
  @EnumSource(value = UserRole::class, names = [ "MANAGER", "MATCHER" ])
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
  @EnumSource(value = UserRole::class, names = [ "MANAGER", "MATCHER" ])
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
  @EnumSource(value = UserRole::class, names = [ "MANAGER", "MATCHER" ])
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
  @EnumSource(value = UserRole::class, names = [ "MANAGER", "MATCHER" ])
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
  @EnumSource(value = UserRole::class, names = [ "MANAGER", "MATCHER" ])
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
}
