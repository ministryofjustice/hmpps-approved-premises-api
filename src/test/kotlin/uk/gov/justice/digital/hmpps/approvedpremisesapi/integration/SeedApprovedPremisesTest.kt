package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.ApprovedPremisesSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedApprovedPremisesTest : SeedTestBase() {
  @Test
  fun `Attempting to create an Approved Premises with an invalid Probation Region logs an error`() {
    withCsv(
      "invalid-probation",
      approvedPremisesSeedCsvRowsToCsv(
        listOf(
          ApprovedPremisesSeedCsvRowFactory()
            .withProbationRegion("Not Real Region")
            .produce()
        )
      )
    )

    seedService.seedData(SeedFileType.approvedPremises, "invalid-probation")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Seed Job" &&
        it.throwable != null &&
        it.throwable.cause!!.message == "Probation Region Not Real Region does not exist"
    }
  }

  @Test
  fun `Attempting to create an Approved Premises with an invalid Local Authority Area logs an error`() {
    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withApArea(apAreaEntityFactory.produceAndPersist())
    }

    withCsv(
      "invalid-local-authority",
      approvedPremisesSeedCsvRowsToCsv(
        listOf(
          ApprovedPremisesSeedCsvRowFactory()
            .withProbationRegion(probationRegion.name)
            .withLocalAuthorityArea("Not Real Authority")
            .produce()
        )
      )
    )

    seedService.seedData(SeedFileType.approvedPremises, "invalid-local-authority")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Seed Job" &&
        it.throwable != null &&
        it.throwable.cause!!.message == "Local Authority Area Not Real Authority does not exist"
    }
  }

  @Test
  fun `Attempting to create an Approved Premises with an invalid characteristic logs an error`() {
    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withApArea(apAreaEntityFactory.produceAndPersist())
    }

    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

    withCsv(
      "invalid-characteristic",
      approvedPremisesSeedCsvRowsToCsv(
        listOf(
          ApprovedPremisesSeedCsvRowFactory()
            .withProbationRegion(probationRegion.name)
            .withLocalAuthorityArea(localAuthorityArea.name)
            .withCharacteristics(listOf("Not Real Characteristic"))
            .produce()
        )
      )
    )

    seedService.seedData(SeedFileType.approvedPremises, "invalid-characteristic")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Seed Job" &&
        it.throwable != null &&
        it.throwable.cause!!.message == "Characteristic Not Real Characteristic does not exist"
    }
  }

  @Test
  fun `Attempting to create an Approved Premises with an incorrectly service-scoped characteristic logs an error`() {
    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withApArea(apAreaEntityFactory.produceAndPersist())
    }

    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

    val characteristic = characteristicEntityFactory.produceAndPersist {
      withId(UUID.fromString("8e04628f-2cdd-4d9a-8ae7-27689d7daa73"))
      withServiceScope("temporary-accommodation")
    }

    withCsv(
      "invalid-service-scope",
      approvedPremisesSeedCsvRowsToCsv(
        listOf(
          ApprovedPremisesSeedCsvRowFactory()
            .withProbationRegion(probationRegion.name)
            .withLocalAuthorityArea(localAuthorityArea.name)
            .withCharacteristics(listOf(characteristic.name))
            .produce()
        )
      )
    )

    seedService.seedData(SeedFileType.approvedPremises, "invalid-service-scope")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Seed Job" &&
        it.throwable != null &&
        it.throwable.cause!!.message == "Service scope does not match for Characteristic ${characteristic.id}"
    }
  }

  @Test
  fun `Attempting to create an Approved Premises with an incorrectly model-scoped characteristic logs an error`() {
    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withApArea(apAreaEntityFactory.produceAndPersist())
    }

    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

    val characteristic = characteristicEntityFactory.produceAndPersist {
      withId(UUID.fromString("8e04628f-2cdd-4d9a-8ae7-27689d7daa73"))
      withServiceScope("approved-premises")
      withModelScope("booking")
    }

    withCsv(
      "invalid-model-scope",
      approvedPremisesSeedCsvRowsToCsv(
        listOf(
          ApprovedPremisesSeedCsvRowFactory()
            .withProbationRegion(probationRegion.name)
            .withLocalAuthorityArea(localAuthorityArea.name)
            .withCharacteristics(listOf(characteristic.name))
            .produce()
        )
      )
    )

    seedService.seedData(SeedFileType.approvedPremises, "invalid-model-scope")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Seed Job" &&
        it.throwable != null &&
        it.throwable.cause!!.message == "Model scope does not match for Characteristic ${characteristic.id}"
    }
  }

  @Test
  fun `Creating a new Approved Premises persists correctly`() {
    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withApArea(apAreaEntityFactory.produceAndPersist())
    }

    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

    val characteristic = characteristicEntityFactory.produceAndPersist {
      withId(UUID.fromString("8e04628f-2cdd-4d9a-8ae7-27689d7daa73"))
      withServiceScope("approved-premises")
      withModelScope("premises")
    }

    val csvRow = ApprovedPremisesSeedCsvRowFactory()
      .withId(UUID.randomUUID())
      .withProbationRegion(probationRegion.name)
      .withLocalAuthorityArea(localAuthorityArea.name)
      .withCharacteristics(listOf(characteristic.name))
      .produce()

    withCsv(
      "new-ap",
      approvedPremisesSeedCsvRowsToCsv(
        listOf(
          csvRow
        )
      )
    )

    seedService.seedData(SeedFileType.approvedPremises, "new-ap")

    val persistedApprovedPremises = approvedPremisesRepository.findByIdOrNull(csvRow.id)
    assertThat(persistedApprovedPremises).isNotNull
    assertThat(persistedApprovedPremises!!.id).isEqualTo(csvRow.id)
    assertThat(persistedApprovedPremises.apCode).isEqualTo(csvRow.apCode)
    assertThat(persistedApprovedPremises.name).isEqualTo(csvRow.name)
    assertThat(persistedApprovedPremises.addressLine1).isEqualTo(csvRow.addressLine1)
    assertThat(persistedApprovedPremises.postcode).isEqualTo(csvRow.postcode)
    assertThat(persistedApprovedPremises.totalBeds).isEqualTo(csvRow.totalBeds)
    assertThat(persistedApprovedPremises.notes).isEqualTo(csvRow.notes)
    assertThat(persistedApprovedPremises.probationRegion.name).isEqualTo(csvRow.probationRegion)
    assertThat(persistedApprovedPremises.localAuthorityArea!!.name).isEqualTo(csvRow.localAuthorityArea)
    assertThat(persistedApprovedPremises.characteristics.map { it.name }).isEqualTo(csvRow.characteristics)
    assertThat(persistedApprovedPremises.status).isEqualTo(csvRow.status)
  }

  @Test
  fun `Updating an existing Approved Premises persists correctly`() {
    val originalProbationRegion = probationRegionEntityFactory.produceAndPersist {
      withApArea(apAreaEntityFactory.produceAndPersist())
    }

    val updatedProbationRegion = probationRegionEntityFactory.produceAndPersist {
      withApArea(apAreaEntityFactory.produceAndPersist())
    }

    val originalLocalAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

    val updatedLocalAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

    val existingApprovedPremises = approvedPremisesEntityFactory.produceAndPersist {
      withId(UUID.randomUUID())
      withProbationRegion(originalProbationRegion)
      withLocalAuthorityArea(originalLocalAuthorityArea)
      withQCode("OLD Q-CODE")
    }

    val csvRow = ApprovedPremisesSeedCsvRowFactory()
      .withId(existingApprovedPremises.id)
      .withProbationRegion(updatedProbationRegion.name)
      .withLocalAuthorityArea(updatedLocalAuthorityArea.name)
      .withCharacteristics(emptyList())
      .produce()

    withCsv(
      "update-ap",
      approvedPremisesSeedCsvRowsToCsv(
        listOf(
          csvRow
        )
      )
    )

    seedService.seedData(SeedFileType.approvedPremises, "update-ap")

    val persistedApprovedPremises = approvedPremisesRepository.findByIdOrNull(csvRow.id)
    assertThat(persistedApprovedPremises).isNotNull
    assertThat(persistedApprovedPremises!!.id).isEqualTo(csvRow.id)
    assertThat(persistedApprovedPremises.apCode).isEqualTo(csvRow.apCode)
    assertThat(persistedApprovedPremises.name).isEqualTo(csvRow.name)
    assertThat(persistedApprovedPremises.addressLine1).isEqualTo(csvRow.addressLine1)
    assertThat(persistedApprovedPremises.postcode).isEqualTo(csvRow.postcode)
    assertThat(persistedApprovedPremises.totalBeds).isEqualTo(csvRow.totalBeds)
    assertThat(persistedApprovedPremises.notes).isEqualTo(csvRow.notes)
    assertThat(persistedApprovedPremises.probationRegion.name).isEqualTo(csvRow.probationRegion)
    assertThat(persistedApprovedPremises.localAuthorityArea!!.name).isEqualTo(csvRow.localAuthorityArea)
    assertThat(persistedApprovedPremises.characteristics.map { it.name }).isEqualTo(csvRow.characteristics)
    assertThat(persistedApprovedPremises.status).isEqualTo(csvRow.status)
  }

  private fun approvedPremisesSeedCsvRowsToCsv(rows: List<ApprovedPremisesSeedCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "id",
        "name",
        "addressLine1",
        "postcode",
        "totalBeds",
        "notes",
        "probationRegion",
        "localAuthorityArea",
        "characteristics",
        "status",
        "apCode",
        "qCode"
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.id)
        .withQuotedField(it.name)
        .withQuotedField(it.addressLine1)
        .withQuotedField(it.postcode)
        .withUnquotedField(it.totalBeds)
        .withQuotedFields(it.notes)
        .withQuotedField(it.probationRegion)
        .withQuotedField(it.localAuthorityArea)
        .withQuotedField(it.characteristics.joinToString(","))
        .withQuotedField(it.status.value)
        .withQuotedField(it.apCode)
        .withQuotedField(it.qCode)
        .newRow()
    }

    return builder.build()
  }
}

