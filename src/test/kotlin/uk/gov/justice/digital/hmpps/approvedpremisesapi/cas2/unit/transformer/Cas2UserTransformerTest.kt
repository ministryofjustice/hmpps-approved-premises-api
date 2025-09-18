package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.transformer

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.Cas2UserTransformer

class Cas2UserTransformerTest {
  private val cas2UserTransformer = Cas2UserTransformer()

  @Test
  fun `transforms JPA Cas2User db entity to Cas2User api representation`() {
    val jpaEntity = Cas2UserEntityFactory().produce()

    val expectedRepresentation = Cas2User(
      id = jpaEntity.id,
      username = jpaEntity.username,
      externalOrigin = jpaEntity.externalOrigin,
      name = jpaEntity.name,
      email = jpaEntity.email,
      deliusStaffCode = jpaEntity.deliusStaffCode,
      deliusTeamCodes = jpaEntity.deliusTeamCodes,
      nomisAccountType = jpaEntity.nomisAccountType,
      nomisStaffId = jpaEntity.nomisStaffId,
      isEnabled = jpaEntity.isEnabled,
      isActive = jpaEntity.isActive,
      userType = jpaEntity.userType,
      activeNomisCaseloadId = jpaEntity.activeNomisCaseloadId,
      applications = jpaEntity.applications,
    )

    val transformation = cas2UserTransformer.transformJpaToApi(jpaEntity)

    Assertions.assertThat(transformation).isEqualTo(expectedRepresentation)
  }
}
