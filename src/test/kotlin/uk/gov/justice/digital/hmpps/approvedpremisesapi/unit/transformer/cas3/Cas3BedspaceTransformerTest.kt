package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3BedspaceStatus
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
        status = Cas3BedspaceStatus.online,
        notes = room.notes,
        characteristics = emptyList(),
      ),
    )
  }
}