class ApprovedPremisesSeedCsvRowFactory : Factory<ApprovedPremisesSeedCsvRow> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var addressLine1: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var postcode: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var totalBeds: Yielded<Int> = { randomInt(5, 50) }
  private var notes: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var probationRegion: Yielded<String> = { randomStringMultiCaseWithNumbers(5) }
  private var localAuthorityArea: Yielded<String> = { randomStringMultiCaseWithNumbers(5) }
  private var characteristics: Yielded<List<String>> = { listOf() }
  private var status: Yielded<PropertyStatus> = { PropertyStatus.active }
  private var apCode: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var qCode: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withAddressLine1(addressLine1: String) = apply {
    this.addressLine1 = { addressLine1 }
  }

  fun withPostcode(postcode: String) = apply {
    this.postcode = { postcode }
  }

  fun withTotalBeds(totalBeds: Int) = apply {
    this.totalBeds = { totalBeds }
  }

  fun withNotes(notes: String) = apply {
    this.notes = { notes }
  }

  fun withProbationRegion(probationRegion: String) = apply {
    this.probationRegion = { probationRegion }
  }

  fun withLocalAuthorityArea(localAuthorityArea: String) = apply {
    this.localAuthorityArea = { localAuthorityArea }
  }

  fun withCharacteristics(characteristics: List<String>) = apply {
    this.characteristics = { characteristics }
  }

  fun withStatus(status: PropertyStatus) = apply {
    this.status = { status }
  }

  fun withApCode(apCode: String) = apply {
    this.apCode = { apCode }
  }

  fun withQCode(qCode: String) = apply {
    this.qCode = { qCode }
  }

  override fun produce() = ApprovedPremisesSeedCsvRow(
    id = this.id(),
    name = this.name(),
    addressLine1 = this.addressLine1(),
    postcode = this.postcode(),
    totalBeds = this.totalBeds(),
    notes = this.notes(),
    probationRegion = this.probationRegion(),
    localAuthorityArea = this.localAuthorityArea(),
    characteristics = this.characteristics(),
    status = this.status(),
    apCode = this.apCode(),
    qCode = this.qCode()
  )
}
