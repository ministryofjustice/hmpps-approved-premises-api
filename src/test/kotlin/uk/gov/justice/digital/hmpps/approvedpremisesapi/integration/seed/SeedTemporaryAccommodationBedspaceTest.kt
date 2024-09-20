package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Probation Region`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.TemporaryAccommodationBedspaceSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedTemporaryAccommodationBedspaceTest : SeedTestBase() {
  @Test
  fun `Attempting to create a Temporary Accommodation Bedspace with an invalid premises logs an error`() {
    withCsv(
      "invalid-ta-bedspace-premises-name",
      temporaryAccommodationBedspaceSeedCsvRowsToCsv(
        listOf(
          TemporaryAccommodationBedspaceSeedCsvRowFactory()
            .withPremisesName("Not a real premises")
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.temporaryAccommodationBedspace, "invalid-ta-bedspace-premises-name.csv")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Error on row 1:" &&
        it.throwable != null &&
        it.throwable.message == "Premises with reference 'Not a real premises' does not exist"
    }
  }

  @Test
  fun `Creating a new Temporary Accommodation Bedspace persists correctly`() {
    val probationRegion = `Given a Probation Region`()

    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

    val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
    }

    val csvRow = TemporaryAccommodationBedspaceSeedCsvRowFactory()
      .withPremisesName(premises.name)
      .withCharacteristics(listOf("Shared bathroom", "Shared kitchen"))
      .produce()

    withCsv(
      "new-ta-bedspace",
      temporaryAccommodationBedspaceSeedCsvRowsToCsv(
        listOf(
          csvRow,
        ),
      ),
    )

    seedService.seedData(SeedFileType.temporaryAccommodationBedspace, "new-ta-bedspace.csv")

    val persistedPremises = temporaryAccommodationPremisesRepository.findByName(premises.name)
    assertThat(persistedPremises).isNotNull
    assertThat(persistedPremises!!.rooms.size).isEqualTo(1)
    val room = persistedPremises.rooms.first()
    assertThat(room.name).isEqualTo(csvRow.bedspaceName)
    assertThat(room.characteristics.map { it.name }).containsExactlyInAnyOrder(*csvRow.characteristics.toTypedArray())
    assertThat(room.notes).isEqualTo(csvRow.notes)
    assertThat(room.beds.size).isEqualTo(1)
  }

  @Test
  fun `Updating an existing Temporary Accommodation Bedspace persists correctly`() {
    val originalProbationRegion = `Given a Probation Region`()

    val originalLocalAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

    val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(originalProbationRegion)
      withLocalAuthorityArea(originalLocalAuthorityArea)
    }

    val existingRoom = roomEntityFactory.produceAndPersist {
      withYieldedPremises { premises }
      withName("existing-ta-bedspace")
    }

    bedEntityFactory.produceAndPersist {
      withName("default-bed")
      withYieldedRoom { existingRoom }
    }

    val csvRow = TemporaryAccommodationBedspaceSeedCsvRowFactory()
      .withPremisesName(premises.name)
      .withBedspaceName(existingRoom.name)
      .withCharacteristics(listOf("Shared kitchen"))
      .produce()

    withCsv(
      "update-ta-bedspace",
      temporaryAccommodationBedspaceSeedCsvRowsToCsv(
        listOf(
          csvRow,
        ),
      ),
    )

    seedService.seedData(SeedFileType.temporaryAccommodationBedspace, "update-ta-bedspace.csv")

    val persistedPremises = temporaryAccommodationPremisesRepository.findByName(premises.name)
    assertThat(persistedPremises).isNotNull
    assertThat(persistedPremises!!.rooms.size).isEqualTo(1)
    val room = persistedPremises.rooms.first()
    assertThat(room.name).isEqualTo(csvRow.bedspaceName)
    assertThat(room.characteristics.map { it.name }.sorted()).isEqualTo(csvRow.characteristics.sorted())
    assertThat(room.notes).isEqualTo(csvRow.notes)
    assertThat(room.beds.size).isEqualTo(1)
  }

  private fun temporaryAccommodationBedspaceSeedCsvRowsToCsv(rows: List<TemporaryAccommodationBedspaceSeedCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "Property reference",
        "Bedspace reference",
        // Sample of characteristics
        "Shared bathroom",
        "Shared kitchen",
        // Sample of characteristics
        "Optional notes about the bedspace",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.premisesName)
        .withQuotedField(it.bedspaceName)
        .withUnquotedField(it.characteristics.contains("Shared bathroom").toString().uppercase())
        .withUnquotedField(it.characteristics.contains("Shared kitchen").toString().uppercase())
        .withQuotedField(it.notes ?: "")
        .newRow()
    }

    return builder.build()
  }
}

class TemporaryAccommodationBedspaceSeedCsvRowFactory : Factory<TemporaryAccommodationBedspaceSeedCsvRow> {
  private var premisesName: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var bedspaceName: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var characteristics: Yielded<List<String>> = { listOf() }
  private var notes: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }

  fun withPremisesName(premisesName: String) = apply {
    this.premisesName = { premisesName }
  }

  fun withBedspaceName(bedspaceName: String) = apply {
    this.bedspaceName = { bedspaceName }
  }

  fun withCharacteristics(characteristics: List<String>) = apply {
    this.characteristics = { characteristics }
  }

  fun withNotes(notes: String) = apply {
    this.notes = { notes }
  }

  override fun produce() = TemporaryAccommodationBedspaceSeedCsvRow(
    premisesName = this.premisesName(),
    bedspaceName = this.bedspaceName(),
    characteristics = this.characteristics(),
    notes = this.notes(),
  )
}
