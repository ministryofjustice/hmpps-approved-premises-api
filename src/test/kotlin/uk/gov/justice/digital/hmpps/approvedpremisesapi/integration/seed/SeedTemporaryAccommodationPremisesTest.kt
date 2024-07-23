package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.TemporaryAccommodationPremisesSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedTemporaryAccommodationPremisesTest : SeedTestBase() {
  @Test
  fun `Attempting to create a Temporary Accommodation Premises with an invalid Probation Region logs an error`() {
    withCsv(
      "invalid-probation-ta",
      temporaryAccommodationPremisesSeedCsvRowsToCsv(
        listOf(
          TemporaryAccommodationPremisesSeedCsvRowFactory()
            .withProbationRegion("Not Real Region")
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.temporaryAccommodationPremises, "invalid-probation-ta")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Error on row 1:" &&
        it.throwable != null &&
        it.throwable.message == "Probation Region Not Real Region does not exist"
    }
  }

  @Test
  fun `Attempting to create a Temporary Accommodation Premises with an invalid Local Authority Area logs an error`() {
    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withApArea(apAreaEntityFactory.produceAndPersist())
    }

    withCsv(
      "invalid-local-authority-ta",
      temporaryAccommodationPremisesSeedCsvRowsToCsv(
        listOf(
          TemporaryAccommodationPremisesSeedCsvRowFactory()
            .withProbationRegion(probationRegion.name)
            .withLocalAuthorityArea("Not Real Authority")
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.temporaryAccommodationPremises, "invalid-local-authority-ta")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Error on row 1:" &&
        it.throwable != null &&
        it.throwable.message == "Local Authority Area Not Real Authority does not exist"
    }
  }

  @Test
  fun `Creating a new Temporary Accommodation Premises persists correctly`() {
    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withApArea(apAreaEntityFactory.produceAndPersist())
    }

    val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

    val csvRow = TemporaryAccommodationPremisesSeedCsvRowFactory()
      .withProbationRegion(probationRegion.name)
      .withLocalAuthorityArea(localAuthorityArea.name)
      .withPdu(probationDeliveryUnit.name)
      .withCharacteristics(listOf("Park nearby", "Pub nearby"))
      .produce()

    withCsv(
      "new-ta-premises",
      temporaryAccommodationPremisesSeedCsvRowsToCsv(
        listOf(
          csvRow,
        ),
      ),
    )

    seedService.seedData(SeedFileType.temporaryAccommodationPremises, "new-ta-premises")

    val persistedPremises = temporaryAccommodationPremisesRepository.findByName(csvRow.name)
    assertThat(persistedPremises).isNotNull
    assertThat(persistedPremises!!.addressLine1).isEqualTo(csvRow.addressLine1)
    assertThat(persistedPremises.addressLine2).isEqualTo(csvRow.addressLine2)
    assertThat(persistedPremises.town).isEqualTo(csvRow.town)
    assertThat(persistedPremises.postcode).isEqualTo(csvRow.postcode)
    assertThat(persistedPremises.probationRegion.name).isEqualTo(csvRow.probationRegion)
    assertThat(persistedPremises.localAuthorityArea!!.name).isEqualTo(csvRow.localAuthorityArea)
    assertThat(persistedPremises.probationDeliveryUnit!!.name).isEqualTo(csvRow.pdu)
    assertThat(persistedPremises.characteristics.map { it.name }).isEqualTo(csvRow.characteristics)
    assertThat(persistedPremises.notes).isEqualTo(csvRow.notes)
  }

  @Test
  fun `Updating an existing Temporary Accommodation Premises persists correctly`() {
    val originalProbationRegion = probationRegionEntityFactory.produceAndPersist {
      withApArea(apAreaEntityFactory.produceAndPersist())
    }

    val originalProbationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(originalProbationRegion)
    }

    val updatedProbationRegion = probationRegionEntityFactory.produceAndPersist {
      withApArea(apAreaEntityFactory.produceAndPersist())
    }

    val updateProbationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(updatedProbationRegion)
    }

    val originalLocalAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

    val updatedLocalAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

    val existingPremises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withName("ta-premises-to-update")
      withProbationRegion(originalProbationRegion)
      withLocalAuthorityArea(originalLocalAuthorityArea)
      withProbationDeliveryUnit(originalProbationDeliveryUnit)
    }

    val csvRow = TemporaryAccommodationPremisesSeedCsvRowFactory()
      .withName(existingPremises.name)
      .withProbationRegion(updatedProbationRegion.name)
      .withLocalAuthorityArea(updatedLocalAuthorityArea.name)
      .withPdu(updateProbationDeliveryUnit.name)
      .withCharacteristics(listOf("Park nearby"))
      .produce()

    withCsv(
      "update-ta-premises",
      temporaryAccommodationPremisesSeedCsvRowsToCsv(
        listOf(
          csvRow,
        ),
      ),
    )

    seedService.seedData(SeedFileType.temporaryAccommodationPremises, "update-ta-premises")

    val persistedPremises = temporaryAccommodationPremisesRepository.findByName(csvRow.name)
    assertThat(persistedPremises).isNotNull
    assertThat(persistedPremises!!.addressLine1).isEqualTo(csvRow.addressLine1)
    assertThat(persistedPremises.addressLine2).isEqualTo(csvRow.addressLine2)
    assertThat(persistedPremises.town).isEqualTo(csvRow.town)
    assertThat(persistedPremises.postcode).isEqualTo(csvRow.postcode)
    assertThat(persistedPremises.probationRegion.name).isEqualTo(csvRow.probationRegion)
    assertThat(persistedPremises.localAuthorityArea!!.name).isEqualTo(csvRow.localAuthorityArea)
    assertThat(persistedPremises.probationDeliveryUnit!!.name).isEqualTo(csvRow.pdu)
    assertThat(persistedPremises.characteristics.map { it.name }).isEqualTo(csvRow.characteristics)
    assertThat(persistedPremises.notes).isEqualTo(csvRow.notes)
  }

  private fun temporaryAccommodationPremisesSeedCsvRowsToCsv(rows: List<TemporaryAccommodationPremisesSeedCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "Property reference",
        "Address Line 1",
        "Address Line 2 (optional)",
        "City/Town",
        "Postcode",
        "Region",
        "Local authority / Borough",
        "Probation delivery unit (PDU)",
        // Sample of characteristics
        "Pub nearby",
        "Park nearby",
        "School nearby",
        // Sample of characteristics
        "Optional notes",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.name)
        .withQuotedField(it.addressLine1)
        .withQuotedField(it.addressLine2 ?: "")
        .withQuotedField(it.town ?: "")
        .withQuotedField(it.postcode)
        .withQuotedField(it.probationRegion)
        .withQuotedField(it.localAuthorityArea)
        .withQuotedField(it.pdu)
        .withUnquotedField(it.characteristics.contains("Pub nearby").toString().uppercase())
        .withUnquotedField(it.characteristics.contains("Park nearby").toString().uppercase())
        .withUnquotedField(it.characteristics.contains("School nearby").toString().uppercase())
        .withQuotedField(it.notes)
        .newRow()
    }

    return builder.build()
  }
}

