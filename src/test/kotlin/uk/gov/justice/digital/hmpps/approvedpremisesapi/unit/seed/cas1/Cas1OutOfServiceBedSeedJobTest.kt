package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.seed.cas1

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedCancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1OutOfServiceBedSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1OutOfServiceBedSeedCsvRowKey
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1OutOfServiceBedSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1OutOfServiceBedSeedJobTest {
  @Suppress("unused") // Required for @InjectMockKs
  private val filename = "test"

  @MockK
  private lateinit var cas1OutOfServiceBedService: Cas1OutOfServiceBedService

  @MockK
  private lateinit var premisesService: PremisesService

  @InjectMockKs
  private lateinit var seedJob: Cas1OutOfServiceBedSeedJob

  @Nested
  inner class DeserializeRow {
    @Test
    fun `Deserializes CSV row correctly for an active out-of-service bed`() {
      val expectedPremisesId = UUID.randomUUID()
      val expectedBedId = UUID.randomUUID()
      val expectedReasonId = UUID.randomUUID()

      val inputRow = mapOf(
        "premisesId" to expectedPremisesId.toString(),
        "bedId" to expectedBedId.toString(),
        "startDate" to "2024-01-01",
        "endDate" to "2024-02-02",
        "reasonId" to expectedReasonId.toString(),
        "referenceNumber" to "ABC123",
        "notes" to "Some notes",
        "isCancelled" to "false",
        "cancellationNotes" to "",
      )

      val actual = seedJob.deserializeRow(inputRow)

      assertThat(actual.key.premisesId).isEqualTo(expectedPremisesId)
      assertThat(actual.key.bedId).isEqualTo(expectedBedId)
      assertThat(actual.startDate).isEqualTo(LocalDate.of(2024, 1, 1))
      assertThat(actual.endDate).isEqualTo(LocalDate.of(2024, 2, 2))
      assertThat(actual.reasonId).isEqualTo(expectedReasonId)
      assertThat(actual.referenceNumber).isEqualTo("ABC123")
      assertThat(actual.notes).isEqualTo("Some notes")
      assertThat(actual.isCancelled).isFalse
      assertThat(actual.cancellationNotes).isNull()
    }

    @Test
    fun `Deserializes CSV row correctly for a cancelled out-of-service bed`() {
      val expectedPremisesId = UUID.randomUUID()
      val expectedBedId = UUID.randomUUID()
      val expectedReasonId = UUID.randomUUID()

      val inputRow = mapOf(
        "premisesId" to expectedPremisesId.toString(),
        "bedId" to expectedBedId.toString(),
        "startDate" to "2024-01-01",
        "endDate" to "2024-02-02",
        "reasonId" to expectedReasonId.toString(),
        "referenceNumber" to "ABC123",
        "notes" to "Some notes",
        "isCancelled" to "true",
        "cancellationNotes" to "Some cancellation notes",
      )

      val actual = seedJob.deserializeRow(inputRow)

      assertThat(actual.key.premisesId).isEqualTo(expectedPremisesId)
      assertThat(actual.key.bedId).isEqualTo(expectedBedId)
      assertThat(actual.startDate).isEqualTo(LocalDate.of(2024, 1, 1))
      assertThat(actual.endDate).isEqualTo(LocalDate.of(2024, 2, 2))
      assertThat(actual.reasonId).isEqualTo(expectedReasonId)
      assertThat(actual.referenceNumber).isEqualTo("ABC123")
      assertThat(actual.notes).isEqualTo("Some notes")
      assertThat(actual.isCancelled).isTrue
      assertThat(actual.cancellationNotes).isEqualTo("Some cancellation notes")
    }

    @Test
    fun `Deserializes CSV row correctly for optional rows`() {
      val expectedPremisesId = UUID.randomUUID()
      val expectedBedId = UUID.randomUUID()
      val expectedReasonId = UUID.randomUUID()

      val inputRow = mapOf(
        "premisesId" to expectedPremisesId.toString(),
        "bedId" to expectedBedId.toString(),
        "startDate" to "2024-01-01",
        "endDate" to "2024-02-02",
        "reasonId" to expectedReasonId.toString(),
        "referenceNumber" to "",
        "notes" to "",
        "isCancelled" to "false",
        "cancellationNotes" to "",
      )

      val actual = seedJob.deserializeRow(inputRow)

      assertThat(actual.key.premisesId).isEqualTo(expectedPremisesId)
      assertThat(actual.key.bedId).isEqualTo(expectedBedId)
      assertThat(actual.startDate).isEqualTo(LocalDate.of(2024, 1, 1))
      assertThat(actual.endDate).isEqualTo(LocalDate.of(2024, 2, 2))
      assertThat(actual.reasonId).isEqualTo(expectedReasonId)
      assertThat(actual.referenceNumber).isNull()
      assertThat(actual.notes).isNull()
      assertThat(actual.isCancelled).isFalse
      assertThat(actual.cancellationNotes).isNull()
    }
  }

  @Nested
  inner class ProcessRow {
    @Test
    fun `Throws exception if no Approved Premises with given premises ID exists`() {
      val row = Cas1OutOfServiceBedSeedCsvRow(
        key = Cas1OutOfServiceBedSeedCsvRowKey(
          premisesId = UUID.randomUUID(),
          bedId = UUID.randomUUID(),
        ),
        startDate = LocalDate.parse("2024-01-01"),
        endDate = LocalDate.parse("2024-02-02"),
        reasonId = UUID.randomUUID(),
        referenceNumber = "ABC123",
        notes = "Some notes",
        isCancelled = false,
        cancellationNotes = null,
      )

      every { premisesService.getPremises(any()) } returns null

      assertThatExceptionOfType(SeedException::class.java)
        .isThrownBy { seedJob.processRow(row) }
        .withMessage("No Approved Premises with ID ${row.key.premisesId} exists.")

      verify(exactly = 1) {
        premisesService.getPremises(row.key.premisesId)
      }

      confirmVerified()
    }

    @Test
    fun `Creates an out-of-service bed for the first row with a given premisesId-bedId pair`() {
      val row = Cas1OutOfServiceBedSeedCsvRow(
        key = Cas1OutOfServiceBedSeedCsvRowKey(
          premisesId = UUID.randomUUID(),
          bedId = UUID.randomUUID(),
        ),
        startDate = LocalDate.parse("2024-01-01"),
        endDate = LocalDate.parse("2024-02-02"),
        reasonId = UUID.randomUUID(),
        referenceNumber = "ABC123",
        notes = "Some notes",
        isCancelled = false,
        cancellationNotes = null,
      )

      val expectedPremises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .produce()

      every { premisesService.getPremises(any()) } returns expectedPremises

      every { cas1OutOfServiceBedService.createOutOfServiceBed(any(), any(), any(), any(), any(), any(), any(), any()) } returns
        CasResult.Success(
          Cas1OutOfServiceBedEntityFactory()
            .withBed {
              withRoom {
                withPremises(expectedPremises)
              }
            }
            .produce(),
        )

      seedJob.processRow(row)

      verify(exactly = 1) {
        premisesService.getPremises(row.key.premisesId)

        cas1OutOfServiceBedService.createOutOfServiceBed(
          expectedPremises,
          row.startDate,
          row.endDate,
          row.reasonId,
          row.referenceNumber,
          row.notes,
          row.key.bedId,
          createdBy = null,
        )
      }

      confirmVerified()
    }

    @Test
    fun `Creates a cancelled out-of-service bed for the first row with a given premisesId-bedId pair`() {
      val row = Cas1OutOfServiceBedSeedCsvRow(
        key = Cas1OutOfServiceBedSeedCsvRowKey(
          premisesId = UUID.randomUUID(),
          bedId = UUID.randomUUID(),
        ),
        startDate = LocalDate.parse("2024-01-01"),
        endDate = LocalDate.parse("2024-02-02"),
        reasonId = UUID.randomUUID(),
        referenceNumber = "ABC123",
        notes = "Some notes",
        isCancelled = true,
        cancellationNotes = "Some cancellation notes",
      )

      val expectedPremises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .produce()

      every { premisesService.getPremises(any()) } returns expectedPremises

      val expectedOutOfServiceBed = Cas1OutOfServiceBedEntityFactory()
        .withBed {
          withRoom {
            withPremises(expectedPremises)
          }
        }
        .produce()

      every { cas1OutOfServiceBedService.createOutOfServiceBed(any(), any(), any(), any(), any(), any(), any(), any()) } returns
        CasResult.Success(expectedOutOfServiceBed)

      every { cas1OutOfServiceBedService.cancelOutOfServiceBed(any(), any()) } returns
        CasResult.Success(
          Cas1OutOfServiceBedCancellationEntityFactory()
            .withOutOfServiceBed(expectedOutOfServiceBed)
            .produce(),
        )

      seedJob.processRow(row)

      verify(exactly = 1) {
        premisesService.getPremises(row.key.premisesId)

        cas1OutOfServiceBedService.createOutOfServiceBed(
          expectedPremises,
          row.startDate,
          row.endDate,
          row.reasonId,
          row.referenceNumber,
          row.notes,
          row.key.bedId,
          createdBy = null,
        )

        cas1OutOfServiceBedService.cancelOutOfServiceBed(
          any(),
          row.cancellationNotes,
        )
      }

      confirmVerified()
    }

    @Test
    fun `Updates an out-of-service bed for the second row with a given premisesId-bedId pair`() {
      val firstRow = Cas1OutOfServiceBedSeedCsvRow(
        key = Cas1OutOfServiceBedSeedCsvRowKey(
          premisesId = UUID.randomUUID(),
          bedId = UUID.randomUUID(),
        ),
        startDate = LocalDate.parse("2024-01-01"),
        endDate = LocalDate.parse("2024-02-02"),
        reasonId = UUID.randomUUID(),
        referenceNumber = "ABC123",
        notes = "Some notes",
        isCancelled = false,
        cancellationNotes = null,
      )

      val secondRow = Cas1OutOfServiceBedSeedCsvRow(
        key = firstRow.key,
        startDate = LocalDate.parse("2024-01-02"),
        endDate = LocalDate.parse("2024-02-03"),
        reasonId = UUID.randomUUID(),
        referenceNumber = "XYZ789",
        notes = "Some additional notes",
        isCancelled = false,
        cancellationNotes = null,
      )

      val expectedPremises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .produce()

      every { premisesService.getPremises(any()) } returns expectedPremises

      val expectedOutOfServiceBed = Cas1OutOfServiceBedEntityFactory()
        .withBed {
          withRoom {
            withPremises(expectedPremises)
          }
        }
        .produce()

      every { cas1OutOfServiceBedService.createOutOfServiceBed(any(), any(), any(), any(), any(), any(), any(), any()) } returns
        CasResult.Success(expectedOutOfServiceBed)

      every { cas1OutOfServiceBedService.updateOutOfServiceBed(any(), any(), any(), any(), any(), any(), any()) } returns
        CasResult.Success(expectedOutOfServiceBed)

      seedJob.processRow(firstRow)

      verify(exactly = 1) {
        premisesService.getPremises(firstRow.key.premisesId)

        cas1OutOfServiceBedService.createOutOfServiceBed(
          expectedPremises,
          firstRow.startDate,
          firstRow.endDate,
          firstRow.reasonId,
          firstRow.referenceNumber,
          firstRow.notes,
          firstRow.key.bedId,
          createdBy = null,
        )
      }

      confirmVerified()

      seedJob.processRow(secondRow)

      verify(exactly = 1) {
        premisesService.getPremises(secondRow.key.premisesId)

        cas1OutOfServiceBedService.updateOutOfServiceBed(
          expectedOutOfServiceBed.id,
          secondRow.startDate,
          secondRow.endDate,
          secondRow.reasonId,
          secondRow.referenceNumber,
          secondRow.notes,
          createdBy = null,
        )
      }

      confirmVerified()
    }

    @Test
    fun `Creates an out-of-service bed for the second row with a different premisesId-bedId pair`() {
      val firstRow = Cas1OutOfServiceBedSeedCsvRow(
        key = Cas1OutOfServiceBedSeedCsvRowKey(
          premisesId = UUID.randomUUID(),
          bedId = UUID.randomUUID(),
        ),
        startDate = LocalDate.parse("2024-01-01"),
        endDate = LocalDate.parse("2024-02-02"),
        reasonId = UUID.randomUUID(),
        referenceNumber = "ABC123",
        notes = "Some notes",
        isCancelled = false,
        cancellationNotes = null,
      )

      val secondRow = Cas1OutOfServiceBedSeedCsvRow(
        key = Cas1OutOfServiceBedSeedCsvRowKey(
          premisesId = UUID.randomUUID(),
          bedId = UUID.randomUUID(),
        ),
        startDate = LocalDate.parse("2024-01-02"),
        endDate = LocalDate.parse("2024-02-03"),
        reasonId = UUID.randomUUID(),
        referenceNumber = "XYZ789",
        notes = "Some other notes",
        isCancelled = false,
        cancellationNotes = null,
      )

      val firstPremises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .produce()

      val secondPremises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .produce()

      every { premisesService.getPremises(any()) } returnsMany listOf(firstPremises, secondPremises)

      val firstOutOfServiceBed = Cas1OutOfServiceBedEntityFactory()
        .withBed {
          withRoom {
            withPremises(firstPremises)
          }
        }
        .produce()

      val secondOutOfServiceBed = Cas1OutOfServiceBedEntityFactory()
        .withBed {
          withRoom {
            withPremises(secondPremises)
          }
        }
        .produce()

      every { cas1OutOfServiceBedService.createOutOfServiceBed(any(), any(), any(), any(), any(), any(), any(), any()) } returnsMany
        listOf(
          CasResult.Success(firstOutOfServiceBed),
          CasResult.Success(secondOutOfServiceBed),
        )

      every { cas1OutOfServiceBedService.updateOutOfServiceBed(any(), any(), any(), any(), any(), any(), any()) } returnsMany
        listOf(
          CasResult.Success(firstOutOfServiceBed),
          CasResult.Success(secondOutOfServiceBed),
        )

      seedJob.processRow(firstRow)

      verify(exactly = 1) {
        premisesService.getPremises(firstRow.key.premisesId)

        cas1OutOfServiceBedService.createOutOfServiceBed(
          firstPremises,
          firstRow.startDate,
          firstRow.endDate,
          firstRow.reasonId,
          firstRow.referenceNumber,
          firstRow.notes,
          firstRow.key.bedId,
          createdBy = null,
        )
      }

      confirmVerified()

      seedJob.processRow(secondRow)

      verify(exactly = 1) {
        premisesService.getPremises(secondRow.key.premisesId)

        cas1OutOfServiceBedService.createOutOfServiceBed(
          secondPremises,
          secondRow.startDate,
          secondRow.endDate,
          secondRow.reasonId,
          secondRow.referenceNumber,
          secondRow.notes,
          secondRow.key.bedId,
          createdBy = null,
        )
      }

      confirmVerified()
    }
  }
}
