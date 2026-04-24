package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.unit.seed

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed.Cas1ClosePremisesSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedRevisionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1ClosePremisesSeedJobTest {
  @MockK
  private lateinit var approvedPremisesRepository: ApprovedPremisesRepository

  @MockK
  private lateinit var bedRepository: BedRepository

  @MockK
  private lateinit var cas1OutOfServiceBedService: Cas1OutOfServiceBedService

  @InjectMockKs
  private lateinit var seedJob: Cas1ClosePremisesSeedJob

  @Nested
  inner class DeserializeRow {
    @Test
    fun `Deserializes CSV row correctly`() {
      val expectedPremisesId = UUID.randomUUID()
      val inputRow = mapOf(
        "premises_id" to expectedPremisesId.toString(),
        "closure_date" to "2025-05-10",
      )

      val actual = seedJob.deserializeRow(inputRow)

      assertThat(actual.premisesId).isEqualTo(expectedPremisesId)
      assertThat(actual.closureDate).isEqualTo(LocalDate.of(2025, 5, 10))
    }
  }

  @Nested
  inner class ProcessRow {
    @Test
    fun `Updates premises, beds and out-of-service beds correctly`() {
      val premisesId = UUID.randomUUID()
      val closureDate = LocalDate.of(2025, 12, 31)
      val row = Cas1ClosePremisesSeedJob.CsvRow(
        premisesId = premisesId,
        closureDate = closureDate,
        notes = null,
      )

      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withId(premisesId)
        .withAllowNewSpaceBookings(true)
        .produce()

      val room = RoomEntityFactory()
        .withDefaults()
        .withPremises(premises)
        .produce()

      val bed1 = BedEntityFactory()
        .withDefaults()
        .withRoom(room)
        .withEndDate(null)
        .produce()

      val bed2 = BedEntityFactory()
        .withDefaults()
        .withRoom(room)
        .withEndDate(LocalDate.of(2026, 1, 1))
        .produce()

      val oosb1 = Cas1OutOfServiceBedEntityFactory()
        .withBed(bed1)
        .produce()
      oosb1.revisionHistory += Cas1OutOfServiceBedRevisionEntityFactory()
        .withOutOfServiceBed(oosb1)
        .produce()

      every { approvedPremisesRepository.findByIdOrNull(premisesId) } returns premises
      every { approvedPremisesRepository.save(any()) } returnsArgument 0
      every { bedRepository.findByRoomPremisesId(premisesId) } returns listOf(bed1, bed2)
      every { bedRepository.save(any()) } returnsArgument 0
      every { cas1OutOfServiceBedService.getActiveOutOfServiceBedsForPremisesId(premisesId) } returns listOf(oosb1)
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

      verifyOrder {
        approvedPremisesRepository.findByIdOrNull(premisesId)
        bedRepository.findByRoomPremisesId(premisesId)
        bedRepository.save(match { it.id == bed1.id && it.endDate?.isEqual(closureDate) == true })
        bedRepository.save(match { it.id == bed2.id && it.endDate?.isEqual(closureDate) == true })
        cas1OutOfServiceBedService.getActiveOutOfServiceBedsForPremisesId(premisesId)
        cas1OutOfServiceBedService.updateOutOfServiceBed(
          outOfServiceBedId = oosb1.id,
          startDate = oosb1.startDate,
          endDate = closureDate,
          reasonId = oosb1.reason.id,
          referenceNumber = oosb1.referenceNumber,
          notes = oosb1.notes,
          createdBy = null,
        )
        approvedPremisesRepository.save(match { it.id == premisesId && !it.allowNewSpaceBookings })
      }

      confirmVerified()
    }
  }
}
