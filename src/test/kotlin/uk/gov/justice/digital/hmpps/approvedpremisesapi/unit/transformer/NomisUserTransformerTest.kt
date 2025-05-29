package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NomisUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NomisUserTransformer

class NomisUserTransformerTest {
  private val nomisUserTransformer = NomisUserTransformer()

  @Test
  fun `transforms JPA NomisUser db entity to NomisUser api representation`() {
    val jpaEntity = NomisUserEntityFactory().withName("John Smith").produce()

    val expectedRepresentation = NomisUser(
      id = jpaEntity.id,
      nomisUsername = jpaEntity.nomisUsername,
      name = jpaEntity.name,
      email = jpaEntity.email,
      isActive = jpaEntity.isActive,
    )

    val transformation = nomisUserTransformer.transformJpaToApi(jpaEntity)

    assertThat(transformation).isEqualTo(expectedRepresentation)
  }

  @Test
  fun `transforms JPA Cas2User delius db entity to NomisUser api representation`() {
    val jpaEntity = Cas2UserEntityFactory()
      .withName("John Smith")
      .withUserType(Cas2UserType.DELIUS)
      .produce()

    val expectedRepresentation = NomisUser(
      id = jpaEntity.id,
      nomisUsername = jpaEntity.username,
      name = jpaEntity.name,
      email = jpaEntity.email,
      isActive = jpaEntity.isActive,
    )

    val transformation = nomisUserTransformer.transformJpaToApi(jpaEntity)

    assertThat(transformation).isEqualTo(expectedRepresentation)
  }
}
