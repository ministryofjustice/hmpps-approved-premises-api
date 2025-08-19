package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_CRU_MEMBER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_JANITOR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EnvironmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addRoleForUnitTest
import java.time.OffsetDateTime

class Cas1UserAccessServiceTest {
  private val userService = mockk<UserService>()
  private val environmentService = mockk<EnvironmentService>()

  private val service = Cas1UserAccessService(
    userService,
    environmentService,
  )

  @Nested
  inner class CurrentUserHasPermission {

    @Test
    fun `currentUserHasPermission denies experimental permissions in production environment`() {
      every {
        userService.getUserForRequest()
      } returns UserEntityFactory().withDefaults().withRoles(CAS1_JANITOR).produce()

      every { environmentService.isNotProd() } returns false

      val result = service.currentUserHasPermission(UserPermission.CAS1_TEST_EXPERIMENTAL_PERMISSION)

      assertThat(result).isFalse
    }

    @Test
    fun `currentUserHasPermission allows experimental permissions in non-production environment`() {
      every {
        userService.getUserForRequest()
      } returns UserEntityFactory().withDefaults().withRoles(CAS1_JANITOR).produce()

      every { environmentService.isNotProd() } returns true

      val result = service.currentUserHasPermission(UserPermission.CAS1_TEST_EXPERIMENTAL_PERMISSION)

      assertThat(result).isTrue
    }
  }

  @Nested
  inner class UserMayWithdrawApplication {
    private val probationRegion = ProbationRegionEntityFactory().withDefaults().produce()
    private val user = UserEntityFactory().withProbationRegion(probationRegion).produce()
    private val anotherUserInRegion = UserEntityFactory().withProbationRegion(probationRegion).produce()

    @Test
    fun `userMayWithdrawApplication returns true if application was created by user`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      assertThat(service.userMayWithdrawApplication(user, application)).isTrue
    }

    @Test
    fun `userMayWithdrawApplication returns false if application was not created by user`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(anotherUserInRegion)
        .produce()

      assertThat(service.userMayWithdrawApplication(user, application)).isFalse()
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class)
    fun `userMayWithdrawApplication returns true if submitted and has CRU_MEMBER or JANITOR role`(
      role: UserRole,
    ) {
      val otherUser = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      otherUser.addRoleForUnitTest(role)

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val canCancelApplication = listOf(CAS1_CRU_MEMBER, CAS1_JANITOR).contains(role)

      assertThat(service.userMayWithdrawApplication(otherUser, application)).isEqualTo(canCancelApplication)
    }

    @Test
    fun `userMayWithdrawApplication returns false if not submitted and has CAS1_CRU_MEMBER role`() {
      val cruMember = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      cruMember.roles.add(
        UserRoleAssignmentEntityFactory()
          .withUser(user)
          .withRole(CAS1_CRU_MEMBER)
          .produce(),
      )

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      assertThat(service.userMayWithdrawApplication(cruMember, application)).isFalse()
    }

    @Test
    fun `userMayWithdrawApplication returns false if not CAS1`() {
      val application = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(anotherUserInRegion)
        .withProbationRegion(probationRegion)
        .withSubmittedAt(null)
        .produce()

      assertThat(service.userMayWithdrawApplication(user, application)).isFalse
    }
  }

  @Nested
  inner class UserMayWithdrawPlacementApplication {
    private val probationRegion = ProbationRegionEntityFactory().withDefaults().produce()
    private val user = UserEntityFactory().withProbationRegion(probationRegion).produce()
    private val anotherUserInRegion = UserEntityFactory().withProbationRegion(probationRegion).produce()

    @Test
    fun `userMayWithdrawPlacementApplication returns true if application was created by user`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withCreatedByUser(user)
        .produce()

      assertThat(service.userMayWithdrawPlacementApplication(user, placementApplication)).isTrue
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

      assertThat(service.userMayWithdrawPlacementApplication(user, placementApplication)).isFalse
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class)
    fun `userMayWithdrawPlacementApplication returns true if submitted and has CRU_MEMBER or JANITOR role`(
      role: UserRole,
    ) {
      val otherUser = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      otherUser.addRoleForUnitTest(role)

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val canCancelPlacementApp = listOf(CAS1_CRU_MEMBER, CAS1_JANITOR).contains(role)

      assertThat(service.userMayWithdrawPlacementApplication(otherUser, placementApplication)).isEqualTo(canCancelPlacementApp)
    }

    @Test
    fun `userMayWithdrawPlacementApplication returns false if not submitted and has CAS1_CRU_MEMBER role`() {
      val cruMember = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      cruMember.roles.add(
        UserRoleAssignmentEntityFactory()
          .withUser(user)
          .withRole(CAS1_CRU_MEMBER)
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

      assertThat(service.userMayWithdrawPlacementApplication(user, placementApplication)).isFalse
    }
  }
}
