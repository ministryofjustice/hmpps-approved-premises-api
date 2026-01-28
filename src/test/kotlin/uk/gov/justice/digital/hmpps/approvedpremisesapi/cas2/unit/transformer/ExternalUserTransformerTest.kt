package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.transformer

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ExternalUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.ExternalUserTransformer

class ExternalUserTransformerTest {
  private val externalUserTransformer = ExternalUserTransformer()

  @Test
  fun `transforms JPA Cas2User db entity to ExternalUser api representation`() {
    val jpaEntity = Cas2UserEntityFactory()
      .withUserType(Cas2UserType.EXTERNAL)
      .produce()

    val expectedRepresentation = ExternalUser(
      id = jpaEntity.id,
      username = jpaEntity.username,
      origin = jpaEntity.externalType,
      name = jpaEntity.name,
      email = jpaEntity.email!!,
    )

    val transformation = externalUserTransformer.transformJpaToApi(jpaEntity)

    Assertions.assertThat(transformation).isEqualTo(expectedRepresentation)
  }
}
