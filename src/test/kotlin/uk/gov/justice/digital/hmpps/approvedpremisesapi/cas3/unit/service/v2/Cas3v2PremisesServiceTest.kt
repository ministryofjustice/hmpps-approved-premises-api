package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service.v2

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2PremisesService
import java.time.LocalDate

class Cas3v2PremisesServiceTest {
  private val mockPremisesRepository = mockk<Cas3PremisesRepository>()
  private val mockCas3DomainEventService = mockk<Cas3v2DomainEventService>()

  private val cas3v2PremisesService = Cas3v2PremisesService(
    mockPremisesRepository,
    mockCas3DomainEventService,
  )

  @Nested
  inner class UnarchivePremises {
    @Test
    fun `When get a bedspace returns Success with correct result when validation passed`() {
      val startDate = LocalDate.now().plusDays(1)
      val endDate = LocalDate.now().plusDays(100)
      val restartDate = LocalDate.now().plusDays(5)
      val premises = Cas3PremisesEntityFactory()
        .withDefaults()
        .withStatus(Cas3PremisesStatus.archived)
        .withNotes("test notes")
        .withStartDate(startDate)
        .withEndDate(endDate)
        .produce()

      val updatedPremises = premises.copy(
        status = Cas3PremisesStatus.online,
        startDate = restartDate,
        endDate = null,
      )

      every { mockPremisesRepository.save(match { it.id == premises.id }) } returns updatedPremises
      every { mockCas3DomainEventService.savePremisesUnarchiveEvent(eq(updatedPremises), premises.startDate, restartDate, premises.endDate!!) } returns Unit

      cas3v2PremisesService.unarchivePremisesAndSaveDomainEvent(premises, restartDate)

      val slot = slot<Cas3PremisesEntity>()
      verify(exactly = 1) {
        mockPremisesRepository.save(capture(slot))
      }

      val savedPremises = slot.captured
      assertAll(
        { assertThat(savedPremises.id).isEqualTo(premises.id) },
        { assertThat(savedPremises.notes).isEqualTo(premises.notes) },
        { assertThat(savedPremises.status).isEqualTo(Cas3PremisesStatus.online) },
        { assertThat(savedPremises.startDate).isEqualTo(restartDate) },
        { assertThat(savedPremises.endDate).isNull() },
      )

      verify(exactly = 1) {
        mockCas3DomainEventService.savePremisesUnarchiveEvent(eq(premises), eq(startDate), eq(restartDate), eq(endDate))
      }
    }
  }
}
