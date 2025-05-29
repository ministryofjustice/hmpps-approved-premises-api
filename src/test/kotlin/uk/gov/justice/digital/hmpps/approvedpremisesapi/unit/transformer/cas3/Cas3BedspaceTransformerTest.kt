package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas3

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3BedspaceTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate

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

    println()

    val result = cas3BedspaceTransformer.transformJpaToApi(room)

    assertThat(result).isEqualTo(
      Cas3Bedspace(
        id = bed.id,
        reference = room.name,
        characteristics = room.characteristics.map(characteristicTransformer::transformJpaToApi),
      ),
    )
  }

  @Test
  fun `transformJpaToApi transforms the RoomEntity into a Cas3Bedspace with optional fields`() {
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .withCode("NEABC-1")
      .withName("Some name")
      .withNotes("Some notes")
      .produce()

    val bed = BedEntityFactory()
      .withCode("BED 1")
      .withName("BED A")
      .withStartDate(LocalDate.now())
      .withEndDate(LocalDate.now().plusDays(1))
      .withRoom(room)
      .produce()

    room.beds.add(bed)

    val result = cas3BedspaceTransformer.transformJpaToApi(room)

    assertThat(result).isEqualTo(
      Cas3Bedspace(
        id = bed.id,
        reference = room.name,
        startDate = bed.startDate,
        endDate = bed.endDate,
        notes = room.notes,
        characteristics = room.characteristics.map(characteristicTransformer::transformJpaToApi),
      ),
    )
  }

  @Test
  fun `transformJpaToApi throws error when no beds in room`() {
    val logger = mockk<Logger>()
    cas3BedspaceTransformer.log = logger

    every { logger.error(any<String>()) } returns Unit

    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .withCode("NEABC-1")
      .withName("Some name")
      .withNotes("This room is large")
      .produce()

    val result = cas3BedspaceTransformer.transformJpaToApi(room)

    assertThat(result).isNull()

    verify { logger.error(eq("No beds found for room ID ${room.id}.")) }
  }

  @Test
  fun `transformJpaToApi transforms the BedEntity into Cas3Bedspace correctly`() {
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .withName(randomStringMultiCaseWithNumbers(10))
      .withNotes(randomStringLowerCase(100))
      .produce()

    val bed = BedEntityFactory()
      .withName(randomStringMultiCaseWithNumbers(10))
      .withStartDate(LocalDate.now().minusDays(90))
      .withEndDate(LocalDate.now().plusDays(180))
      .withRoom(room)
      .produce()

    val result = cas3BedspaceTransformer.transformJpaToApi(bed)

    assertThat(result).isEqualTo(
      Cas3Bedspace(
        id = bed.id,
        reference = room.name,
        startDate = bed.startDate,
        endDate = bed.endDate,
        notes = room.notes,
        characteristics = emptyList(),
      ),
    )
  }
}
