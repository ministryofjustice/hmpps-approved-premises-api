package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission.adhocBookingCreate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission.applicationWithdrawOthers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission.assessAppealedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission.bookingCreate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission.bookingWithdraw
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission.outOfServiceBedCreate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission.premisesViewSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission.processAnAppeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission.requestForPlacementWithdrawOthers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission.viewAssignedAssessments
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission.viewCruDashboard
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission.viewManageTasks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission.viewOutOfServiceBeds
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole.appealsManager
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole.matcher
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole.workflowManager
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProfileResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName.approvedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName.temporaryAccommodation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUserRole.referrer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUserRole.reporter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserWithWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.WOMENS
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_APPEALS_MANAGER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_JANITOR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_MATCHER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_WORKFLOW_MANAGER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_REFERRER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_REPORTER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.UserWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationDeliveryUnitTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationRegionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addQualificationForUnitTest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addRoleForUnitTest
import java.time.OffsetDateTime
import java.util.UUID.randomUUID

class UserTransformerTest {
  private val probationRegionTransformer = mockk<ProbationRegionTransformer>()
  private val probationDeliveryUnitTransformer = mockk<ProbationDeliveryUnitTransformer>()
  private val apAreaTransformer = mockk<ApAreaTransformer>()

  private val userTransformer = UserTransformer(probationRegionTransformer, probationDeliveryUnitTransformer, apAreaTransformer)

  private val apArea = ApArea(randomUUID(), "someIdentifier", "someName")

  @BeforeEach
  fun setup() {
    every { probationRegionTransformer.transformJpaToApi(any()) } returns ProbationRegion(randomUUID(), "someName")
  }

  @Nested
  inner class TransformJapToSummaryApi {

    @Test
    fun `successfully transform user`() {
      val id = randomUUID()
      val user = UserEntityFactory()
        .withDefaults()
        .withId(id)
        .withName("the name")
        .produce()

      val result = userTransformer.transformJpaToSummaryApi(user)

      assertThat(result.id).isEqualTo(id)
      assertThat(result.name).isEqualTo("the name")
    }
  }

