package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service.v2

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3PremisesCharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

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
  inner class CreateNewPremises {

    @Test
    fun `has validation errors when incorrect values are used`() {
      every { localAuthorityAreaRepository.findByIdOrNull(any()) } returns null
      every { probationDeliveryUnitRepository.findByIdAndProbationRegionId(any(), any()) } returns null
      every { cas3PremisesCharacteristicRepository.findActiveCharacteristicsByIdIn(any()) } returns emptyList()

      val characteristicId = UUID.randomUUID()

      val result = cas3v2PremisesService.createNewPremises(
        reference = "",
        addressLine1 = "",
        addressLine2 = "",
        town = "",
        postcode = "",
        localAuthorityAreaId = UUID.randomUUID(),
        probationRegionId = UUID.randomUUID(),
        probationDeliveryUnitId = UUID.randomUUID(),
        characteristicIds = mutableListOf(characteristicId),
        notes = null,
        turnaroundWorkingDays = -1,
      )

      assertThatCasResult(result).isFieldValidationError()
        .hasMessage("$.reference", "empty")
        .hasMessage("$.address", "empty")
        .hasMessage("$.postcode", "empty")
        .hasMessage("$.localAuthorityAreaId", "doesNotExist")
        .hasMessage("$.probationRegionId", "doesNotExist")
        .hasMessage("$.probationDeliveryUnitId", "doesNotExist")
        .hasMessage("$.premisesCharacteristics[$characteristicId]", "doesNotExist")
        .hasMessage("$.turnaroundWorkingDays", "isNotAPositiveInteger")
        .withNumberOfMessages(8)

      verify(exactly = 0) { cas3PremisesRepository.existsByNameIgnoreCaseAndProbationDeliveryUnitId(any(), any()) }
    }

    @Test
    fun `has validation error when reference is not unique`() {
      every { localAuthorityAreaRepository.findByIdOrNull(any()) } returns mockk<LocalAuthorityAreaEntity>()

      val mockPdu = mockk<ProbationDeliveryUnitEntity>()
      every { probationDeliveryUnitRepository.findByIdAndProbationRegionId(any(), any()) } returns mockPdu
      every { mockPdu.probationRegion } returns mockk<ProbationRegionEntity>()
      every { cas3PremisesRepository.existsByNameIgnoreCaseAndProbationDeliveryUnitId(any(), any()) } returns true
      every { cas3PremisesCharacteristicRepository.findActiveCharacteristicsByIdIn(any()) } returns emptyList()

      val pduId = UUID.randomUUID()

      val result = cas3v2PremisesService.createNewPremises(
        reference = "notunique",
        addressLine1 = "asd",
        addressLine2 = "asd",
        town = "asd",
        postcode = "asd",
        localAuthorityAreaId = null,
        probationRegionId = UUID.randomUUID(),
        probationDeliveryUnitId = pduId,
        characteristicIds = mutableListOf(),
        notes = null,
        turnaroundWorkingDays = 0,
      )

      verify(exactly = 1) {
        cas3PremisesRepository.existsByNameIgnoreCaseAndProbationDeliveryUnitId(
          "notunique",
          pduId,
        )
      }

      assertThatCasResult(result).isFieldValidationError()
        .hasMessage("$.reference", "notUnique")
        .withNumberOfMessages(1)
    }

    @Test
    fun `returns CasResult-Success when validation is passed`() {
      val laa = LocalAuthorityEntityFactory().produce()
      val pdu = ProbationDeliveryUnitEntityFactory().withDefaults().produce()
      val cas3PremisesCharacteristic = Cas3PremisesCharacteristicEntityFactory().produce()

      val premisesName = "newName"

      every { localAuthorityAreaRepository.findByIdOrNull(laa.id) } returns laa
      every { probationDeliveryUnitRepository.findByIdAndProbationRegionId(pdu.id, pdu.probationRegion.id) } returns pdu
      every {
        cas3PremisesRepository.existsByNameIgnoreCaseAndProbationDeliveryUnitId(premisesName, pdu.id)
      } returns false
      every { cas3PremisesCharacteristicRepository.findActiveCharacteristicsByIdIn(listOf(cas3PremisesCharacteristic.id)) } returns listOf(
        cas3PremisesCharacteristic,
      )

      val premisesSlot = slot<Cas3PremisesEntity>()
      every { cas3PremisesRepository.save(capture(premisesSlot)) } answers { premisesSlot.captured }

      val result = cas3v2PremisesService.createNewPremises(
        reference = premisesName,
        addressLine1 = "asd1",
        addressLine2 = "asd2",
        town = "asd3",
        postcode = "asd4",
        localAuthorityAreaId = laa.id,
        probationRegionId = pdu.probationRegion.id,
        probationDeliveryUnitId = pdu.id,
        characteristicIds = listOf(cas3PremisesCharacteristic.id),
        notes = "some new notes",
        turnaroundWorkingDays = 9,
      )

      assertAll({
        val capturedPremises = premisesSlot.captured
        assertThat(capturedPremises.id).isNotNull
        assertThat(capturedPremises.name).isEqualTo(premisesName)
        assertThat(capturedPremises.addressLine1).isEqualTo("asd1")
        assertThat(capturedPremises.addressLine2).isEqualTo("asd2")
        assertThat(capturedPremises.town).isEqualTo("asd3")
        assertThat(capturedPremises.postcode).isEqualTo("asd4")
        assertThat(capturedPremises.localAuthorityArea).isEqualTo(laa)
        assertThat(capturedPremises.notes).isEqualTo("some new notes")
        assertThat(capturedPremises.status).isEqualTo(Cas3PremisesStatus.online)
        assertThat(capturedPremises.probationDeliveryUnit).isEqualTo(pdu)
        assertThat(capturedPremises.characteristics).isEqualTo(mutableListOf(cas3PremisesCharacteristic))
        assertThat(capturedPremises.turnaroundWorkingDays).isEqualTo(9)
        assertThat(capturedPremises.bookings).isEmpty()
        assertThat(capturedPremises.bedspaces).isEmpty()
        assertThat(capturedPremises.startDate).isEqualTo(LocalDate.now())
        assertThat(capturedPremises.endDate).isNull()
        assertThat(capturedPremises.createdAt).isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(capturedPremises.lastUpdatedAt).isNull()

        assertThatCasResult(result).isSuccess().with {
          assertThat(it).isEqualTo(capturedPremises)
        }
      })

      verify(exactly = 1) { localAuthorityAreaRepository.findByIdOrNull(laa.id) }
      verify(exactly = 1) {
        probationDeliveryUnitRepository.findByIdAndProbationRegionId(pdu.id, pdu.probationRegion.id)
      }
      verify(exactly = 1) {
        cas3PremisesRepository.existsByNameIgnoreCaseAndProbationDeliveryUnitId(premisesName, pdu.id)
      }
    }

    @Test
    fun `turnaround working days defaults to 2 when null is provided`() {
      val laa = LocalAuthorityEntityFactory().produce()
      val pdu = ProbationDeliveryUnitEntityFactory().withDefaults().produce()
      val cas3PremisesCharacteristic = Cas3PremisesCharacteristicEntityFactory().produce()

      val premisesName = "newName"

      every { localAuthorityAreaRepository.findByIdOrNull(laa.id) } returns laa
      every { probationDeliveryUnitRepository.findByIdAndProbationRegionId(pdu.id, pdu.probationRegion.id) } returns pdu
      every {
        cas3PremisesRepository.existsByNameIgnoreCaseAndProbationDeliveryUnitId(premisesName, pdu.id)
      } returns false
      every { cas3PremisesCharacteristicRepository.findActiveCharacteristicsByIdIn(listOf(cas3PremisesCharacteristic.id)) } returns listOf(
        cas3PremisesCharacteristic,
      )

      val premisesSlot = slot<Cas3PremisesEntity>()
      every { cas3PremisesRepository.save(capture(premisesSlot)) } answers { premisesSlot.captured }

      val result = cas3v2PremisesService.createNewPremises(
        reference = premisesName,
        addressLine1 = "asd1",
        addressLine2 = "asd2",
        town = "asd3",
        postcode = "asd4",
        localAuthorityAreaId = laa.id,
        probationRegionId = pdu.probationRegion.id,
        probationDeliveryUnitId = pdu.id,
        characteristicIds = listOf(cas3PremisesCharacteristic.id),
        notes = "",
        turnaroundWorkingDays = null,
      )

      assertAll({
        val capturedPremises = premisesSlot.captured
        assertThat(capturedPremises.turnaroundWorkingDays).isEqualTo(2)
        assertThatCasResult(result).isSuccess().with {
          assertThat(it.turnaroundWorkingDays).isEqualTo(2)
        }
      })
    }
  }

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
      every { cas3DomainEventService.savePremisesUnarchiveEvent(eq(updatedPremises), premises.startDate, restartDate, premises.endDate!!, any()) } returns Unit

      cas3v2PremisesService.unarchivePremisesAndSaveDomainEvent(premises, restartDate, UUID.randomUUID())

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
        cas3DomainEventService.savePremisesUnarchiveEvent(eq(premises), eq(startDate), eq(restartDate), eq(endDate), any())
      }
    }
  }
}
