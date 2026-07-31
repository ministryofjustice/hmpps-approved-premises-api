package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1CharacteristicTransformer

class Cas1CharacteristicTransformerTest {
  private val characteristicTransformer = Cas1CharacteristicTransformer()

  @Test
  fun `transformToApi transforms correctly - includes propertyName`() {
    val characteristicEntityFactory = Cas1CharacteristicEntityFactory()

    val characteristic = characteristicEntityFactory
      .withPropertyName("isCatered")
      .withName("Is this AP catered?")
      .withModelScope("premises")
      .produce()

    val result = characteristicTransformer.transformJpaToApi(characteristic)

    assertThat(result.propertyName).isEqualTo("isCatered")
    assertThat(result.name).isEqualTo("Is this AP catered?")
    assertThat(result.modelScope.value).isEqualTo("premises")
    assertThat(result.serviceScope.value).isEqualTo("approved-premises")
  }
}
