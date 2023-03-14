package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer

class CharacteristicTransformerTest {
  private val characteristicTransformer = CharacteristicTransformer()

  @Test
  fun `transformToApi transforms correctly - includes propertyName`() {
    val characteristicEntityFactory = CharacteristicEntityFactory()

    val characteristic = characteristicEntityFactory
      .withPropertyName("isCatered")
      .withName("Is this AP catered?")
      .withModelScope("premises")
      .withServiceScope(ServiceName.approvedPremises.value)
      .produce()

    val result = characteristicTransformer.transformJpaToApi(characteristic)

    assertThat(result.propertyName).isEqualTo("isCatered")
    assertThat(result.name).isEqualTo("Is this AP catered?")
    assertThat(result.modelScope.value).isEqualTo("premises")
    assertThat(result.serviceScope.value).isEqualTo("approved-premises")
  }
}