class TemporaryAccommodationPremisesSeedCsvRowFactory : Factory<TemporaryAccommodationPremisesSeedCsvRow> {
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var addressLine1: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var addressLine2: Yielded<String?> = { randomStringMultiCaseWithNumbers(10) }
  private var town: Yielded<String?> = { randomStringMultiCaseWithNumbers(10) }
  private var postcode: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var probationRegion: Yielded<String> = { randomStringMultiCaseWithNumbers(5) }
  private var localAuthorityArea: Yielded<String> = { randomStringMultiCaseWithNumbers(5) }
  private var pdu: Yielded<String> = { randomStringMultiCaseWithNumbers(5) }
  private var characteristics: Yielded<List<String>> = { listOf() }
  private var notes: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var emailAddress: Yielded<String?> = { randomStringMultiCaseWithNumbers(10) }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withAddressLine1(addressLine1: String) = apply {
    this.addressLine1 = { addressLine1 }
  }

  fun withAddressLine2(addressLine2: String?) = apply {
    this.addressLine2 = { addressLine2 }
  }

  fun withTown(town: String?) = apply {
    this.town = { town }
  }

  fun withPostcode(postcode: String) = apply {
    this.postcode = { postcode }
  }

  fun withProbationRegion(probationRegion: String) = apply {
    this.probationRegion = { probationRegion }
  }

  fun withLocalAuthorityArea(localAuthorityArea: String) = apply {
    this.localAuthorityArea = { localAuthorityArea }
  }

  fun withPdu(pdu: String) = apply {
    this.pdu = { pdu }
  }

  fun withCharacteristics(characteristics: List<String>) = apply {
    this.characteristics = { characteristics }
  }

  fun withNotes(notes: String) = apply {
    this.notes = { notes }
  }

  override fun produce() = TemporaryAccommodationPremisesSeedCsvRow(
    name = this.name(),
    addressLine1 = this.addressLine1(),
    addressLine2 = this.addressLine2(),
    town = this.town(),
    postcode = this.postcode(),
    probationRegion = this.probationRegion(),
    localAuthorityArea = this.localAuthorityArea(),
    pdu = this.pdu(),
    characteristics = this.characteristics(),
    notes = this.notes(),
    emailAddress = this.emailAddress(),
  )
}
