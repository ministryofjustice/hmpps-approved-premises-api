package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NomisUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcNomisUserTransformer

class NomisUserTransformerTest {
  private val cas2HdcNomisUserTransformer = Cas2HdcNomisUserTransformer()

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

    val transformation = cas2HdcNomisUserTransformer.transformJpaToApi(jpaEntity)

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

    val transformation = cas2HdcNomisUserTransformer.transformJpaToApi(jpaEntity)

    assertThat(transformation).isEqualTo(expectedRepresentation)
  }

  fun `transforms JPA application to NomisUser api representation`() {
    val jpaEntity = Cas2UserEntityFactory()
      .withName("John Smith")
      .withUserType(Cas2UserType.DELIUS)
      .produce()

    val application = Cas2ApplicationEntityFactory().withCreatedByUser(jpaEntity).produce()

    val expectedRepresentation = NomisUser(
      id = jpaEntity.id,
      nomisUsername = jpaEntity.username,
      name = jpaEntity.name,
      email = jpaEntity.email,
      isActive = jpaEntity.isActive,
    )

    val transformation = cas2HdcNomisUserTransformer.transformJpaToApi(application)

    assertThat(transformation).isEqualTo(expectedRepresentation)
  }
}
