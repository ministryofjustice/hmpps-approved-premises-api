package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApplicationTeamCodeEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_REFERRER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_REPORTER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.RequestContextService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.TemporaryAccommodationApplicationAccessLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addRoleForUnitTest
import java.time.OffsetDateTime
import java.util.UUID

class UserAccessServiceTest {
  private val userService = mockk<UserService>()
  private val offenderService = mockk<OffenderService>()
  private val requestContextService = mockk<RequestContextService>()

  private val userAccessService = UserAccessService(
    userService,
    offenderService,
    requestContextService,
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
    every { requestContextService.getServiceForRequest() } returns service
  }

  private fun currentRequestIsForArbitraryService() {
    every { requestContextService.getServiceForRequest() } returns null
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
  fun `userCanViewPremises returns true if the given premises is a Temporary Accommodation premises and the user has the CAS3_ASSESSOR role and can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.userCanViewPremises(user, temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `userCanViewPremises returns false if the given premises is a Temporary Accommodation premises and the user has the CAS3_ASSESSOR role and cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.userCanViewPremises(user, temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @Test
  fun `userCanViewPremises returns false if the given premises is a Temporary Accommodation premises and the user does not have a suitable role`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanViewPremises(user, temporaryAccommodationPremisesInUserRegion)).isFalse
    assertThat(userAccessService.userCanViewPremises(user, temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @Test
  fun `currentUserCanViewPremises returns true if the given premises is an Approved Premises`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.currentUserCanViewPremises(approvedPremises)).isTrue
  }

  @Test
  fun `currentUserCanViewPremises returns true if the given premises is a Temporary Accommodation premises and the current user has the CAS3_ASSESSOR role and can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.currentUserCanViewPremises(temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `currentUserCanViewPremises returns false if the given premises is a Temporary Accommodation premises and the current user has the CAS3_ASSESSOR role and cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.currentUserCanViewPremises(temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @Test
  fun `currentUserCanViewPremises returns false if the given premises is a Temporary Accommodation premises and the user does not have a suitable role`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserCanViewPremises(temporaryAccommodationPremisesInUserRegion)).isFalse
    assertThat(userAccessService.currentUserCanViewPremises(temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @Test
  fun `userCanManagePremises returns true if the given premises is an Approved Premises`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.userCanManagePremises(user, approvedPremises)).isTrue
  }

  @Test
  fun `userCanManagePremises returns true if the given premises is a Temporary Accommodation premises and the user has the CAS3_ASSESSOR role and can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.userCanManagePremises(user, temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `userCanManagePremises returns false if the given premises is a Temporary Accommodation premises and the user has the CAS3_ASSESSOR role and cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.userCanManagePremises(user, temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @Test
  fun `userCanManagePremises returns false if the given premises is a Temporary Accommodation premises and the user does not have a suitable role`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanManagePremises(user, temporaryAccommodationPremisesInUserRegion)).isFalse
    assertThat(userAccessService.userCanManagePremises(user, temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @Test
  fun `currentUserCanManagePremises returns true if the given premises is an Approved Premises`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.currentUserCanManagePremises(approvedPremises)).isTrue
  }

  @Test
  fun `currentUserCanManagePremises returns true if the given premises is a Temporary Accommodation premises and the current user has the CAS3_ASSESSOR role and can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.currentUserCanManagePremises(temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `currentUserCanManagePremises returns false if the given premises is a Temporary Accommodation premises and the current user has the CAS3_ASSESSOR role and cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.currentUserCanManagePremises(temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @Test
  fun `currentUserCanManagePremises returns false if the given premises is a Temporary Accommodation premises and the user does not have a suitable role`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserCanManagePremises(temporaryAccommodationPremisesInUserRegion)).isFalse
    assertThat(userAccessService.currentUserCanManagePremises(temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @Nested
  inner class UserCanViewBooking {

    private val cas1Booking = BookingEntityFactory()
      .withPremises(approvedPremises)
      .produce()

    private val cas3BookingNotInUserRegion = BookingEntityFactory()
      .withPremises(temporaryAccommodationPremisesNotInUserRegion)
      .produce()

    private val cas3BookingInUserRegion = BookingEntityFactory()
      .withPremises(temporaryAccommodationPremisesInUserRegion)
      .produce()

    @Test
    fun `userCanViewBooking CAS1 always returns true`() {
      currentRequestIsFor(ServiceName.approvedPremises)

      assertThat(userAccessService.userCanViewBooking(user, cas1Booking)).isTrue
    }

    @Test
    fun `userCanViewBooking returns true if the given premises is a CAS3 premises and the user has the CAS3_ASSESSOR role and can access the premises's probation region`() {
      currentRequestIsFor(ServiceName.temporaryAccommodation)

      user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

      assertThat(userAccessService.userCanViewBooking(user, cas3BookingInUserRegion)).isTrue
    }

    @Test
    fun `userCanViewBooking returns false if the given premises is a CAS3 premises and the user has the CAS3_ASSESSOR role and cannot access the premises's probation region`() {
      currentRequestIsFor(ServiceName.temporaryAccommodation)

      user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

      assertThat(
        userAccessService.userCanViewBooking(
          user,
          cas3BookingNotInUserRegion,
        ),
      ).isFalse
    }

    @Test
    fun `userCanViewBooking returns false if the given premises is a CAS3 premises and the user does not have a suitable role`() {
      currentRequestIsFor(ServiceName.temporaryAccommodation)

      assertThat(userAccessService.userCanViewBooking(user, cas3BookingInUserRegion)).isFalse
      assertThat(userAccessService.userCanViewBooking(user, cas3BookingNotInUserRegion)).isFalse
    }
  }

  @Nested
  inner class UserCanManagePremisesBookings {

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER", "CAS1_WORKFLOW_MANAGER"])
    fun `userCanManagePremisesBookings returns true if the given premises is a CAS1 premises and the user has either the MANAGER or MATCHER user role`(
      role: UserRole,
    ) {
      currentRequestIsFor(ServiceName.approvedPremises)

      user.addRoleForUnitTest(role)

      assertThat(userAccessService.userCanManagePremisesBookings(user, approvedPremises)).isTrue
    }

    @Test
    fun `userCanManagePremisesBookings returns false if the given premises is a CAS1 premises and the user has no suitable role`() {
      currentRequestIsFor(ServiceName.approvedPremises)

      assertThat(userAccessService.userCanManagePremisesBookings(user, approvedPremises)).isFalse
    }

    @Test
    fun `userCanManagePremisesBookings returns true if the given premises is a CAS3 premises and the user has the CAS3_ASSESSOR role and can access the premises's probation region`() {
      currentRequestIsFor(ServiceName.temporaryAccommodation)

      user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

      assertThat(
        userAccessService.userCanManagePremisesBookings(
          user,
          temporaryAccommodationPremisesInUserRegion,
        ),
      ).isTrue
    }

    @Test
    fun `userCanManagePremisesBookings returns false if the given premises is a CAS3 premises and the user has the CAS3_ASSESSOR role and cannot access the premises's probation region`() {
      currentRequestIsFor(ServiceName.temporaryAccommodation)

      user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

      assertThat(
        userAccessService.userCanManagePremisesBookings(
          user,
          temporaryAccommodationPremisesNotInUserRegion,
        ),
      ).isFalse
    }

    @Test
    fun `userCanManagePremisesBookings returns false if the given premises is a CAS3 premises and the user does not have a suitable role`() {
      currentRequestIsFor(ServiceName.temporaryAccommodation)

      assertThat(
        userAccessService.userCanManagePremisesBookings(
          user,
          temporaryAccommodationPremisesInUserRegion,
        ),
      ).isFalse
      assertThat(
        userAccessService.userCanManagePremisesBookings(
          user,
          temporaryAccommodationPremisesNotInUserRegion,
        ),
      ).isFalse
    }
  }

  @Nested
  inner class UserMayCancelBooking {
    private val cas1Booking = BookingEntityFactory()
      .withPremises(approvedPremises)
      .produce()

    private val cas3BookingNotInUserRegion = BookingEntityFactory()
      .withPremises(temporaryAccommodationPremisesNotInUserRegion)
      .produce()

    private val cas3BookingInUserRegion = BookingEntityFactory()
      .withPremises(temporaryAccommodationPremisesInUserRegion)
      .produce()

    @ParameterizedTest
    @EnumSource(value = UserRole::class)
    fun `userMayCancelBooking returns true if the given premises is a CAS1 premises and the user has either the MANAGER or CAS1_WORKFLOW_MANAGER user role`(
      role: UserRole,
    ) {
      currentRequestIsFor(ServiceName.approvedPremises)

      user.addRoleForUnitTest(role)

      val canCancelBooking = listOf(UserRole.CAS1_WORKFLOW_MANAGER).contains(role)

      assertThat(userAccessService.userMayCancelBooking(user, cas1Booking)).isEqualTo(canCancelBooking)
    }

    @Test
    fun `userMayCancelBooking returns false if the given premises is a CAS1 premises and the user has no suitable role`() {
      currentRequestIsFor(ServiceName.approvedPremises)

      assertThat(userAccessService.userMayCancelBooking(user, cas1Booking)).isFalse
    }

    @Test
    fun `userMayCancelBooking returns true if the given premises is a CAS3 premises and the user has the CAS3_ASSESSOR role and can access the premises's probation region`() {
      currentRequestIsFor(ServiceName.temporaryAccommodation)

      user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

      assertThat(userAccessService.userMayCancelBooking(user, cas3BookingInUserRegion)).isTrue
    }

    @Test
    fun `userMayCancelBooking returns false if the given premises is a CAS3 premises and the user has the CAS3_ASSESSOR role and cannot access the premises's probation region`() {
      currentRequestIsFor(ServiceName.temporaryAccommodation)

      user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

      assertThat(userAccessService.userMayCancelBooking(user, cas3BookingNotInUserRegion)).isFalse
    }

    @Test
    fun `userMayCancelBooking returns false if the given premises is a CAS3 premises and the user does not have a suitable role`() {
      currentRequestIsFor(ServiceName.temporaryAccommodation)

      assertThat(userAccessService.userMayCancelBooking(user, cas3BookingInUserRegion)).isFalse
      assertThat(userAccessService.userMayCancelBooking(user, cas3BookingNotInUserRegion)).isFalse
    }
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
  fun `currentUserCanManagePremisesBookings returns true if the given premises is a Temporary Accommodation premises and the current user has the CAS3_ASSESSOR role and can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.currentUserCanManagePremisesBookings(temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `currentUserCanManagePremisesBookings returns false if the given premises is a Temporary Accommodation premises and the current user has the CAS3_ASSESSOR role and cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.currentUserCanManagePremisesBookings(temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @Test
  fun `currentUserCanManagePremisesBookings returns false if the given premises is a Temporary Accommodation premises and the user does not have a suitable role`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserCanManagePremisesBookings(temporaryAccommodationPremisesInUserRegion)).isFalse
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
  fun `userCanManagePremisesLostBeds returns true if the given premises is a Temporary Accommodation premises and the user has the CAS3_ASSESSOR role and can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.userCanManagePremisesLostBeds(user, temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `userCanManagePremisesLostBeds returns false if the given premises is a Temporary Accommodation premises and the user has the CAS3_ASSESSOR role and cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.userCanManagePremisesLostBeds(user, temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @Test
  fun `userCanManagePremisesLostBeds returns false if the given premises is a Temporary Accommodation premises and the user does not have a suitable role`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanManagePremisesLostBeds(user, temporaryAccommodationPremisesInUserRegion)).isFalse
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
  fun `currentUserCanManagePremisesLostBeds returns true if the given premises is a Temporary Accommodation premises and the current user has the CAS3_ASSESSOR role and can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.currentUserCanManagePremisesLostBeds(temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `currentUserCanManagePremisesLostBeds returns false if the given premises is a Temporary Accommodation premises and the current user has the CAS3_ASSESSOR role and cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.currentUserCanManagePremisesLostBeds(temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @Test
  fun `currentUserCanManagePremisesLostBeds returns false if the given premises is a Temporary Accommodation premises and the user does not have a suitable role`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserCanManagePremisesLostBeds(temporaryAccommodationPremisesInUserRegion)).isFalse
    assertThat(userAccessService.currentUserCanManagePremisesLostBeds(temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "CAS1_FUTURE_MANAGER", "CAS1_MANAGER", "CAS1_MATCHER" ])
  fun `userCanManagePremisesOutOfServiceBed returns true if the given premises is an Approved Premises and the user has one of the FUTURE_MANAGER, MANAGER or MATCHER user roles`(role: UserRole) {
    currentRequestIsFor(ServiceName.approvedPremises)

    user.addRoleForUnitTest(role)

    assertThat(userAccessService.userCanManagePremisesOutOfServiceBed(user, approvedPremises)).isTrue
  }

  @Test
  fun `userCanManagePremisesOutOfServiceBed returns false if the given premises is an Approved Premises and the user has no suitable role`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.userCanManagePremisesOutOfServiceBed(user, approvedPremises)).isFalse
  }

  @Test
  fun `userCanManagePremisesOutOfServiceBed returns true if the given premises is a Temporary Accommodation premises and the user has the CAS3_ASSESSOR role and can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.userCanManagePremisesOutOfServiceBed(user, temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `userCanManagePremisesOutOfServiceBed returns false if the given premises is a Temporary Accommodation premises and the user has the CAS3_ASSESSOR role and cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.userCanManagePremisesOutOfServiceBed(user, temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @Test
  fun `userCanManagePremisesOutOfServiceBed returns false if the given premises is a Temporary Accommodation premises and the user does not have a suitable role`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanManagePremisesOutOfServiceBed(user, temporaryAccommodationPremisesInUserRegion)).isFalse
    assertThat(userAccessService.userCanManagePremisesOutOfServiceBed(user, temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "CAS1_FUTURE_MANAGER", "CAS1_MANAGER", "CAS1_MATCHER" ])
  fun `currentUserCanManagePremisesOutOfServiceBed returns true if the given premises is an Approved Premises and the current user has one of the FUTURE_MANAGER, MANAGER or MATCHER user roles`(role: UserRole) {
    currentRequestIsFor(ServiceName.approvedPremises)

    user.addRoleForUnitTest(role)

    assertThat(userAccessService.currentUserCanManagePremisesOutOfServiceBed(approvedPremises)).isTrue
  }

  @Test
  fun `currentUserCanManagePremisesOutOfServiceBed returns false if the given premises is an Approved Premises and the current user has no suitable role`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.currentUserCanManagePremisesOutOfServiceBed(approvedPremises)).isFalse
  }

