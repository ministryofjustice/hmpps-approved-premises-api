package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole.matcher
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName.approvedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName.temporaryAccommodation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUserRole.referrer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUserRole.reporter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification.WOMENS
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_REFERRER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_REPORTER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationRegionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addQualificationForUnitTest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addRoleForUnitTest
import java.util.UUID.randomUUID

class UserTransformerTest {
  private val probationRegionTransformer = mockk<ProbationRegionTransformer>()
  private val userTransformer = UserTransformer(probationRegionTransformer)

  @BeforeEach
  fun setup() {
    every { probationRegionTransformer.transformJpaToApi(any()) } returns ProbationRegion(randomUUID(), "someName")
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
      userTransformer.transformJpaToApi(buildUserEntity(UserRole.CAS1_MATCHER), approvedPremises) as ApprovedPremisesUser

    assertThat(result.roles).contains(matcher)
    assertThat(result.service).isEqualTo(approvedPremises.value)
    verify(exactly = 1) { probationRegionTransformer.transformJpaToApi(any()) }
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
