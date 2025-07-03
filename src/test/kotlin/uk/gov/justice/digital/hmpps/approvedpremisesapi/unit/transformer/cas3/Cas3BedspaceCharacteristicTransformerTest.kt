package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BedspaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3BedspaceCharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3BedspaceCharacteristicTransformer

class Cas3BedspaceCharacteristicTransformerTest {

  private val transformer = Cas3BedspaceCharacteristicTransformer()

  @Test
  fun `transformJpaToApi transforms a Bedspace characteristic correctly`() {
    val characteristicEntity = Cas3BedspaceCharacteristicEntityFactory().produce()
    val result = transformer.transformJpaToApi(characteristicEntity)
    assertThat(result).isEqualTo(
      Cas3BedspaceCharacteristic(
        id = characteristicEntity.id,
        name = characteristicEntity.name,
        description = characteristicEntity.description,
      ),
    )
  }
}
