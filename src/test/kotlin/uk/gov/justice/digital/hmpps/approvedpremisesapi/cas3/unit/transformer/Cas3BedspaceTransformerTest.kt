package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BedspaceCharacteristicTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BedspaceTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.stream.Stream

class Cas3BedspaceTransformerTest {

  private val characteristicTransformer = CharacteristicTransformer()
  private val cas3BedspaceCharacteristicTransformer = Cas3BedspaceCharacteristicTransformer()
  private val cas3BedspaceTransformer = Cas3BedspaceTransformer(characteristicTransformer, cas3BedspaceCharacteristicTransformer)

  @ParameterizedTest
  @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.transformer.Cas3BedspaceTransformerTest#startDateAndStatusProvider")
  fun `transformJpaToApi transforms the BedEntity into Cas3Bedspace correctly`(startDate: LocalDate, status: Cas3BedspaceStatus) {
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
      .withStartDate(startDate)
      .withEndDate(startDate.plusDays(180))
      .withRoom(room)
      .withCreatedAt { OffsetDateTime.now().minusDays(100) }
      .produce()

    val result = cas3BedspaceTransformer.transformJpaToApi(bed)

    assertThat(result).isEqualTo(
      Cas3Bedspace(
        id = bed.id,
        reference = room.name,
        startDate = bed.createdAt!!.toLocalDate(),
        endDate = bed.endDate,
        status = status,
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
        startDate = bedspace.createdAt!!.toLocalDate(),
        endDate = bedspace.endDate,
        notes = bedspace.notes,
        status = Cas3BedspaceStatus.online,
        bedspaceCharacteristics = bedspace.characteristics.map(cas3BedspaceCharacteristicTransformer::transformJpaToApi),
      ),
    )
  }

  companion object {
    @JvmStatic
    fun startDateAndStatusProvider() = Stream.of(
      Arguments.of(LocalDate.now().minusDays(90).toString(), Cas3BedspaceStatus.online),
      Arguments.of(LocalDate.now().minusDays(300).toString(), Cas3BedspaceStatus.archived),
      Arguments.of(LocalDate.now().plusDays(7).toString(), Cas3BedspaceStatus.upcoming),
    )
  }
}
