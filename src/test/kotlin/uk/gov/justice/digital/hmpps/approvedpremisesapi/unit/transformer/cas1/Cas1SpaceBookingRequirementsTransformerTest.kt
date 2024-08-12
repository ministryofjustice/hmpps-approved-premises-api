package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingRequirementsTransformer

@ExtendWith(MockKExtension::class)
class Cas1SpaceBookingRequirementsTransformerTest {
  @InjectMockKs
  private lateinit var transformer: Cas1SpaceBookingRequirementsTransformer

  @Test
  fun `Placement requirements are transformed correctly`() {
    val cas1SpaceCharacteristics = Cas1SpaceCharacteristic.entries.map { it.toCharacteristicEntity() }

    val placementRequirements = PlacementRequirementsEntityFactory()
      .withDefaults()
      .withEssentialCriteria(cas1SpaceCharacteristics)
      .withDesirableCriteria(cas1SpaceCharacteristics)
      .produce()

    val result = transformer.transformJpaToApi(placementRequirements)

    assertThat(result.apType).isEqualTo(placementRequirements.apType)
    assertThat(result.gender).isEqualTo(placementRequirements.gender)
    assertThat(result.desirableCharacteristics).isEqualTo(Cas1SpaceCharacteristic.entries)
    assertThat(result.essentialCharacteristics).isEqualTo(Cas1SpaceCharacteristic.entries)
  }

  private fun Cas1SpaceCharacteristic.toCharacteristicEntity() = CharacteristicEntityFactory()
    .withName(this.value)
    .withPropertyName(this.value)
    .withServiceScope(ServiceName.approvedPremises.value)
    .withModelScope("*")
    .produce()
}
