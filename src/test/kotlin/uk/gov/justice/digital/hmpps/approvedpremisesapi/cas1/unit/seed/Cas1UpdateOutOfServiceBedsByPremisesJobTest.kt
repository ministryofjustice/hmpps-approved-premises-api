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
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed.Cas1UpdateOutOfServiceBedsByPremisesCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed.Cas1UpdateOutOfServiceBedsByPremisesJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedRevisionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1UpdateOutOfServiceBedsByPremisesJobTest {
  @MockK
  private lateinit var cas1OutOfServiceBedService: Cas1OutOfServiceBedService

  @MockK
  private lateinit var premisesService: PremisesService

  @MockK
  private lateinit var cas1OutOfServiceBedReasonRepository: Cas1OutOfServiceBedReasonRepository

  @InjectMockKs
  private lateinit var seedJob: Cas1UpdateOutOfServiceBedsByPremisesJob

  @Nested
  inner class DeserializeRow {
    @Test
    fun `Deserializes CSV row correctly`() {
      val expectedPremisesId = UUID.randomUUID()
      val expectedReasonId = UUID.randomUUID()
      val inputRow = mapOf(
        "premises_id" to expectedPremisesId.toString(),
        "end_date" to "2025-05-10",
        "notes" to "Some notes",
        "reason_id" to expectedReasonId.toString(),
      )

      val actual = seedJob.deserializeRow(inputRow)

      assertThat(actual.premisesId).isEqualTo(expectedPremisesId)
      assertThat(actual.endDate).isEqualTo(LocalDate.of(2025, 5, 10))
      assertThat(actual.notes).isEqualTo("Some notes")
      assertThat(actual.reasonId).isEqualTo(expectedReasonId)
    }

    @Test
    fun `Deserializes CSV row correctly with null values`() {
      val expectedPremisesId = UUID.randomUUID()
      val inputRow = mapOf(
        "premises_id" to expectedPremisesId.toString(),
        "end_date" to "",
        "notes" to "Some notes",
        "reason_id" to "",
      )

      val actual = seedJob.deserializeRow(inputRow)

      assertThat(actual.premisesId).isEqualTo(expectedPremisesId)
      assertThat(actual.endDate).isNull()
      assertThat(actual.notes).isEqualTo("Some notes")
      assertThat(actual.reasonId).isNull()
    }
  }

  @Nested
  inner class ProcessRow {
    @Test
    fun `Throws exception if no Premises with given premises ID exists`() {
      val premisesId = UUID.randomUUID()
      val row = Cas1UpdateOutOfServiceBedsByPremisesCsvRow(
        premisesId = premisesId,
        endDate = LocalDate.now(),
        notes = "Some notes",
        reasonId = UUID.randomUUID(),
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
    fun `Throws exception if no Out of Service Bed Reason with given ID exists`() {
      val premisesId = UUID.randomUUID()
      val reasonId = UUID.randomUUID()
      val row = Cas1UpdateOutOfServiceBedsByPremisesCsvRow(
        premisesId = premisesId,
        endDate = LocalDate.now(),
        notes = "Some notes",
        reasonId = reasonId,
      )

      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .produce()

      every { premisesService.getPremises(premisesId) } returns premises
      every { cas1OutOfServiceBedReasonRepository.findByIdOrNull(reasonId) } returns null

      assertThatExceptionOfType(SeedException::class.java)
        .isThrownBy { seedJob.processRow(row) }
        .withMessage("No Out of Service Bed Reason with ID $reasonId exists.")

      verify(exactly = 1) {
        premisesService.getPremises(premisesId)
        cas1OutOfServiceBedReasonRepository.findByIdOrNull(reasonId)
      }

      confirmVerified()
    }

    @Test
    fun `Updates all active out-of-service beds for the premises`() {
      val premisesId = UUID.randomUUID()
      val endDate = LocalDate.of(2025, 12, 31)
      val notes = "Updated notes"
      val reasonId = UUID.randomUUID()
      val row = Cas1UpdateOutOfServiceBedsByPremisesCsvRow(
        premisesId = premisesId,
        endDate = endDate,
        notes = notes,
        reasonId = reasonId,
      )

      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .produce()

      val reason = Cas1OutOfServiceBedReasonEntityFactory()
        .withId(reasonId)
        .produce()

      val oosb1 = Cas1OutOfServiceBedEntityFactory()
        .withBed { withRoom { withPremises(premises) } }
        .produce()
      oosb1.revisionHistory += Cas1OutOfServiceBedRevisionEntityFactory()
        .withOutOfServiceBed(oosb1)
        .produce()

      val oosb2 = Cas1OutOfServiceBedEntityFactory()
        .withBed { withRoom { withPremises(premises) } }
        .produce()
      oosb2.revisionHistory += Cas1OutOfServiceBedRevisionEntityFactory()
        .withOutOfServiceBed(oosb2)
        .produce()

      every { premisesService.getPremises(premisesId) } returns premises
      every { cas1OutOfServiceBedReasonRepository.findByIdOrNull(reasonId) } returns reason
      every { cas1OutOfServiceBedService.getActiveOutOfServiceBedsForPremisesId(premisesId) } returns listOf(oosb1, oosb2)
      every {
        cas1OutOfServiceBedService.updateOutOfServiceBed(
          outOfServiceBedId = any(),
          startDate = any(),
          endDate = any(),
          reasonId = any(),
          referenceNumber = any(),
          notes = any(),
          createdBy = any(),
        )
      } returns CasResult.Success(oosb1)

      seedJob.processRow(row)

      verify(exactly = 1) {
        premisesService.getPremises(premisesId)
        cas1OutOfServiceBedReasonRepository.findByIdOrNull(reasonId)
        cas1OutOfServiceBedService.getActiveOutOfServiceBedsForPremisesId(premisesId)
      }

      verify(exactly = 1) {
        cas1OutOfServiceBedService.updateOutOfServiceBed(
          outOfServiceBedId = oosb1.id,
          startDate = oosb1.startDate,
          endDate = endDate,
          reasonId = reasonId,
          referenceNumber = oosb1.referenceNumber,
          notes = notes,
          createdBy = null,
        )
        cas1OutOfServiceBedService.updateOutOfServiceBed(
          outOfServiceBedId = oosb2.id,
          startDate = oosb2.startDate,
          endDate = endDate,
          reasonId = reasonId,
          referenceNumber = oosb2.referenceNumber,
          notes = notes,
          createdBy = null,
        )
      }

      confirmVerified()
    }

    @Test
    fun `Updates all active out-of-service beds using existing values if CSV values are null`() {
      val premisesId = UUID.randomUUID()
      val notes = "Updated notes"
      val row = Cas1UpdateOutOfServiceBedsByPremisesCsvRow(
        premisesId = premisesId,
        endDate = null,
        notes = notes,
        reasonId = null,
      )

      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .produce()

      val existingReason = Cas1OutOfServiceBedReasonEntityFactory()
        .produce()

      val oosb = Cas1OutOfServiceBedEntityFactory()
        .withBed { withRoom { withPremises(premises) } }
        .produce()
      oosb.revisionHistory += Cas1OutOfServiceBedRevisionEntityFactory()
        .withOutOfServiceBed(oosb)
        .withReason(existingReason)
        .withEndDate(LocalDate.of(2025, 6, 6))
        .produce()

      every { premisesService.getPremises(premisesId) } returns premises
      every { cas1OutOfServiceBedService.getActiveOutOfServiceBedsForPremisesId(premisesId) } returns listOf(oosb)
      every {
        cas1OutOfServiceBedService.updateOutOfServiceBed(
          outOfServiceBedId = any(),
          startDate = any(),
          endDate = any(),
          reasonId = any(),
          referenceNumber = any(),
          notes = any(),
          createdBy = any(),
        )
      } returns CasResult.Success(oosb)

      seedJob.processRow(row)

      verify(exactly = 1) {
        premisesService.getPremises(premisesId)
        cas1OutOfServiceBedService.getActiveOutOfServiceBedsForPremisesId(premisesId)
      }

      verify(exactly = 1) {
        cas1OutOfServiceBedService.updateOutOfServiceBed(
          outOfServiceBedId = oosb.id,
          startDate = oosb.startDate,
          endDate = oosb.endDate,
          reasonId = oosb.reason.id,
          referenceNumber = oosb.referenceNumber,
          notes = notes,
          createdBy = null,
        )
      }

      confirmVerified()
    }
  }
}