  @Nested
  inner class TransformJpaToApi {

    @Test
    fun `transformJpaToApi CAS3 Should successfully transfer user entity with role CAS3_REPORTER to reporter`() {
      val result = userTransformer.transformJpaToApi(
        buildUserEntity(CAS3_REPORTER),
        temporaryAccommodation,
      ) as TemporaryAccommodationUser

      assertThat(result.roles).contains(reporter)
      assertThat(result.service).isEqualTo("CAS3")
      verify(exactly = 1) { probationRegionTransformer.transformJpaToApi(any()) }
    }

    @Test
    fun `transformJpaToApi CAS3 Should successfully transfer user entity with role CAS3_REFERRER to referrer`() {
      val result = userTransformer.transformJpaToApi(
        buildUserEntity(CAS3_REFERRER),
        temporaryAccommodation,
      ) as TemporaryAccommodationUser

      assertThat(result.roles).contains(referrer)
      assertThat(result.service).isEqualTo("CAS3")
      verify(exactly = 1) { probationRegionTransformer.transformJpaToApi(any()) }
    }

    @Test
    fun `transformJpaToApi CAS1 Should successfully transfer user entity with role CAS1_MATCHER to matcher`() {
      val apAreaEntity = ApAreaEntityFactory().produce()

      val user = buildUserEntity(
        role = CAS1_MATCHER,
        apArea = apAreaEntity,
      )

      every { apAreaTransformer.transformJpaToApi(apAreaEntity) } returns apArea

      val result = userTransformer.transformJpaToApi(user, approvedPremises) as ApprovedPremisesUser

      assertThat(result.roles).contains(matcher)
      assertThat(result.service).isEqualTo("CAS1")
      verify(exactly = 1) { probationRegionTransformer.transformJpaToApi(any()) }
      assertThat(result.apArea).isEqualTo(apArea)
    }

    @Test
    fun `transformJpaToApi CAS1 should return distinct roles for Approved Premises`() {
      val user = buildUserEntity(
        role = CAS1_MATCHER,
        apArea = ApAreaEntityFactory().produce(),
      )
      user.addRoleForUnitTest(CAS1_MATCHER)
      user.addRoleForUnitTest(CAS1_MATCHER)
      user.addRoleForUnitTest(CAS1_MATCHER)
      user.addRoleForUnitTest(CAS1_WORKFLOW_MANAGER)
      user.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      every { apAreaTransformer.transformJpaToApi(any()) } returns apArea

      val result =
        userTransformer.transformJpaToApi(user, approvedPremises) as ApprovedPremisesUser

      assertThat(result.roles).isEqualTo(
        listOf(
          matcher,
          workflowManager,
          appealsManager,
        ),
      )
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_APPEALS_MANAGER"])
    fun `transformJpaToApi CAS1 should return permissions for Approved Premises roles which have permissions defined`(role: UserRole) {
      val user = buildUserEntity(
        role = role,
        apArea = ApAreaEntityFactory().produce(),
      )

      every { apAreaTransformer.transformJpaToApi(any()) } returns apArea

      val result =
        userTransformer.transformJpaToApi(user, approvedPremises) as ApprovedPremisesUser

      assertThat(result.permissions).isEqualTo(
        listOf(
          assessAppealedApplication,
          processAnAppeal,
          viewAssignedAssessments,
        ),
      )
    }

    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      names = [
        "CAS1_JANITOR",
        "CAS1_APPEALS_MANAGER",
        "CAS1_ASSESSOR",
        "CAS1_MATCHER",
        "CAS1_CRU_MEMBER",
        "CAS1_FUTURE_MANAGER",
        "CAS1_WORKFLOW_MANAGER",
        "CAS1_MANAGER",
        "CAS1_LEGACY_MANAGER",
        "CAS1_REPORT_VIEWER",
      ],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `transformJpaToApi CAS1 should return no permissions for Approved Premises roles which have no permissions defined`(role: UserRole) {
      val user = buildUserEntity(
        role = role,
        apArea = ApAreaEntityFactory().produce(),
      )

      every { apAreaTransformer.transformJpaToApi(any()) } returns apArea

      val result =
        userTransformer.transformJpaToApi(user, approvedPremises) as ApprovedPremisesUser

      assertThat(result.permissions).isEmpty()
    }

    @Test
    fun `transformJpaToApi CAS1 should return distinct permissions for Approved Premises roles which have the same permissions defined`() {
      val user = buildUserEntity(
        role = CAS1_JANITOR,
        apArea = ApAreaEntityFactory().produce(),
      )
      user.addRoleForUnitTest(CAS1_APPEALS_MANAGER)

      every { apAreaTransformer.transformJpaToApi(any()) } returns apArea

      val result =
        userTransformer.transformJpaToApi(user, approvedPremises) as ApprovedPremisesUser

      assertThat(result.permissions).hasSameElementsAs(
        listOf(
          adhocBookingCreate,
          applicationWithdrawOthers,
          assessAppealedApplication,
          bookingCreate,
          bookingWithdraw,
          premisesViewSummary,
          processAnAppeal,
          outOfServiceBedCreate,
          requestForPlacementWithdrawOthers,
          viewAssignedAssessments,
          viewCruDashboard,
          viewManageTasks,
          viewOutOfServiceBeds,
        ),
      )
    }

    @Test
    fun `transformJpaToApi CAS1 should return version`() {
      val user = buildUserEntity(
        role = CAS1_JANITOR,
        apArea = ApAreaEntityFactory().produce(),
      )

      every { apAreaTransformer.transformJpaToApi(any()) } returns apArea

      val result =
        userTransformer.transformJpaToApi(user, approvedPremises) as ApprovedPremisesUser

      assertThat(result.version).isNotNull()
      assertThat(result.version).isEqualTo(-950936260)
    }

    @Test
    fun `transformJpaToApi CAS3 should return distinct roles for Temporary Accommodation`() {
      val user = buildUserEntity(CAS3_REFERRER)
      user.addRoleForUnitTest(CAS3_REFERRER)
      user.addRoleForUnitTest(CAS3_REFERRER)
      user.addRoleForUnitTest(CAS3_REFERRER)
      user.addRoleForUnitTest(CAS3_REPORTER)

      val result =
        userTransformer.transformJpaToApi(user, temporaryAccommodation) as TemporaryAccommodationUser

      assertThat(result.roles).isEqualTo(
        listOf(
          referrer,
          reporter,
        ),
      )
    }

    @Test
    fun `transformJpaToApi CAS1 should error if no ap area`() {
      val user = buildUserEntity(
        role = CAS1_MATCHER,
        apArea = null,
      )

      assertThatThrownBy {
        userTransformer.transformJpaToApi(user, approvedPremises)
      }.hasMessage("Internal Server Error: CAS1 user ${user.id} should have AP Area Set")
    }
  }

  @Nested
  inner class TransformJpaToApiUserWithWorkload {

    @Test
    fun `transformJpaToAPIUserWithWorkload should return distinct roles`() {
      val user = buildUserEntity(
        role = CAS1_MATCHER,
        apArea = ApAreaEntityFactory().produce(),
      )
      user.addRoleForUnitTest(CAS1_MATCHER)
      user.addRoleForUnitTest(CAS1_MATCHER)
      user.addRoleForUnitTest(CAS1_MATCHER)
      user.addRoleForUnitTest(CAS1_WORKFLOW_MANAGER)

      every { apAreaTransformer.transformJpaToApi(any()) } returns apArea

      val workload = UserWorkload(
        0,
        0,
        0,
      )

      val result = userTransformer.transformJpaToAPIUserWithWorkload(user, workload) as UserWithWorkload

      assertThat(result.roles).isEqualTo(
        listOf(
          matcher,
          workflowManager,
        ),
      )
    }

    @Test
    fun `transformJpaToAPIUserWithWorkload should return AP area`() {
      val apAreaEntity = ApAreaEntityFactory().produce()

      val user = buildUserEntity(
        role = CAS1_MATCHER,
        apArea = apAreaEntity,
      )

      val workload = UserWorkload(
        0,
        0,
        0,
      )

      every { apAreaTransformer.transformJpaToApi(apAreaEntity) } returns apArea

      val result = userTransformer.transformJpaToAPIUserWithWorkload(user, workload) as UserWithWorkload

      assertThat(result.apArea).isEqualTo(apArea)
    }
  }

  @Nested
  inner class TransformProfileResponseToApi {

    @Test
    fun `transformProfileResponseToApi Should successfully transfer user response when staff record not found`() {
      val result = userTransformer.transformProfileResponseToApi(
        "userName",
        UserService.GetUserResponse.StaffRecordNotFound,
        approvedPremises,
      )

      assertThat(result.deliusUsername).isEqualTo("userName")
      assertThat(result.user).isEqualTo(null)
      assertThat(result.loadError).isEqualTo(ProfileResponse.LoadError.staffRecordNotFound)
    }

    @Test
    fun `transformProfileResponseToApi Should successfully transfer user response when staff record found`() {
      val apAreaEntity = ApAreaEntityFactory().produce()

      val user = buildUserEntity(
        role = CAS1_MATCHER,
        apArea = apAreaEntity,
      )

      every { apAreaTransformer.transformJpaToApi(apAreaEntity) } returns apArea

      val result = userTransformer.transformProfileResponseToApi(
        "userName",
        UserService.GetUserResponse.Success(user),
        approvedPremises,
      )

      assertThat(result.deliusUsername).isEqualTo("userName")
      assertThat(result.loadError).isEqualTo(null)
      verify(exactly = 1) { userTransformer.transformJpaToApi(user, approvedPremises) }
    }
  }

  private fun buildUserEntity(
    role: UserRole,
    apArea: ApAreaEntity? = null,
    updatedAt: OffsetDateTime? = null,
  ) = UserEntityFactory()
    .withId(randomUUID())
    .withName("username")
    .withDeliusUsername("deliusUserName")
    .withEmail("someEmail")
    .withTelephoneNumber("someNumber")
    .withIsActive(true)
    .withProbationRegion(buildProbationRegionEntity())
    .withApArea(apArea)
    .withUpdatedAt(updatedAt)
    .produce()
    .addRoleForUnitTest(role)
    .addQualificationForUnitTest(WOMENS)

  private fun buildProbationRegionEntity() =
    ProbationRegionEntityFactory().withId(randomUUID()).withApArea(ApAreaEntityFactory().produce()).produce()
}