  @Test
  fun `currentUserCanManagePremisesOutOfServiceBed returns true if the given premises is a Temporary Accommodation premises and the current user has the CAS3_ASSESSOR role and can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.currentUserCanManagePremisesOutOfServiceBed(temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `currentUserCanManagePremisesOutOfServiceBed returns false if the given premises is a Temporary Accommodation premises and the current user has the CAS3_ASSESSOR role and cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.currentUserCanManagePremisesOutOfServiceBed(temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @Test
  fun `currentUserCanManagePremisesOutOfServiceBed returns false if the given premises is a Temporary Accommodation premises and the user does not have a suitable role`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserCanManagePremisesOutOfServiceBed(temporaryAccommodationPremisesInUserRegion)).isFalse
    assertThat(userAccessService.currentUserCanManagePremisesOutOfServiceBed(temporaryAccommodationPremisesNotInUserRegion)).isFalse
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
  fun `userCanViewPremisesCapacity returns true if the given premises is a Temporary Accommodation premises and the user has the CAS3_ASSESSOR role and can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.userCanViewPremisesCapacity(user, temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `userCanViewPremisesCapacity returns false if the given premises is a Temporary Accommodation premises and the user has the CAS3_ASSESSOR role and cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.userCanViewPremisesCapacity(user, temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @Test
  fun `userCanViewPremisesCapacity returns false if the given premises is a Temporary Accommodation premises and the user does not have a suitable role`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanViewPremisesCapacity(user, temporaryAccommodationPremisesInUserRegion)).isFalse
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
  fun `currentUserCanViewPremisesCapacity returns true if the given premises is a Temporary Accommodation premises and the current user has the CAS3_ASSESSOR role and can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.currentUserCanViewPremisesCapacity(temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `currentUserCanViewPremisesCapacity returns false if the given premises is a Temporary Accommodation premises and the current user has the CAS3_ASSESSOR role and cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.currentUserCanViewPremisesCapacity(temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @Test
  fun `currentUserCanViewPremisesCapacity returns false if the given premises is a Temporary Accommodation premises and the user does not have a suitable role`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserCanViewPremisesCapacity(temporaryAccommodationPremisesInUserRegion)).isFalse
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
  fun `userCanViewPremisesStaff returns true if the given premises is a Temporary Accommodation premises and the user has the CAS3_ASSESSOR role and can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.userCanViewPremisesStaff(user, temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `userCanViewPremisesStaff returns false if the given premises is a Temporary Accommodation premises and the user has the CAS3_ASSESSOR role and cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.userCanViewPremisesStaff(user, temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @Test
  fun `userCanViewPremisesStaff returns false if the given premises is a Temporary Accommodation premises and the user does not have a suitable role`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanViewPremisesStaff(user, temporaryAccommodationPremisesInUserRegion)).isFalse
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
  fun `currentUserCanViewPremisesStaff returns true if the given premises is a Temporary Accommodation premises and the current user has the CAS3_ASSESSOR role and can access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.currentUserCanViewPremisesStaff(temporaryAccommodationPremisesInUserRegion)).isTrue
  }

  @Test
  fun `currentUserCanViewPremisesStaff returns false if the given premises is a Temporary Accommodation premises and the current user has the CAS3_ASSESSOR role and cannot access the premises's probation region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.currentUserCanViewPremisesStaff(temporaryAccommodationPremisesNotInUserRegion)).isFalse
  }

  @Test
  fun `currentUserCanViewPremisesStaff returns false if the given premises is a Temporary Accommodation premises and the user does not have a suitable role`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserCanViewPremisesStaff(temporaryAccommodationPremisesInUserRegion)).isFalse
    assertThat(userAccessService.currentUserCanViewPremisesStaff(temporaryAccommodationPremisesNotInUserRegion)).isFalse
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

    every { offenderService.getOffenderByCrn(application.crn, user.deliusUsername) } returns AuthorisableActionResult.Unauthorised()

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

  @Test
  fun `userCanViewReport returns returns true if the current request has 'X-Service-Name' header with value 'temporary-accommodation' and the user has the CAS3_ASSESSOR role`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.userCanViewReport(user)).isTrue
  }

  @Test
  fun `userCanViewReport returns false otherwise if the current request has 'X-Service-Name' header with value 'temporary-accommodation'`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanViewReport(user)).isFalse
  }

  @Test
  fun `userCanViewReport returns false by default`() {
    currentRequestIsForArbitraryService()

    assertThat(userAccessService.userCanViewReport(user)).isFalse
  }

  @Test
  fun `currentUserCanViewReport returns returns true if the current request has 'X-Service-Name' header with value 'temporary-accommodation' and the user has the CAS3_ASSESSOR role`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.currentUserCanViewReport()).isTrue
  }

  @Test
  fun `currentUserCanViewReport returns false otherwise if the current request has 'X-Service-Name' header with value 'temporary-accommodation'`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserCanViewReport()).isFalse
  }

  @Test
  fun `currentUserCanViewReport returns false by default`() {
    currentRequestIsForArbitraryService()

    assertThat(userAccessService.currentUserCanViewReport()).isFalse
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_REPORT_VIEWER", "CAS1_ADMIN"], mode = EnumSource.Mode.EXCLUDE)
  fun `currentUserCanViewReport returns returns false if the current request has 'X-Service-Name' header with value 'approved-premises' and the user does not have the correct role`(role: UserRole) {
    currentRequestIsFor(ServiceName.approvedPremises)

    user.addRoleForUnitTest(role)

    assertThat(userAccessService.currentUserCanViewReport()).isFalse
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_REPORT_VIEWER", "CAS1_ADMIN"])
  fun `currentUserCanViewReport returns returns true if the current request has 'X-Service-Name' header with value 'approved-premises' and the user has the correct role`(role: UserRole) {
    currentRequestIsFor(ServiceName.approvedPremises)

    user.addRoleForUnitTest(role)

    assertThat(userAccessService.currentUserCanViewReport()).isTrue
  }

  @Test
  fun `userCanReallocateTask returns true if the current request has 'X-Service-Name' header with value 'temporary-accommodation' and the user has the CAS3_ASSESSOR role`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.userCanReallocateTask(user)).isTrue
  }

  @Test
  fun `userCanReallocateTask returns false otherwise if the current request has 'X-Service-Name' header with value 'temporary-accommodation'`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanReallocateTask(user)).isFalse
  }

  @Test
  fun `userCanReallocateTask returns true if the current request has 'X-Service-Name' header with value 'approved-premises' and the user has the CAS1_WORKFLOW_MANAGER role`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    user.addRoleForUnitTest(UserRole.CAS1_WORKFLOW_MANAGER)

    assertThat(userAccessService.userCanReallocateTask(user)).isTrue
  }

  @Test
  fun `userCanReallocateTask returns false otherwise if the current request has 'X-Service-Name' header with value 'approved-premises'`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.userCanReallocateTask(user)).isFalse
  }

  @Test
  fun `userCanReallocateTask returns false by default`() {
    currentRequestIsForArbitraryService()

    assertThat(userAccessService.userCanReallocateTask(user)).isFalse
  }

  @Test
  fun `currentUserCanReallocateTask returns true if the current request has 'X-Service-Name' header with value 'temporary-accommodation' and the user has the CAS3_ASSESSOR role`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.currentUserCanReallocateTask()).isTrue
  }

  @Test
  fun `currentUserCanReallocateTask returns false otherwise if the current request has 'X-Service-Name' header with value 'temporary-accommodation'`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserCanReallocateTask()).isFalse
  }

  @Test
  fun `currentUserCanReallocateTask returns true if the current request has 'X-Service-Name' header with value 'approved-premises' and the user has the CAS1_WORKFLOW_MANAGER role`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    user.addRoleForUnitTest(UserRole.CAS1_WORKFLOW_MANAGER)

    assertThat(userAccessService.currentUserCanReallocateTask()).isTrue
  }

  @Test
  fun `currentUserCanReallocateTask returns false otherwise if the current request has 'X-Service-Name' header with value 'approved-premises'`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    assertThat(userAccessService.currentUserCanReallocateTask()).isFalse
  }

  @Test
  fun `currentUserCanReallocateTask returns false by default`() {
    currentRequestIsForArbitraryService()

    assertThat(userAccessService.currentUserCanReallocateTask()).isFalse
  }

  @Test
  fun `userCanDeallocateTask returns true if the current request has 'X-Service-Name' header with value 'temporary-accommodation' and the user has the CAS3_ASSESSOR role`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.userCanDeallocateTask(user)).isTrue
  }

  @Test
  fun `userCanDeallocateTask returns false otherwise if the current request has 'X-Service-Name' header with value 'temporary-accommodation'`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.userCanDeallocateTask(user)).isFalse
  }

  @Test
  fun `userCanDeallocateTask returns false by default`() {
    currentRequestIsForArbitraryService()

    assertThat(userAccessService.userCanDeallocateTask(user)).isFalse
  }

  @Test
  fun `currentUserCanDeallocateTask returns true if the current request has 'X-Service-Name' header with value 'temporary-accommodation' and the user has the CAS3_ASSESSOR role`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.currentUserCanDeallocateTask()).isTrue
  }

  @Test
  fun `currentUserCanDeallocateTask returns false otherwise if the current request has 'X-Service-Name' header with value 'temporary-accommodation'`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    assertThat(userAccessService.currentUserCanDeallocateTask()).isFalse
  }

  @Test
  fun `currentUserCanDeallocateTask returns false by default`() {
    currentRequestIsForArbitraryService()

    assertThat(userAccessService.currentUserCanDeallocateTask()).isFalse
  }

  @Test
  fun `userCanViewAssessment returns true for a Temporary Accommodation assessment if the user has the CAS3_ASSESSOR role and the assessment is in the same region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(anotherUserInRegion)
      .withProbationRegion(probationRegion)
      .produce()

    val assessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(application)
      .produce()

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.userCanViewAssessment(user, assessment)).isTrue
  }

  @Test
  fun `userCanViewAssessment returns false for a Temporary Accommodation assessment if the user has the CAS3_ASSESSOR role but the assessment is not in the same region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(anotherUserNotInRegion)
      .withProbationRegion(anotherProbationRegion)
      .produce()

    val assessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(application)
      .produce()

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.userCanViewAssessment(user, assessment)).isFalse
  }

  @Test
  fun `userCanViewAssessment returns false for a Temporary Accommodation assessment if the user does not have the CAS3_ASSESSOR role`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(anotherUserInRegion)
      .withProbationRegion(probationRegion)
      .produce()

    val assessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(application)
      .produce()

    assertThat(userAccessService.userCanViewAssessment(user, assessment)).isFalse
  }

  @Test
  fun `userCanViewAssessment returns true for an Approved Premises assessment if the user has the CAS1_WORKFLOW_MANAGER role`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(anotherUserNotInRegion)
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(anotherUserNotInRegion)
      .produce()

    user.addRoleForUnitTest(UserRole.CAS1_WORKFLOW_MANAGER)

    assertThat(userAccessService.userCanViewAssessment(user, assessment)).isTrue
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class)
  fun `userCanViewAssessment returns true for an Approved Premises assessment for any role`(role: UserRole) {
    currentRequestIsFor(ServiceName.approvedPremises)
    user.addRoleForUnitTest(role)

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(anotherUserInRegion)
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(user)
      .produce()

    assertThat(userAccessService.userCanViewAssessment(user, assessment)).isTrue
  }

  @Test
  fun `currentUserCanViewAssessment returns true for a Temporary Accommodation assessment if the user has the CAS3_ASSESSOR role and the assessment is in the same region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(anotherUserInRegion)
      .withProbationRegion(probationRegion)
      .produce()

    val assessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(application)
      .produce()

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.currentUserCanViewAssessment(assessment)).isTrue
  }

  @Test
  fun `currentUserCanViewAssessment returns false for a Temporary Accommodation assessment if the user has the CAS3_ASSESSOR role but the assessment is not in the same region`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(anotherUserNotInRegion)
      .withProbationRegion(anotherProbationRegion)
      .produce()

    val assessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(application)
      .produce()

    user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)

    assertThat(userAccessService.currentUserCanViewAssessment(assessment)).isFalse
  }

  @Test
  fun `currentUserCanViewAssessment returns false for a Temporary Accommodation assessment if the user does not have the CAS3_ASSESSOR role`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(anotherUserInRegion)
      .withProbationRegion(probationRegion)
      .produce()

    val assessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(application)
      .produce()

    assertThat(userAccessService.currentUserCanViewAssessment(assessment)).isFalse
  }

  @Test
  fun `currentUserCanViewAssessment returns true for an Approved Premises assessment if the user has the CAS1_WORKFLOW_MANAGER role`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(anotherUserNotInRegion)
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(anotherUserNotInRegion)
      .produce()

    user.addRoleForUnitTest(UserRole.CAS1_WORKFLOW_MANAGER)

    assertThat(userAccessService.currentUserCanViewAssessment(assessment)).isTrue
  }

  @Test
  fun `currentUserCanViewAssessment returns true for an Approved Premises assessment if the assessment is assigned to the user`() {
    currentRequestIsFor(ServiceName.approvedPremises)

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(anotherUserInRegion)
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(user)
      .produce()

    assertThat(userAccessService.currentUserCanViewAssessment(assessment)).isTrue
  }

  @Test
  fun `reporterUserCanViewReport returns true if the current request has 'X-Service-Name' header with value 'temporary-accommodation' and the user has the CAS3_REPORTER role`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)
    user.addRoleForUnitTest(CAS3_REPORTER)

    assertThat(userAccessService.currentUserCanViewReport()).isTrue
  }

  @Test
  fun `reporterUserCanViewReport returns false otherwise if the current request has 'X-Service-Name' header with value 'approved-premises' and the user has the CAS3_REPORTER role`() {
    currentRequestIsFor(ServiceName.approvedPremises)
    user.addRoleForUnitTest(CAS3_REPORTER)

    assertThat(userAccessService.currentUserCanViewReport()).isFalse
  }

  @Test
  fun `userHasAllRegionsAccess returns true if the current request has 'X-Service-Name' header with value 'temporary-accommodation' and user role is CAS3_REPORTER`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)
    user.addRoleForUnitTest(CAS3_REPORTER)

    assertThat(userAccessService.userHasAllRegionsAccess(user)).isTrue()
  }

  @Test
  fun `userHasAllRegionsAccess returns false if the current request has 'X-Service-Name' header with value 'temporary-accommodation' and user role is CAS3_REFERRER`() {
    currentRequestIsFor(ServiceName.temporaryAccommodation)
    user.addRoleForUnitTest(CAS3_REFERRER)

    assertThat(userAccessService.userHasAllRegionsAccess(user)).isFalse()
  }

  @Nested
  inner class UserMayWithdrawApplication {

    @Test
    fun `userMayWithdrawApplication returns true if application was created by user`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      assertThat(userAccessService.userMayWithdrawApplication(user, application)).isTrue
    }

    @Test
    fun `userMayWithdrawApplication returns false if application was not created by user`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(anotherUserInRegion)
        .produce()

      assertThat(userAccessService.userMayWithdrawApplication(user, application)).isFalse()
    }

    @Test
    fun `userMayWithdrawApplication returns true if submitted and has CAS1_WORKFLOW_MANAGER role`() {
      val workflowManager = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      workflowManager.roles.add(
        UserRoleAssignmentEntityFactory()
          .withUser(user)
          .withRole(UserRole.CAS1_WORKFLOW_MANAGER)
          .produce(),
      )

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      assertThat(userAccessService.userMayWithdrawApplication(workflowManager, application)).isTrue()
    }

    @Test
    fun `userMayWithdrawApplication returns false if not submitted and has CAS1_WORKFLOW_MANAGER role`() {
      val workflowManager = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      workflowManager.roles.add(
        UserRoleAssignmentEntityFactory()
          .withUser(user)
          .withRole(UserRole.CAS1_WORKFLOW_MANAGER)
          .produce(),
      )

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      assertThat(userAccessService.userMayWithdrawApplication(workflowManager, application)).isFalse()
    }

    @Test
    fun `userMayWithdrawApplication returns false if not CAS1`() {
      val newestJsonSchema = TemporaryAccommodationApplicationJsonSchemaEntityFactory()
        .withSchema("{}")
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(anotherUserInRegion)
        .withApplicationSchema(newestJsonSchema)
        .withProbationRegion(probationRegion)
        .withSubmittedAt(null)
        .produce()

      assertThat(userAccessService.userMayWithdrawApplication(user, application)).isFalse
    }
  }

  @Nested
  inner class UserMayWithdrawPlacementRequest {

    @Test
    fun `userMayWithdrawPlacementRequest returns true if application was created by user`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .produce()

      assertThat(userAccessService.userMayWithdrawPlacementRequest(user, placementRequest)).isTrue
    }

    @Test
    fun `userMayWithdrawPlacementRequest returns false if application was not created by user and doesn't have CAS1_WORKFLOW_MANAGER role`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(anotherUserInRegion)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .produce()

      assertThat(userAccessService.userMayWithdrawPlacementRequest(user, placementRequest)).isFalse
    }

    @Test
    fun `userMayWithdrawPlacementRequest returns true if user has CAS1_WORKFLOW_MANAGER role`() {
      val workflowManager = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      workflowManager.roles.add(
        UserRoleAssignmentEntityFactory()
          .withUser(user)
          .withRole(UserRole.CAS1_WORKFLOW_MANAGER)
          .produce(),
      )

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(user)
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .produce()

      assertThat(userAccessService.userMayWithdrawPlacementRequest(workflowManager, placementRequest)).isTrue
    }
  }

  @Nested
  inner class UserMayWithdrawPlacementApplication {

    @Test
    fun `userMayWithdrawPlacementApplication returns true if application was created by user`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withCreatedByUser(user)
        .produce()

      assertThat(userAccessService.userMayWithdrawPlacementApplication(user, placementApplication)).isTrue
    }

    @Test
    fun `userMayWithdrawPlacementApplication returns false if application was not created by user`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(anotherUserInRegion)
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withCreatedByUser(anotherUserInRegion)
        .produce()

      assertThat(userAccessService.userMayWithdrawPlacementApplication(user, placementApplication)).isFalse
    }

    @Test
    fun `userMayWithdrawPlacementApplication returns true if submitted and has CAS1_WORKFLOW_MANAGER role`() {
      val workflowManager = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      workflowManager.roles.add(
        UserRoleAssignmentEntityFactory()
          .withUser(user)
          .withRole(UserRole.CAS1_WORKFLOW_MANAGER)
          .produce(),
      )

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withCreatedByUser(user)
        .produce()

      assertThat(userAccessService.userMayWithdrawPlacementApplication(user, placementApplication)).isTrue
    }

    @Test
    fun `userMayWithdrawPlacementApplication returns false if not submitted and has CAS1_WORKFLOW_MANAGER role`() {
      val workflowManager = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      workflowManager.roles.add(
        UserRoleAssignmentEntityFactory()
          .withUser(user)
          .withRole(UserRole.CAS1_WORKFLOW_MANAGER)
          .produce(),
      )

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(anotherUserInRegion)
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withCreatedByUser(anotherUserInRegion)
        .withSubmittedAt(null)
        .produce()

      assertThat(userAccessService.userMayWithdrawPlacementApplication(user, placementApplication)).isFalse
    }
  }
}
