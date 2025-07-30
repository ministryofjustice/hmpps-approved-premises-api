package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BedspaceCharacteristicTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BedspaceTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate

class Cas3BedspaceTransformerTest {

  private val characteristicTransformer = CharacteristicTransformer()
  private val cas3BedspaceCharacteristicTransformer = Cas3BedspaceCharacteristicTransformer()
  private val cas3BedspaceTransformer = Cas3BedspaceTransformer(characteristicTransformer, cas3BedspaceCharacteristicTransformer)

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
        startDate = bed.startDate!!,
        endDate = bed.endDate,
        status = Cas3BedspaceStatus.online,
        notes = room.notes,
        characteristics = emptyList(),
      ),
    )
  }

  @Test
  fun `transformJpaToApi transforms the BedspaceEntity into Cas3Bedspace correctly`() {
    val premises = Cas3PremisesEntityFactory()
      .withDefaults()
      .produce()

    val bedspace = Cas3BedspaceEntityFactory()
      .withPremises(premises)
      .produce()

    val result = cas3BedspaceTransformer.transformJpaToApi(bedspace)

    assertThat(result).isEqualTo(
      Cas3Bedspace(
        id = bedspace.id,
        reference = bedspace.reference,
        startDate = bedspace.startDate!!,
        endDate = bedspace.endDate,
        notes = bedspace.notes,
        status = Cas3BedspaceStatus.online,
        bedspaceCharacteristics = bedspace.characteristics.map(cas3BedspaceCharacteristicTransformer::transformJpaToApi),
      ),
    )
  }
}
