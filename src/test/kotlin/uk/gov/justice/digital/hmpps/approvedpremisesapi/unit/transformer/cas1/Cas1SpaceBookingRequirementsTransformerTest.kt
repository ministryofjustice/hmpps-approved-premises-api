package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingRequirementsTransformer

@ExtendWith(MockKExtension::class)
class Cas1SpaceBookingRequirementsTransformerTest {
  @InjectMockKs
  private lateinit var transformer: Cas1SpaceBookingRequirementsTransformer

  @Test
  fun `Placement requirements are transformed correctly`() {
    val cas1EssentialSpaceCharacteristics = Cas1SpaceBookingCharacteristic.entries.map { it.toCharacteristicEntity() }

    val spaceBooking = Cas1SpaceBookingEntityFactory()
      .withCriteria(cas1EssentialSpaceCharacteristics.toMutableList())
      .produce()

    val result = transformer.transformJpaToApi(spaceBooking)

    assertThat(result.essentialCharacteristics).isEqualTo(Cas1SpaceBookingCharacteristic.entries)
  }

  private fun Cas1SpaceBookingCharacteristic.toCharacteristicEntity() = CharacteristicEntityFactory()
    .withName(this.value)
    .withPropertyName(this.value)
    .withServiceScope(ServiceName.approvedPremises.value)
    .withModelScope("*")
    .produce()
}
