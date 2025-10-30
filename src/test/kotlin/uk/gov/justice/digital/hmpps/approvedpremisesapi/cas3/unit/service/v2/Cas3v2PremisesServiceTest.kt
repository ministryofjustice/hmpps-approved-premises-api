package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service.v2

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3PremisesCharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3UserAccessService
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3DomainEventService

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

  @MockK
  lateinit var cas3UserAccessService: Cas3UserAccessService

  @MockK
  lateinit var objectMapper: ObjectMapper

  @InjectMockKs
  lateinit var cas3v2PremisesService: Cas3v2PremisesService

  @Nested
  inner class GetPremises {
    @Test
    fun `returns CasResult-NotFound when premises is not found`() {
      every { cas3PremisesRepository.findByIdOrNull(any()) } returns null
      val id = UUID.randomUUID()
      val result = cas3v2PremisesService.getValidatedPremises(id)

      assertThatCasResult(result).isNotFound("Cas3Premises", id)
    }

    @Test
    fun `returns CasResult-Forbidden when user cannot access premises`() {
      val premises = Cas3PremisesEntityFactory().withDefaults().produce()
      every { cas3PremisesRepository.findByIdOrNull(premises.id) } returns premises
      every { cas3UserAccessService.currentUserCanViewPremises(premises.probationDeliveryUnit.probationRegion.id) } returns false

      val result = cas3v2PremisesService.getValidatedPremises(premises.id)

      assertThatCasResult(result).isUnauthorised()
    }

    @Test
    fun `returns CasResult-Success when user can access premises`() {
      val premises = Cas3PremisesEntityFactory().withDefaults().produce()
      every { cas3PremisesRepository.findByIdOrNull(premises.id) } returns premises
      every { cas3UserAccessService.currentUserCanViewPremises(premises.probationDeliveryUnit.probationRegion.id) } returns true

      val result = cas3v2PremisesService.getValidatedPremises(premises.id)

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).isEqualTo(premises)
      }
    }
  }

  @Nested
  inner class UpdatePremises {
    private val premises = Cas3PremisesEntityFactory().withDefaults().produce()
    private val probationRegion = premises.probationDeliveryUnit.probationRegion
    private val laa = premises.localAuthorityArea!!
    private val pdu = premises.probationDeliveryUnit
    private val cas3PremisesCharacteristic = Cas3PremisesCharacteristicEntityFactory().produce()

    @BeforeEach
    fun setup() {
      every { cas3PremisesRepository.findByIdOrNull(premises.id) } returns premises
      every { cas3PremisesRepository.existsByNameIgnoreCaseAndProbationDeliveryUnitId(any(), any()) } returns false
      every { localAuthorityAreaRepository.findByIdOrNull(laa.id) } returns laa
      every { probationDeliveryUnitRepository.findByIdAndProbationRegionId(pdu.id, probationRegion.id) } returns pdu
      every { cas3PremisesCharacteristicRepository.findActiveCharacteristicsByIdIn(listOf(cas3PremisesCharacteristic.id)) } returns mutableListOf(
        cas3PremisesCharacteristic,
      )
    }

    @Test
    fun `When update a premises returns Success with correct result when validation passed`() {
      val premisesSlot = slot<Cas3PremisesEntity>()
      every { cas3PremisesRepository.save(capture(premisesSlot)) } answers { premisesSlot.captured }

      val updatedName = "updatedName"
      val result = cas3v2PremisesService.updatePremises(
        premisesId = premises.id,
        reference = updatedName,
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
        assertThat(capturedPremises.id).isEqualTo(premises.id)
        assertThat(capturedPremises.name).isEqualTo(updatedName)
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
        assertThat(capturedPremises.startDate).isEqualTo(premises.startDate)
        assertThat(capturedPremises.endDate).isNull()
        assertThat(capturedPremises.createdAt).isEqualTo(premises.createdAt)
        assertThat(capturedPremises.lastUpdatedAt).isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))

        assertThatCasResult(result).isSuccess().with {
          assertThat(it).isEqualTo(capturedPremises)
        }
      })

      verify(exactly = 1) { cas3PremisesRepository.findByIdOrNull(premises.id) }
      verify(exactly = 1) { localAuthorityAreaRepository.findByIdOrNull(laa.id) }
      verify(exactly = 1) {
        probationDeliveryUnitRepository.findByIdAndProbationRegionId(pdu.id, pdu.probationRegion.id)
      }
      verify(exactly = 1) {
        cas3PremisesRepository.existsByNameIgnoreCaseAndProbationDeliveryUnitId(updatedName, pdu.id)
      }
    }

    @Test
    fun `has validation errors when incorrect values are used`() {
      every { localAuthorityAreaRepository.findByIdOrNull(any()) } returns null
      every { probationDeliveryUnitRepository.findByIdAndProbationRegionId(any(), any()) } returns null
      every { cas3PremisesCharacteristicRepository.findActiveCharacteristicsByIdIn(any()) } returns mutableListOf()

      val characteristicId = UUID.randomUUID()

      val result = cas3v2PremisesService.updatePremises(
        premisesId = premises.id,
        reference = "",
        addressLine1 = "",
        addressLine2 = "",
        town = "",
        postcode = "",
        localAuthorityAreaId = UUID.randomUUID(),
        probationRegionId = UUID.randomUUID(),
        probationDeliveryUnitId = pdu.id,
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
    fun `has validation errors when given probationDeliveryUnitId does not match premises`() {
      every { localAuthorityAreaRepository.findByIdOrNull(any()) } returns null
      every { probationDeliveryUnitRepository.findByIdAndProbationRegionId(any(), any()) } returns null

      val result = cas3v2PremisesService.updatePremises(
        premisesId = premises.id,
        reference = "",
        addressLine1 = "",
        addressLine2 = "",
        town = "",
        postcode = "",
        localAuthorityAreaId = UUID.randomUUID(),
        probationRegionId = UUID.randomUUID(),
        probationDeliveryUnitId = UUID.randomUUID(),
        characteristicIds = mutableListOf(),
        notes = null,
        turnaroundWorkingDays = 2,
      )

      assertThatCasResult(result).isFieldValidationError()
        .hasMessage("$.probationDeliveryUnitId", "premisesNotInProbationDeliveryUnit")
        .withNumberOfMessages(1)
    }

    @Test
    fun `has validation error when reference is not unique`() {
      val notUniqueName = "notUnique"
      every {
        cas3PremisesRepository.existsByNameIgnoreCaseAndProbationDeliveryUnitId(
          notUniqueName,
          pdu.id,
        )
      } returns true

      val result = cas3v2PremisesService.updatePremises(
        premisesId = premises.id,
        reference = "notUnique",
        addressLine1 = "address1",
        addressLine2 = "",
        town = "",
        postcode = "asd4",
        localAuthorityAreaId = laa.id,
        probationRegionId = pdu.probationRegion.id,
        probationDeliveryUnitId = pdu.id,
        characteristicIds = listOf(cas3PremisesCharacteristic.id),
        notes = null,
        turnaroundWorkingDays = 2,
      )

      assertThatCasResult(result).isFieldValidationError()
        .hasMessage("$.reference", "notUnique")
        .withNumberOfMessages(1)
    }

    @Test
    fun `returns Not Found when premises does not exist`() {
      every { cas3PremisesRepository.findByIdOrNull(any()) } returns null
      val invalidId = UUID.randomUUID()
      val result = cas3v2PremisesService.updatePremises(
        premisesId = invalidId,
        reference = "",
        addressLine1 = "",
        addressLine2 = "",
        town = "",
        postcode = "",
        localAuthorityAreaId = UUID.randomUUID(),
        probationRegionId = UUID.randomUUID(),
        probationDeliveryUnitId = UUID.randomUUID(),
        characteristicIds = mutableListOf(),
        notes = null,
        turnaroundWorkingDays = -1,
      )

      assertThatCasResult(result).isNotFound("Cas3Premises", invalidId)
    }
  }

  @Nested
  inner class CreateNewPremises {

    @Test
    fun `has validation errors when incorrect values are used`() {
      every { localAuthorityAreaRepository.findByIdOrNull(any()) } returns null
      every { probationDeliveryUnitRepository.findByIdAndProbationRegionId(any(), any()) } returns null
      every { cas3PremisesCharacteristicRepository.findActiveCharacteristicsByIdIn(any()) } returns mutableListOf()

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
      every { cas3PremisesCharacteristicRepository.findActiveCharacteristicsByIdIn(any()) } returns mutableListOf()

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
      every { cas3PremisesCharacteristicRepository.findActiveCharacteristicsByIdIn(listOf(cas3PremisesCharacteristic.id)) } returns mutableListOf(
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
      every { cas3PremisesCharacteristicRepository.findActiveCharacteristicsByIdIn(listOf(cas3PremisesCharacteristic.id)) } returns mutableListOf(
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
