package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Approved Premises Bed`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1OutOfServiceBedSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1OutOfServiceBedSeedCsvRowKey
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

class SeedCas1OutOfServiceBedTest : SeedTestBase() {
  @Test
  fun `Logs an error if the premises ID is not provided`() {
    seedWithCsv("no-premises-id") {
      withRow(
        bedId = UUID.randomUUID(),
        startDate = LocalDate.now(),
        endDate = LocalDate.now().plusDays(1),
        reasonId = UUID.randomUUID(),
        isCancelled = false,
      )
    }

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Seed Job" &&
        it.throwable != null &&
        it.throwable.message != null &&
        it.throwable.message!!.contains("Unable to deserialize CSV at row: 1: Invalid UUID string")
    }
  }

  @Test
  fun `Logs an error if no premises exists with the given ID`() {
    val row = Cas1OutOfServiceBedSeedCsvRowFactory().produce()

    seedWithCsv("no-premises") {
      withRow(row)
    }

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Error on row 1:" &&
        it.throwable != null &&
        it.throwable.message == "No Approved Premises with ID ${row.key.premisesId} exists."
    }
  }

  @Test
  fun `Logs an error if the bed ID is not provided`() {
    seedWithCsv("no-bed-id") {
      withRow(
        premisesId = UUID.randomUUID(),
        startDate = LocalDate.now(),
        endDate = LocalDate.now().plusDays(1),
        reasonId = UUID.randomUUID(),
        isCancelled = false,
      )
    }

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Seed Job" &&
        it.throwable != null &&
        it.throwable.message != null &&
        it.throwable.message!!.contains("Unable to deserialize CSV at row: 1: Invalid UUID string")
    }
  }

  @Test
  fun `Logs an error if no bed exists with the given ID`() {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist {
          withYieldedApArea {
            apAreaEntityFactory.produceAndPersist()
          }
        }
      }
      withYieldedLocalAuthorityArea {
        localAuthorityEntityFactory.produceAndPersist()
      }
    }

    val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

    val row = Cas1OutOfServiceBedSeedCsvRowFactory()
      .withPremisesId(premises.id)
      .withReasonId(reason.id)
      .produce()

    seedWithCsv("no-bed") {
      withRow(row)
    }

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Error on row 1:" &&
        it.throwable is BadRequestProblem &&
        it.throwable.invalidParams == ValidationErrors(mutableMapOf("$.bedId" to "doesNotExist"))
    }
  }

  @Test
  fun `Logs an error if the start date is not provided`() {
    seedWithCsv("no-start-date") {
      withRow(
        premisesId = UUID.randomUUID(),
        bedId = UUID.randomUUID(),
        endDate = LocalDate.now().plusDays(1),
        reasonId = UUID.randomUUID(),
        isCancelled = false,
      )
    }

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Seed Job" &&
        it.throwable != null &&
        it.throwable.message != null &&
        it.throwable.message!!.contains("Unable to deserialize CSV at row: 1: Text '' could not be parsed at index 0")
    }
  }

  @Test
  fun `Logs an error if the end date is not provided`() {
    seedWithCsv("no-end-date") {
      withRow(
        premisesId = UUID.randomUUID(),
        bedId = UUID.randomUUID(),
        startDate = LocalDate.now(),
        reasonId = UUID.randomUUID(),
        isCancelled = false,
      )
    }

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Seed Job" &&
        it.throwable != null &&
        it.throwable.message != null &&
        it.throwable.message!!.contains("Unable to deserialize CSV at row: 1: Text '' could not be parsed at index 0")
    }
  }

  @Test
  fun `Logs an error if the reason ID is not provided`() {
    seedWithCsv("no-reason-id") {
      withRow(
        premisesId = UUID.randomUUID(),
        bedId = UUID.randomUUID(),
        startDate = LocalDate.now(),
        endDate = LocalDate.now().plusDays(1),
        isCancelled = false,
      )
    }

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Seed Job" &&
        it.throwable != null &&
        it.throwable.message != null &&
        it.throwable.message!!.contains("Unable to deserialize CSV at row: 1: Invalid UUID string")
    }
  }

  @Test
  fun `Logs an error if no reason exists with the given ID`() {
    `Given an Approved Premises Bed` { bed ->
      val row = Cas1OutOfServiceBedSeedCsvRowFactory()
        .withPremisesId(bed.room.premises.id)
        .withBedId(bed.id)
        .produce()

      seedWithCsv("no-reason") {
        withRow(row)
      }

      assertThat(logEntries).anyMatch {
        it.level == "error" &&
          it.message == "Error on row 1:" &&
          it.throwable is BadRequestProblem &&
          it.throwable.invalidParams == ValidationErrors(mutableMapOf("$.reason" to "doesNotExist"))
      }
    }
  }

  @Test
  fun `Creates an out-of-service bed with the correct data`() {
    `Given an Approved Premises Bed` { bed ->
      val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

      val row = Cas1OutOfServiceBedSeedCsvRowFactory()
        .withPremisesId(bed.room.premises.id)
        .withBedId(bed.id)
        .withStartDate(LocalDate.of(2024, 7, 1))
        .withEndDate(LocalDate.of(2024, 7, 3))
        .withReasonId(reason.id)
        .withReferenceNumber("ABC123")
        .withNotes("Some notes")
        .withIsCancelled(false)
        .withCancellationNotes(null)
        .produce()

      seedWithCsv("valid-out-of-service-bed") {
        withRow(row)
      }

      val outOfServiceBeds = cas1OutOfServiceBedTestRepository.findAll()

      assertThat(outOfServiceBeds).hasSize(1)

      val outOfServiceBed = outOfServiceBeds.first()
      assertThat(outOfServiceBed.premises.id).isEqualTo(row.key.premisesId)
      assertThat(outOfServiceBed.bed.id).isEqualTo(row.key.bedId)
      assertThat(outOfServiceBed.revisionHistory).hasSize(1)
      assertThat(outOfServiceBed.startDate).isEqualTo(row.startDate)
      assertThat(outOfServiceBed.endDate).isEqualTo(row.endDate)
      assertThat(outOfServiceBed.reason.id).isEqualTo(row.reasonId)
      assertThat(outOfServiceBed.referenceNumber).isEqualTo(row.referenceNumber)
      assertThat(outOfServiceBed.notes).isEqualTo(row.notes)
      assertThat(outOfServiceBed.cancellation).isNull()
    }
  }

  @Test
  fun `Creates a cancelled out-of-service bed with the correct data`() {
    `Given an Approved Premises Bed` { bed ->
      val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

      val row = Cas1OutOfServiceBedSeedCsvRowFactory()
        .withPremisesId(bed.room.premises.id)
        .withBedId(bed.id)
        .withStartDate(LocalDate.of(2024, 7, 1))
        .withEndDate(LocalDate.of(2024, 7, 3))
        .withReasonId(reason.id)
        .withReferenceNumber("ABC123")
        .withNotes("Some notes")
        .withIsCancelled(true)
        .withCancellationNotes("Some cancellation notes")
        .produce()

      seedWithCsv("cancelled-out-of-service-bed") {
        withRow(row)
      }

      val outOfServiceBeds = cas1OutOfServiceBedTestRepository.findAll()

      assertThat(outOfServiceBeds).hasSize(1)

      val outOfServiceBed = outOfServiceBeds.first()
      assertThat(outOfServiceBed.premises.id).isEqualTo(row.key.premisesId)
      assertThat(outOfServiceBed.bed.id).isEqualTo(row.key.bedId)
      assertThat(outOfServiceBed.revisionHistory).hasSize(1)
      assertThat(outOfServiceBed.startDate).isEqualTo(row.startDate)
      assertThat(outOfServiceBed.endDate).isEqualTo(row.endDate)
      assertThat(outOfServiceBed.reason.id).isEqualTo(row.reasonId)
      assertThat(outOfServiceBed.referenceNumber).isEqualTo(row.referenceNumber)
      assertThat(outOfServiceBed.notes).isEqualTo(row.notes)
      assertThat(outOfServiceBed.cancellation).isNotNull
      assertThat(outOfServiceBed.cancellation!!.notes).isEqualTo(row.cancellationNotes)
    }
  }

  @Test
  fun `Creates an updated out-of-service bed with the correct data`() {
    `Given an Approved Premises Bed` { bed ->
      val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

      val factory = Cas1OutOfServiceBedSeedCsvRowFactory()
        .withPremisesId(bed.room.premises.id)
        .withBedId(bed.id)
        .withStartDate(LocalDate.of(2024, 7, 1))
        .withEndDate(LocalDate.of(2024, 7, 3))
        .withReasonId(reason.id)
        .withReferenceNumber("ABC123")
        .withNotes("Some notes")
        .withIsCancelled(false)
        .withCancellationNotes(null)

      val firstRow = factory.produce()

      val secondRow = factory
        .withEndDate(LocalDate.of(2024, 7, 4))
        .withReferenceNumber("XYZ789")
        .withNotes("Updated notes")
        .produce()

      seedWithCsv("updated-out-of-service-bed") {
        withRow(firstRow)
        withRow(secondRow)
      }

      val outOfServiceBeds = cas1OutOfServiceBedTestRepository.findAll()

      assertThat(outOfServiceBeds).hasSize(1)

      val outOfServiceBed = outOfServiceBeds.first()
      assertThat(outOfServiceBed.premises.id).isEqualTo(firstRow.key.premisesId)
      assertThat(outOfServiceBed.bed.id).isEqualTo(firstRow.key.bedId)
      assertThat(outOfServiceBed.revisionHistory).hasSize(2)
      assertThat(outOfServiceBed.startDate).isEqualTo(secondRow.startDate)
      assertThat(outOfServiceBed.endDate).isEqualTo(secondRow.endDate)
      assertThat(outOfServiceBed.reason.id).isEqualTo(secondRow.reasonId)
      assertThat(outOfServiceBed.referenceNumber).isEqualTo(secondRow.referenceNumber)
      assertThat(outOfServiceBed.notes).isEqualTo(secondRow.notes)
      assertThat(outOfServiceBed.cancellation).isNull()
    }
  }

  @Test
  fun `Creates multiple out-of-service bed with the correct data`() {
    `Given an Approved Premises Bed` { firstBed ->
      `Given an Approved Premises Bed` { secondBed ->
        val firstReason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()
        val secondReason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

        val firstFactory = Cas1OutOfServiceBedSeedCsvRowFactory()
          .withPremisesId(firstBed.room.premises.id)
          .withBedId(firstBed.id)
          .withStartDate(LocalDate.of(2024, 7, 1))
          .withEndDate(LocalDate.of(2024, 7, 3))
          .withReasonId(firstReason.id)
          .withReferenceNumber("ABC123")
          .withNotes("Some notes")
          .withIsCancelled(false)
          .withCancellationNotes(null)

        val firstRow = firstFactory.produce()

        val secondRow = firstFactory
          .withEndDate(LocalDate.of(2024, 7, 4))
          .withReferenceNumber("XYZ789")
          .withNotes("Updated notes")
          .produce()

        val thirdRow = Cas1OutOfServiceBedSeedCsvRowFactory()
          .withPremisesId(secondBed.room.premises.id)
          .withBedId(secondBed.id)
          .withStartDate(LocalDate.of(2024, 8, 15))
          .withEndDate(LocalDate.of(2024, 8, 18))
          .withReasonId(secondReason.id)
          .withReferenceNumber("LMN456")
          .withNotes("Some other notes")
          .withIsCancelled(true)
          .withCancellationNotes("Some cancellation notes")
          .produce()

        seedWithCsv("multiple-out-of-service-beds") {
          withRow(firstRow)
          withRow(secondRow)
          withRow(thirdRow)
        }

        val outOfServiceBeds = cas1OutOfServiceBedTestRepository.findAll()

        assertThat(outOfServiceBeds).hasSize(2)

        val firstOutOfServiceBed = outOfServiceBeds.first()
        assertThat(firstOutOfServiceBed.premises.id).isEqualTo(firstRow.key.premisesId)
        assertThat(firstOutOfServiceBed.bed.id).isEqualTo(firstRow.key.bedId)
        assertThat(firstOutOfServiceBed.revisionHistory).hasSize(2)
        assertThat(firstOutOfServiceBed.startDate).isEqualTo(secondRow.startDate)
        assertThat(firstOutOfServiceBed.endDate).isEqualTo(secondRow.endDate)
        assertThat(firstOutOfServiceBed.reason.id).isEqualTo(secondRow.reasonId)
        assertThat(firstOutOfServiceBed.referenceNumber).isEqualTo(secondRow.referenceNumber)
        assertThat(firstOutOfServiceBed.notes).isEqualTo(secondRow.notes)
        assertThat(firstOutOfServiceBed.cancellation).isNull()

        val secondOutOfServiceBed = outOfServiceBeds.last()
        assertThat(secondOutOfServiceBed.premises.id).isEqualTo(thirdRow.key.premisesId)
        assertThat(secondOutOfServiceBed.bed.id).isEqualTo(thirdRow.key.bedId)
        assertThat(secondOutOfServiceBed.revisionHistory).hasSize(1)
        assertThat(secondOutOfServiceBed.startDate).isEqualTo(thirdRow.startDate)
        assertThat(secondOutOfServiceBed.endDate).isEqualTo(thirdRow.endDate)
        assertThat(secondOutOfServiceBed.reason.id).isEqualTo(thirdRow.reasonId)
        assertThat(secondOutOfServiceBed.referenceNumber).isEqualTo(thirdRow.referenceNumber)
        assertThat(secondOutOfServiceBed.notes).isEqualTo(thirdRow.notes)
        assertThat(secondOutOfServiceBed.cancellation).isNotNull
        assertThat(secondOutOfServiceBed.cancellation!!.notes).isEqualTo(thirdRow.cancellationNotes)
      }
    }
  }

  private fun seedWithCsv(name: String? = null, config: CsvBuilder.() -> Unit) {
    val fileName = name ?: randomStringMultiCaseWithNumbers(64)
    generateCsvFile(
      fileName,
      csv(config),
    )

    seedService.seedData(SeedFileType.approvedPremisesOutOfServiceBeds, fileName)
  }

  private fun csv(config: CsvBuilder.() -> Unit) = CsvBuilder()
    .withHeader(
      "premisesId",
      "bedId",
      "startDate",
      "endDate",
      "reasonId",
      "referenceNumber",
      "notes",
      "isCancelled",
      "cancellationNotes",
    )
    .newRow()
    .apply(config)
    .build()

  private fun CsvBuilder.withRow(row: Cas1OutOfServiceBedSeedCsvRow) = withRow(
    premisesId = row.key.premisesId,
    bedId = row.key.bedId,
    startDate = row.startDate,
    endDate = row.endDate,
    reasonId = row.reasonId,
    referenceNumber = row.referenceNumber,
    notes = row.notes,
    isCancelled = row.isCancelled,
    cancellationNotes = row.cancellationNotes,
  )

  @Suppress("detekt:LongParameterList")
  private fun CsvBuilder.withRow(
    premisesId: UUID? = null,
    bedId: UUID? = null,
    startDate: LocalDate? = null,
    endDate: LocalDate? = null,
    reasonId: UUID? = null,
    referenceNumber: String? = null,
    notes: String? = null,
    isCancelled: Boolean? = null,
    cancellationNotes: String? = null,
  ) = apply {
    withUnquotedField(premisesId ?: "")
    withUnquotedField(bedId ?: "")
    withUnquotedField(startDate ?: "")
    withUnquotedField(endDate ?: "")
    withUnquotedField(reasonId ?: "")
    withUnquotedField(referenceNumber ?: "")
    withUnquotedField(notes ?: "")
    withUnquotedField(isCancelled ?: "")
    withUnquotedField(cancellationNotes ?: "")
    newRow()
  }
}

class Cas1OutOfServiceBedSeedCsvRowFactory : Factory<Cas1OutOfServiceBedSeedCsvRow> {
  private var premisesId: Yielded<UUID> = { UUID.randomUUID() }
  private var bedId: Yielded<UUID> = { UUID.randomUUID() }
  private var startDate: Yielded<LocalDate> = { LocalDate.now() }
  private var endDate: Yielded<LocalDate> = { LocalDate.now() }
  private var reasonId: Yielded<UUID> = { UUID.randomUUID() }
  private var referenceNumber: Yielded<String?> = { randomStringMultiCaseWithNumbers(6) }
  private var notes: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var isCancelled: Yielded<Boolean> = { false }
  private var cancellationNotes: Yielded<String?> = { null }

  fun withPremisesId(premisesId: UUID) = apply {
    this.premisesId = { premisesId }
  }

  fun withBedId(bedId: UUID) = apply {
    this.bedId = { bedId }
  }

  fun withStartDate(startDate: LocalDate) = apply {
    this.startDate = { startDate }
  }

  fun withEndDate(endDate: LocalDate) = apply {
    this.endDate = { endDate }
  }

  fun withReasonId(reasonId: UUID) = apply {
    this.reasonId = { reasonId }
  }

  fun withReferenceNumber(referenceNumber: String?) = apply {
    this.referenceNumber = { referenceNumber }
  }

  fun withNotes(notes: String?) = apply {
    this.notes = { notes }
  }

  fun withIsCancelled(isCancelled: Boolean) = apply {
    this.isCancelled = { isCancelled }
  }

  fun withCancellationNotes(cancellationNotes: String?) = apply {
    this.cancellationNotes = { cancellationNotes }
  }

  override fun produce() = Cas1OutOfServiceBedSeedCsvRow(
    key = Cas1OutOfServiceBedSeedCsvRowKey(
      premisesId = this.premisesId(),
      bedId = this.bedId(),
    ),
    startDate = this.startDate(),
    endDate = this.endDate(),
    reasonId = this.reasonId(),
    referenceNumber = this.referenceNumber(),
    notes = this.notes(),
    isCancelled = this.isCancelled(),
    cancellationNotes = this.cancellationNotes(),
  )
}
