package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.unit.seed

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed.Cas1CancelOutOfServiceBedsByPremisesJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedCancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1CancelOutOfServiceBedsByPremisesJobTest {
  @MockK
  private lateinit var cas1OutOfServiceBedService: Cas1OutOfServiceBedService

  @MockK
  private lateinit var premisesService: PremisesService

  @InjectMockKs
  private lateinit var seedJob: Cas1CancelOutOfServiceBedsByPremisesJob

  @Nested
  inner class DeserializeRow {
    @Test
    fun `Deserializes CSV row correctly`() {
      val expectedPremisesId = UUID.randomUUID()
      val inputRow = mapOf(
        "premisesId" to expectedPremisesId.toString(),
        "notes" to "Some notes",
      )

      val actual = seedJob.deserializeRow(inputRow)

      assertThat(actual.premisesId).isEqualTo(expectedPremisesId)
      assertThat(actual.notes).isEqualTo("Some notes")
    }

    @Test
    fun `Deserializes CSV row correctly with null notes`() {
      val expectedPremisesId = UUID.randomUUID()
      val inputRow = mapOf(
        "premisesId" to expectedPremisesId.toString(),
        "notes" to "",
      )

      val actual = seedJob.deserializeRow(inputRow)

      assertThat(actual.premisesId).isEqualTo(expectedPremisesId)
      assertThat(actual.notes).isNull()
    }
  }

  @Nested
  inner class ProcessRow {
    @Test
    fun `Throws exception if no Premises with given premises ID exists`() {
      val premisesId = UUID.randomUUID()
      val row = uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed.Cas1CancelOutOfServiceBedsByPremisesCsvRow(
        premisesId = premisesId,
        notes = "Some notes",
      )

      every { premisesService.getPremises(any()) } returns null

      assertThatExceptionOfType(SeedException::class.java)
        .isThrownBy { seedJob.processRow(row) }
        .withMessage("No Premises with ID $premisesId exists.")

      verify(exactly = 1) {
        premisesService.getPremises(premisesId)
      }

      confirmVerified()
    }

    @Test
    fun `Cancels all active out-of-service beds for the premises`() {
      val premisesId = UUID.randomUUID()
      val notes = "Cancellation notes"
      val row = uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed.Cas1CancelOutOfServiceBedsByPremisesCsvRow(
        premisesId = premisesId,
        notes = notes,
      )

      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .produce()

      val oosb1 = Cas1OutOfServiceBedEntityFactory()
        .withBed { withRoom { withPremises(premises) } }
        .produce()

      val oosb2 = Cas1OutOfServiceBedEntityFactory()
        .withBed { withRoom { withPremises(premises) } }
        .produce()

      every { premisesService.getPremises(premisesId) } returns premises
      every { cas1OutOfServiceBedService.getActiveOutOfServiceBedsForPremisesId(premisesId) } returns listOf(oosb1, oosb2)
      every { cas1OutOfServiceBedService.cancelOutOfServiceBed(any(), any()) } answers {
        val oosb = it.invocation.args[0] as uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
        CasResult.Success(
          Cas1OutOfServiceBedCancellationEntityFactory()
            .withOutOfServiceBed(oosb)
            .produce(),
        )
      }

      seedJob.processRow(row)

      verify(exactly = 1) {
        premisesService.getPremises(premisesId)
        cas1OutOfServiceBedService.getActiveOutOfServiceBedsForPremisesId(premisesId)
        cas1OutOfServiceBedService.cancelOutOfServiceBed(oosb1, notes)
        cas1OutOfServiceBedService.cancelOutOfServiceBed(oosb2, notes)
      }

      confirmVerified()
    }
  }
}
