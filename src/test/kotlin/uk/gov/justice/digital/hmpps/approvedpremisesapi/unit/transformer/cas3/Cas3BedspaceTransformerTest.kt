package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3BedspaceTransformer

class Cas3BedspaceTransformerTest {

  private val characteristicTransformer = CharacteristicTransformer()
  private val cas3BedspaceTransformer = Cas3BedspaceTransformer(characteristicTransformer)

  @Test
  fun `transformJpaToApi transforms the RoomEntity into a Cas3Bedspace without optional fields`() {
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .withCode("NEABC-1")
      .withName("Some name")
      .withNotes(null)
      .produce()

    val bed = BedEntityFactory()
      .withCode("BED 1")
      .withName("BED A")
      .withRoom(room)
      .produce()

    room.beds.add(bed)

    val result = cas3BedspaceTransformer.transformJpaToApi(room)

    assertThat(result).isEqualTo(
      Cas3Bedspace(
        id = bed.id,
        reference = room.name,
        startDate = bed.createdAt?.toLocalDate()!!,
        characteristics = room.characteristics.map(characteristicTransformer::transformJpaToApi),
      ),
    )
  }

  @Test
  fun `transformJpaToApi throws error when no beds in room`() {
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .withCode("NEABC-1")
      .withName("Some name")
      .withNotes("This room is large")
      .produce()

    val error = assertThrows<IllegalStateException> {
      cas3BedspaceTransformer.transformJpaToApi(room)
    }
    assertThat(error.message).isEqualTo("No beds in room ${room.id}.")
  }
}
