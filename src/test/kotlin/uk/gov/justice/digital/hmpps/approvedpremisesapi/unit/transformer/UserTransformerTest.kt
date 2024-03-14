package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole.appealsManager
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole.matcher
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole.workflowManager
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName.approvedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName.temporaryAccommodation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUserRole.referrer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUserRole.reporter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserWithWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.WOMENS
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_MATCHER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_WORKFLOW_MANAGER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_REFERRER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_REPORTER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.UserWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationRegionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addQualificationForUnitTest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addRoleForUnitTest
import java.util.UUID.randomUUID

class UserTransformerTest {
  private val probationRegionTransformer = mockk<ProbationRegionTransformer>()
  private val apAreaTransformer = mockk<ApAreaTransformer>()

  private val userTransformer = UserTransformer(probationRegionTransformer, apAreaTransformer)

  private val apArea = ApArea(randomUUID(), "someIdentifier", "someName")

  @BeforeEach
  fun setup() {
    every { probationRegionTransformer.transformJpaToApi(any()) } returns ProbationRegion(randomUUID(), "someName")
    every { apAreaTransformer.transformJpaToApi(any()) } returns apArea
  }

  @Test
  fun `Should successfully transfer user entity with role CAS3_REPORTER to reporter`() {
    val result = userTransformer.transformJpaToApi(
      buildUserEntity(CAS3_REPORTER),
      temporaryAccommodation,
    ) as TemporaryAccommodationUser

    assertThat(result.roles).contains(reporter)
    assertThat(result.service).isEqualTo(temporaryAccommodation.value)
    verify(exactly = 1) { probationRegionTransformer.transformJpaToApi(any()) }
  }

  @Test
  fun `Should successfully transfer user entity with role CAS3_REFERRER to referrer`() {
    val result = userTransformer.transformJpaToApi(
      buildUserEntity(CAS3_REFERRER),
      temporaryAccommodation,
    ) as TemporaryAccommodationUser

    assertThat(result.roles).contains(referrer)
    assertThat(result.service).isEqualTo(temporaryAccommodation.value)
    verify(exactly = 1) { probationRegionTransformer.transformJpaToApi(any()) }
  }

  @Test
  fun `Should successfully transfer user entity with role CAS1_MATCHER to matcher`() {
    val result =
      userTransformer.transformJpaToApi(buildUserEntity(CAS1_MATCHER), approvedPremises) as ApprovedPremisesUser

    assertThat(result.roles).contains(matcher)
    assertThat(result.service).isEqualTo("CAS1")
    verify(exactly = 1) { probationRegionTransformer.transformJpaToApi(any()) }
  }

  @Test
  fun `should return distinct roles for Approved Premises`() {
    val user = buildUserEntity(CAS1_MATCHER)
    user.addRoleForUnitTest(CAS1_MATCHER)
    user.addRoleForUnitTest(CAS1_MATCHER)
    user.addRoleForUnitTest(CAS1_MATCHER)
    user.addRoleForUnitTest(CAS1_WORKFLOW_MANAGER)
    user.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

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

  @Test
  fun `should return distinct roles for Temporary Accommodation`() {
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
  fun `transformJpaToAPIUserWithWorkload should return distinct roles`() {
    val user = buildUserEntity(CAS1_MATCHER)
    user.addRoleForUnitTest(CAS1_MATCHER)
    user.addRoleForUnitTest(CAS1_MATCHER)
    user.addRoleForUnitTest(CAS1_MATCHER)
    user.addRoleForUnitTest(CAS1_WORKFLOW_MANAGER)

    val workload = UserWorkload(
      0,
      0,
      0,
    )

    val result =
      userTransformer.transformJpaToAPIUserWithWorkload(user, workload) as UserWithWorkload

    assertThat(result.roles).isEqualTo(
      listOf(
        matcher,
        workflowManager,
      ),
    )
  }

  @Test
  fun `transformJpaToAPIUserWithWorkload should return AP area`() {
    val user = buildUserEntity(CAS1_MATCHER)

    val workload = UserWorkload(
      0,
      0,
      0,
    )

    val result =
      userTransformer.transformJpaToAPIUserWithWorkload(user, workload) as UserWithWorkload

    assertThat(result.apArea).isEqualTo(apArea)

    verify {
      apAreaTransformer.transformJpaToApi(user.probationRegion.apArea)
    }
  }

  private fun buildUserEntity(
    role: UserRole,
  ) = UserEntityFactory()
    .withId(randomUUID())
    .withName("username")
    .withDeliusUsername("deliusUserName")
    .withEmail("someEmail")
    .withTelephoneNumber("someNumber")
    .withIsActive(true)
    .withProbationRegion(buildProbationRegionEntity())
    .produce()
    .addRoleForUnitTest(role)
    .addQualificationForUnitTest(WOMENS)

  private fun buildProbationRegionEntity() =
    ProbationRegionEntityFactory().withId(randomUUID()).withApArea(ApAreaEntityFactory().produce()).produce()
}
