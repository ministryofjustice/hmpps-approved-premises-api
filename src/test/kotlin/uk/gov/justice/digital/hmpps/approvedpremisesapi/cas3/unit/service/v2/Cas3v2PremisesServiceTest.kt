package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service.v2

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class Cas3v2PremisesServiceTest {

  @MockK
  lateinit var cas3PremisesRepository: Cas3PremisesRepository

  @MockK
  lateinit var cas3DomainEventService: Cas3v2DomainEventService

  @MockK
  lateinit var localAuthorityAreaRepository: LocalAuthorityAreaRepository

  @MockK
  lateinit var probationDeliveryUnitRepository: ProbationDeliveryUnitRepository

  @MockK
  lateinit var cas3PremisesCharacteristicRepository: Cas3PremisesCharacteristicRepository

  @InjectMockKs
  lateinit var cas3v2PremisesService: Cas3v2PremisesService

  @Nested
  inner class BedspaceTotals {

    val premises = Cas3PremisesEntityFactory().withDefaults().produce()

    val onlineBedspace =
      Cas3BedspaceEntityFactory()
        .withStartDate(LocalDate.now().minusDays(10))
        .withEndDate(LocalDate.now().plusDays(10))
        .withPremises(premises)
        .produce()

    val archivedBedspace =
      Cas3BedspaceEntityFactory()
        .withStartDate(LocalDate.now().minusDays(10))
        .withEndDate(LocalDate.now().minusDays(5))
        .withPremises(premises)
        .produce()

    val upcomingBedspace =
      Cas3BedspaceEntityFactory()
        .withStartDate(LocalDate.now().plusDays(10))
        .withEndDate(LocalDate.now().plusDays(100))
        .withPremises(premises)
        .produce()

    val onlineBedspace2 =
      Cas3BedspaceEntityFactory()
        .withStartDate(LocalDate.now().minusDays(10))
        .withEndDate(LocalDate.now().plusDays(10))
        .withPremises(premises)
        .produce()

    val archivedBedspace2 =
      Cas3BedspaceEntityFactory()
        .withStartDate(LocalDate.now().minusDays(10))
        .withEndDate(LocalDate.now().minusDays(5))
        .withPremises(premises)
        .produce()

    val upcomingBedspace2 =
      Cas3BedspaceEntityFactory()
        .withStartDate(LocalDate.now().plusDays(10))
        .withEndDate(LocalDate.now().plusDays(100))
        .withPremises(premises)
        .produce()

    @Test
    fun `returns correct totals when premises has mixed bedspaces`() {
      premises.bedspaces = mutableListOf(onlineBedspace, onlineBedspace2, archivedBedspace, archivedBedspace2, upcomingBedspace, upcomingBedspace2)
      every { cas3PremisesRepository.findByIdOrNull(premises.id) } returns premises

      val result = cas3v2PremisesService.getBedspaceTotals(premises.id)

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.onlineBedspaces).isEqualTo(2)
        assertThat(it.upcomingBedspaces).isEqualTo(2)
        assertThat(it.archivedBedspaces).isEqualTo(2)
      }
    }

    @Test
    fun `returns all zeros when premises has no bedspaces`() {
      premises.bedspaces = mutableListOf()
      every { cas3PremisesRepository.findByIdOrNull(premises.id) } returns premises
      val result = cas3v2PremisesService.getBedspaceTotals(premises.id)

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.onlineBedspaces).isEqualTo(0)
        assertThat(it.upcomingBedspaces).isEqualTo(0)
        assertThat(it.archivedBedspaces).isEqualTo(0)
      }
    }
  }

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

      every { cas3PremisesRepository.save(match { it.id == premises.id }) } returns updatedPremises
      every {
        cas3DomainEventService.savePremisesUnarchiveEvent(
          eq(updatedPremises),
          premises.startDate,
          restartDate,
          premises.endDate!!,
        )
      } returns Unit

      cas3v2PremisesService.unarchivePremisesAndSaveDomainEvent(premises, restartDate)

      val slot = slot<Cas3PremisesEntity>()
      verify(exactly = 1) {
        cas3PremisesRepository.save(capture(slot))
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
        cas3DomainEventService.savePremisesUnarchiveEvent(eq(premises), eq(startDate), eq(restartDate), eq(endDate))
      }
    }
  }
}
