package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service

import arrow.core.Ior
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3TurnaroundEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3VoidBedspaceCancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3VoidBedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3VoidBedspaceReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceCancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceUnarchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceArchiveAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingSummaryForAvailabilityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DomainEventEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Availability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.ObjectMapperFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID
import java.util.stream.Stream

class Cas3PremisesServiceTest {
  private val premisesRepositoryMock = mockk<PremisesRepository>()
  private val cas3VoidBedspacesRepositoryMock = mockk<Cas3VoidBedspacesRepository>()
  private val cas3VoidBedspaceReasonRepositoryMock = mockk<Cas3VoidBedspaceReasonRepository>()
  private val cas3VoidBedspaceCancellationRepositoryMock = mockk<Cas3VoidBedspaceCancellationRepository>()
  private val bookingRepositoryMock = mockk<BookingRepository>()
  private val localAuthorityAreaRepositoryMock = mockk<LocalAuthorityAreaRepository>()
  private val probationRegionRepositoryMock = mockk<ProbationRegionRepository>()
  private val probationDeliveryUnitRepositoryMock = mockk<ProbationDeliveryUnitRepository>()
  private val roomRepositoryMock = mockk<RoomRepository>()
  private val bedRepositoryMock = mockk<BedRepository>()
  private val characteristicServiceMock = mockk<CharacteristicService>()
  private val workingDayServiceMock = mockk<WorkingDayService>()
  private val objectMapper = ObjectMapperFactory.createRuntimeLikeObjectMapper()
  private val cas3DomainEventServiceMock = mockk<Cas3DomainEventService>()

  private val temporaryAccommodationPremisesFactory = TemporaryAccommodationPremisesEntityFactory()
    .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory().withYieldedApArea { ApAreaEntityFactory().produce() }.produce()
    }

  private val bookingEntityFactory = BookingEntityFactory()

  private val premisesService = Cas3PremisesService(
    premisesRepositoryMock,
    cas3VoidBedspacesRepositoryMock,
    cas3VoidBedspaceReasonRepositoryMock,
    cas3VoidBedspaceCancellationRepositoryMock,
    bookingRepositoryMock,
    localAuthorityAreaRepositoryMock,
    probationRegionRepositoryMock,
    probationDeliveryUnitRepositoryMock,
    roomRepositoryMock,
    bedRepositoryMock,
    characteristicServiceMock,
    workingDayServiceMock,
    cas3DomainEventServiceMock,
    objectMapper,
  )

  @Nested
  inner class CreatePremises {
    @Test
    fun `When create a new premises returns Success with correct result when validation passed`() {
      val premises = createPremisesEntity()

      mockCommonPremisesDependencies(premises)
      every { premisesRepositoryMock.save(any()) } returns null

      val result = callCreateNewPremises(premises)

      assertThatCasResult(result).isSuccess().with { newPremises ->
        assertThat(newPremises.id).isNotNull()
        assertThat(newPremises.name).isEqualTo(premises.name)
        assertThat(newPremises.addressLine1).isEqualTo(premises.addressLine1)
        assertThat(newPremises.addressLine2).isEqualTo(premises.addressLine2)
        assertThat(newPremises.town).isEqualTo(premises.town)
        assertThat(newPremises.postcode).isEqualTo(premises.postcode)
        assertThat(newPremises.localAuthorityArea).isEqualTo(premises.localAuthorityArea)
        assertThat(newPremises.probationRegion).isEqualTo(premises.probationRegion)
        assertThat(newPremises.probationDeliveryUnit).isEqualTo(premises.probationDeliveryUnit)
        assertThat(newPremises.status).isEqualTo(PropertyStatus.active)
        assertThat(newPremises.notes).isEqualTo(premises.notes)
        assertThat(newPremises.turnaroundWorkingDays).isEqualTo(premises.turnaroundWorkingDays)
        assertThat(newPremises.characteristics).isEmpty()
        assertThat(newPremises.startDate).isEqualTo(LocalDate.now())
      }
    }

    @Test
    fun `When creating a new premises with a non exist probation region returns a FieldValidationError with the correct message`() {
      val premises = createPremisesEntity()
      val nonExistProbationRegionId = UUID.randomUUID()
      val localAuthorityArea = premises.localAuthorityArea!!
      val probationDeliveryUnit = premises.probationDeliveryUnit!!

      every { probationRegionRepositoryMock.findByIdOrNull(nonExistProbationRegionId) } returns null
      every { localAuthorityAreaRepositoryMock.findByIdOrNull(localAuthorityArea.id) } returns localAuthorityArea
      every { probationDeliveryUnitRepositoryMock.findByIdAndProbationRegionId(probationDeliveryUnit.id, any()) } returns probationDeliveryUnit
      every { premisesRepositoryMock.nameIsUniqueForType(premises.name, TemporaryAccommodationPremisesEntity::class.java) } returns true

      val result = callCreateNewPremises(premises, probationRegionId = nonExistProbationRegionId)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.probationRegionId", "doesNotExist")
    }

    @Test
    fun `When creating a new premises with a non exist local authority returns a FieldValidationError with the correct message`() {
      val premises = createPremisesEntity()
      val probationRegion = premises.probationRegion
      val probationDeliveryUnit = premises.probationDeliveryUnit!!
      val nonExistLocalAuthorityId = UUID.randomUUID()

      every { probationRegionRepositoryMock.findByIdOrNull(premises.probationRegion.id) } returns probationRegion
      every { localAuthorityAreaRepositoryMock.findByIdOrNull(nonExistLocalAuthorityId) } returns null
      every { probationDeliveryUnitRepositoryMock.findByIdAndProbationRegionId(probationDeliveryUnit.id, any()) } returns probationDeliveryUnit
      every { premisesRepositoryMock.nameIsUniqueForType(premises.name, TemporaryAccommodationPremisesEntity::class.java) } returns true

      val result = callCreateNewPremises(premises, localAuthorityId = nonExistLocalAuthorityId)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.localAuthorityAreaId", "doesNotExist")
    }

    @Test
    fun `When creating a new premises with a non exist probation delivery unit returns a FieldValidationError with the correct message`() {
      val premises = createPremisesEntity()
      val probationRegion = premises.probationRegion
      val localAuthorityArea = premises.localAuthorityArea!!
      val nonExistPduId = UUID.randomUUID()

      every { probationRegionRepositoryMock.findByIdOrNull(probationRegion.id) } returns probationRegion
      every { localAuthorityAreaRepositoryMock.findByIdOrNull(localAuthorityArea.id) } returns localAuthorityArea
      every { probationDeliveryUnitRepositoryMock.findByIdAndProbationRegionId(nonExistPduId, probationRegion.id) } returns null
      every { premisesRepositoryMock.nameIsUniqueForType(premises.name, TemporaryAccommodationPremisesEntity::class.java) } returns true

      val result = callCreateNewPremises(premises, probationDeliveryUnitId = nonExistPduId)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.probationDeliveryUnitId", "doesNotExist")
    }

    @Test
    fun `When creating a new premises with empty reference returns a FieldValidationError with the correct message`() {
      val premises = createPremisesEntity()

      mockCommonPremisesDependencies(premises)

      val result = callCreateNewPremises(premises, reference = "")

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.reference", "empty")
    }

    @Test
    fun `When creating a new premises with existing premises reference returns a FieldValidationError with the correct message`() {
      val premises = createPremisesEntity()

      mockCommonPremisesDependencies(premises, isUniqueReference = false)

      val result = callCreateNewPremises(premises)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.reference", "notUnique")
    }

    @Test
    fun `When creating a new premises with empty address returns a FieldValidationError with the correct message`() {
      val premises = createPremisesEntity()

      mockCommonPremisesDependencies(premises)

      val result = callCreateNewPremises(premises, address1 = "")

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.address", "empty")
    }

    @Test
    fun `When creating a new premises with empty postcode returns a FieldValidationError with the correct message`() {
      val premises = createPremisesEntity()

      mockCommonPremisesDependencies(premises)

      val result = callCreateNewPremises(premises, postcode = "")

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.postcode", "empty")
    }

    @Test
    fun `When creating a new premises with turnaround working days in negative returns a FieldValidationError with the correct message`() {
      val premises = createPremisesEntity()

      mockCommonPremisesDependencies(premises)

      val result = callCreateNewPremises(premises, turnaroundWorkingDays = -2)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.turnaroundWorkingDays", "isNotAPositiveInteger")
    }

    @Test
    fun `When create a new premises with a non exist characteristic returns FieldValidationError with the correct message`() {
      val premises = createPremisesEntity()
      val nonExistCharacteristicId = UUID.randomUUID()

      mockCommonPremisesDependencies(premises)
      every { characteristicServiceMock.getCharacteristic(nonExistCharacteristicId) } returns null

      val result = callCreateNewPremises(premises, characteristicIds = listOf(nonExistCharacteristicId))

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.characteristics[0]", "doesNotExist")
    }

    @Test
    fun `When create a new bedspace with a wrong model scope characteristic returns FieldValidationError with the correct message`() {
      val premises = createPremisesEntity()
      val characteristicEntityFactory = CharacteristicEntityFactory()

      val premisesCharacteristic = characteristicEntityFactory
        .withModelScope("room")
        .withServiceScope(ServiceName.temporaryAccommodation.value)
        .produce()

      mockCommonPremisesDependencies(premises)
      every { characteristicServiceMock.getCharacteristic(premisesCharacteristic.id) } returns premisesCharacteristic
      every { characteristicServiceMock.modelScopeMatches(premisesCharacteristic, any()) } returns false

      val result = callCreateNewPremises(premises, characteristicIds = listOf(premisesCharacteristic.id))

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.characteristics[0]", "incorrectCharacteristicModelScope")
    }

    @Test
    fun `When create a new bedspace with a wrong service scope characteristic returns FieldValidationError with the correct message`() {
      val premises = createPremisesEntity()
      val characteristicEntityFactory = CharacteristicEntityFactory()

      val premisesCharacteristic = characteristicEntityFactory
        .withModelScope("premises")
        .withServiceScope(ServiceName.approvedPremises.value)
        .produce()

      mockCommonPremisesDependencies(premises)
      every { characteristicServiceMock.getCharacteristic(premisesCharacteristic.id) } returns premisesCharacteristic
      every { characteristicServiceMock.modelScopeMatches(premisesCharacteristic, any()) } returns true
      every { characteristicServiceMock.serviceScopeMatches(premisesCharacteristic, any()) } returns false

      val result = callCreateNewPremises(premises, characteristicIds = listOf(premisesCharacteristic.id))

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.characteristics[0]", "incorrectCharacteristicServiceScope")
    }

    private fun createPremisesEntity(): TemporaryAccommodationPremisesEntity {
      val probationRegion = ProbationRegionEntityFactory()
        .withApArea(ApAreaEntityFactory().produce())
        .produce()

      val localAuthority = LocalAuthorityEntityFactory()
        .produce()

      val probationDeliveryUnit = ProbationDeliveryUnitEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      return temporaryAccommodationPremisesFactory
        .withService(ServiceName.temporaryAccommodation.value)
        .withProbationRegion(probationRegion)
        .withProbationDeliveryUnit(probationDeliveryUnit)
        .withLocalAuthorityArea(localAuthority)
        .produce()
    }

    @SuppressWarnings("LongParameterList")
    private fun callCreateNewPremises(
      premises: TemporaryAccommodationPremisesEntity,
      probationRegionId: UUID = premises.probationRegion.id,
      localAuthorityId: UUID? = premises.localAuthorityArea?.id,
      probationDeliveryUnitId: UUID? = premises.probationDeliveryUnit?.id,
      reference: String = premises.name,
      address1: String = premises.addressLine1,
      postcode: String = premises.postcode,
      turnaroundWorkingDays: Int = premises.turnaroundWorkingDays,
      characteristicIds: List<UUID> = emptyList(),
    ) = premisesService.createNewPremises(
      reference = reference,
      addressLine1 = address1,
      addressLine2 = premises.addressLine2,
      town = premises.town,
      postcode = postcode,
      localAuthorityAreaId = localAuthorityId,
      probationRegionId = probationRegionId,
      probationDeliveryUnitId = probationDeliveryUnitId!!,
      characteristicIds = characteristicIds,
      notes = premises.notes,
      turnaroundWorkingDays = turnaroundWorkingDays,
    )

    private fun mockCommonPremisesDependencies(premises: TemporaryAccommodationPremisesEntity, isUniqueReference: Boolean = true) {
      val probationRegion = premises.probationRegion
      val localAuthorityArea = premises.localAuthorityArea!!
      val probationDeliveryUnit = premises.probationDeliveryUnit!!
      every { probationRegionRepositoryMock.findByIdOrNull(probationRegion.id) } returns probationRegion
      every { localAuthorityAreaRepositoryMock.findByIdOrNull(localAuthorityArea.id) } returns localAuthorityArea
      every { probationDeliveryUnitRepositoryMock.findByIdAndProbationRegionId(probationDeliveryUnit.id, probationRegion.id) } returns probationDeliveryUnit
      every { premisesRepositoryMock.nameIsUniqueForType(premises.name, TemporaryAccommodationPremisesEntity::class.java) } returns isUniqueReference
    }
  }

  @Nested
  inner class UpdatePremises {
    @Test
    fun `When update a premises returns Success with correct result when validation passed`() {
      val premises = updatePremisesEntity()
      mockCommonPremisesDependencies(premises)
      every { premisesRepositoryMock.save(any()) } returns premises

      val result = premisesService.updatePremises(
        premises = premises,
        reference = premises.name,
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = premises.postcode,
        localAuthorityAreaId = premises.localAuthorityArea?.id,
        probationRegionId = premises.probationRegion.id,
        characteristicIds = emptyList(),
        notes = premises.notes,
        probationDeliveryUnitId = premises.probationDeliveryUnit?.id!!,
        turnaroundWorkingDays = premises.turnaroundWorkingDays,
      )

      assertThatCasResult(result).isSuccess()
        .with { updatedPremises ->
          assertThat(updatedPremises.id).isNotNull()
          assertThat(updatedPremises.name).isEqualTo(premises.name)
          assertThat(updatedPremises.addressLine1).isEqualTo(premises.addressLine1)
          assertThat(updatedPremises.addressLine2).isEqualTo(premises.addressLine2)
          assertThat(updatedPremises.town).isEqualTo(premises.town)
          assertThat(updatedPremises.postcode).isEqualTo(premises.postcode)
          assertThat(updatedPremises.localAuthorityArea).isEqualTo(premises.localAuthorityArea)
          assertThat(updatedPremises.probationRegion).isEqualTo(premises.probationRegion)
          assertThat(updatedPremises.probationDeliveryUnit?.id).isEqualTo(premises.probationDeliveryUnit?.id)
          assertThat(updatedPremises.notes).isEqualTo(premises.notes)
          assertThat(updatedPremises.turnaroundWorkingDays).isEqualTo(premises.turnaroundWorkingDays)
          assertThat(updatedPremises.characteristics).isEmpty()
        }
    }

    @Test
    fun `When update a premises with a non exist probation region returns a FieldValidationError with the correct message`() {
      val premises = updatePremisesEntity()
      val nonExistProbationRegionId = UUID.randomUUID()
      val localAuthorityArea = premises.localAuthorityArea!!
      val probationDeliveryUnit = premises.probationDeliveryUnit!!

      every { premisesRepositoryMock.nameIsUniqueForType(premises.name, TemporaryAccommodationPremisesEntity::class.java, premises.id) } returns true
      every { probationRegionRepositoryMock.findByIdOrNull(nonExistProbationRegionId) } returns null
      every { localAuthorityAreaRepositoryMock.findByIdOrNull(localAuthorityArea.id) } returns localAuthorityArea
      every { probationDeliveryUnitRepositoryMock.findByIdAndProbationRegionId(probationDeliveryUnit.id, any()) } returns probationDeliveryUnit

      val result = premisesService.updatePremises(
        premises = premises,
        reference = premises.name,
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = premises.postcode,
        localAuthorityAreaId = premises.localAuthorityArea?.id,
        probationRegionId = nonExistProbationRegionId,
        characteristicIds = emptyList(),
        notes = premises.notes,
        probationDeliveryUnitId = premises.probationDeliveryUnit?.id!!,
        turnaroundWorkingDays = premises.turnaroundWorkingDays,
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.probationRegionId", "doesNotExist")
    }

    @Test
    fun `When update a premises with a non exist local authority returns a FieldValidationError with the correct message`() {
      val premises = updatePremisesEntity()
      val probationRegion = premises.probationRegion
      val probationDeliveryUnit = premises.probationDeliveryUnit!!
      val nonExistLocalAuthorityId = UUID.randomUUID()

      every { premisesRepositoryMock.nameIsUniqueForType(premises.name, TemporaryAccommodationPremisesEntity::class.java, premises.id) } returns true
      every { probationRegionRepositoryMock.findByIdOrNull(premises.probationRegion.id) } returns probationRegion
      every { localAuthorityAreaRepositoryMock.findByIdOrNull(nonExistLocalAuthorityId) } returns null
      every { probationDeliveryUnitRepositoryMock.findByIdAndProbationRegionId(probationDeliveryUnit.id, any()) } returns probationDeliveryUnit

      val result = premisesService.updatePremises(
        premises = premises,
        reference = premises.name,
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = premises.postcode,
        localAuthorityAreaId = nonExistLocalAuthorityId,
        probationRegionId = premises.probationRegion.id,
        characteristicIds = emptyList(),
        notes = premises.notes,
        probationDeliveryUnitId = premises.probationDeliveryUnit?.id!!,
        turnaroundWorkingDays = premises.turnaroundWorkingDays,
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.localAuthorityAreaId", "doesNotExist")
    }

    @Test
    fun `When update a premises with a non exist probation delivery unit returns a FieldValidationError with the correct message`() {
      val premises = updatePremisesEntity()
      val probationRegion = premises.probationRegion
      val localAuthorityArea = premises.localAuthorityArea!!
      val nonExistPduId = UUID.randomUUID()

      every { premisesRepositoryMock.nameIsUniqueForType(premises.name, TemporaryAccommodationPremisesEntity::class.java, premises.id) } returns true
      every { probationRegionRepositoryMock.findByIdOrNull(probationRegion.id) } returns probationRegion
      every { localAuthorityAreaRepositoryMock.findByIdOrNull(localAuthorityArea.id) } returns localAuthorityArea
      every { probationDeliveryUnitRepositoryMock.findByIdAndProbationRegionId(nonExistPduId, probationRegion.id) } returns null

      val result = premisesService.updatePremises(
        premises = premises,
        reference = premises.name,
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = premises.postcode,
        localAuthorityAreaId = premises.localAuthorityArea?.id!!,
        probationRegionId = premises.probationRegion.id,
        characteristicIds = emptyList(),
        notes = premises.notes,
        probationDeliveryUnitId = nonExistPduId,
        turnaroundWorkingDays = premises.turnaroundWorkingDays,
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.probationDeliveryUnitId", "doesNotExist")
    }

    @Test
    fun `When update a premises with empty reference returns a FieldValidationError with the correct message`() {
      val premises = updatePremisesEntity()

      mockCommonPremisesDependencies(premises)

      val result = premisesService.updatePremises(
        premises = premises,
        reference = "",
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = premises.postcode,
        localAuthorityAreaId = premises.localAuthorityArea?.id!!,
        probationRegionId = premises.probationRegion.id,
        characteristicIds = emptyList(),
        notes = premises.notes,
        probationDeliveryUnitId = premises.probationDeliveryUnit?.id!!,
        turnaroundWorkingDays = premises.turnaroundWorkingDays,
      )
      assertThatCasResult(result).isFieldValidationError().hasMessage("$.reference", "empty")
    }

    @Test
    fun `When update a premises with existing premises reference returns a FieldValidationError with the correct message`() {
      val premises = updatePremisesEntity()

      mockCommonPremisesDependencies(premises)
      every { premisesRepositoryMock.nameIsUniqueForType(premises.name, TemporaryAccommodationPremisesEntity::class.java, premises.id) } returns false

      val result = premisesService.updatePremises(
        premises = premises,
        reference = premises.name,
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = premises.postcode,
        localAuthorityAreaId = premises.localAuthorityArea?.id!!,
        probationRegionId = premises.probationRegion.id,
        characteristicIds = emptyList(),
        notes = premises.notes,
        probationDeliveryUnitId = premises.probationDeliveryUnit?.id!!,
        turnaroundWorkingDays = premises.turnaroundWorkingDays,
      )
      assertThatCasResult(result).isFieldValidationError().hasMessage("$.reference", "notUnique")
    }

    @Test
    fun `When update a premises with empty address returns a FieldValidationError with the correct message`() {
      val premises = updatePremisesEntity()

      mockCommonPremisesDependencies(premises)

      val result = premisesService.updatePremises(
        premises = premises,
        reference = premises.name,
        addressLine1 = "",
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = premises.postcode,
        localAuthorityAreaId = premises.localAuthorityArea?.id!!,
        probationRegionId = premises.probationRegion.id,
        characteristicIds = emptyList(),
        notes = premises.notes,
        probationDeliveryUnitId = premises.probationDeliveryUnit?.id!!,
        turnaroundWorkingDays = premises.turnaroundWorkingDays,
      )
      assertThatCasResult(result).isFieldValidationError().hasMessage("$.address", "empty")
    }

    @Test
    fun `When update a premises with empty postcode returns a FieldValidationError with the correct message`() {
      val premises = updatePremisesEntity()

      mockCommonPremisesDependencies(premises)

      val result = premisesService.updatePremises(
        premises = premises,
        reference = premises.name,
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = "",
        localAuthorityAreaId = premises.localAuthorityArea?.id!!,
        probationRegionId = premises.probationRegion.id,
        characteristicIds = emptyList(),
        notes = premises.notes,
        probationDeliveryUnitId = premises.probationDeliveryUnit?.id!!,
        turnaroundWorkingDays = premises.turnaroundWorkingDays,
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.postcode", "empty")
    }

    @Test
    fun `When update a premises with turnaround working days in negative returns a FieldValidationError with the correct message`() {
      val premises = updatePremisesEntity()

      mockCommonPremisesDependencies(premises)

      val result = premisesService.updatePremises(
        premises = premises,
        reference = premises.name,
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = premises.postcode,
        localAuthorityAreaId = premises.localAuthorityArea?.id!!,
        probationRegionId = premises.probationRegion.id,
        characteristicIds = emptyList(),
        notes = premises.notes,
        probationDeliveryUnitId = premises.probationDeliveryUnit?.id!!,
        turnaroundWorkingDays = -2,
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.turnaroundWorkingDays", "isNotAPositiveInteger")
    }

    @Test
    fun `When update a premises with a non exist characteristic returns FieldValidationError with the correct message`() {
      val premises = updatePremisesEntity()
      val nonExistCharacteristicId = UUID.randomUUID()

      mockCommonPremisesDependencies(premises)
      every { characteristicServiceMock.getCharacteristic(nonExistCharacteristicId) } returns null

      val result = premisesService.updatePremises(
        premises = premises,
        reference = premises.name,
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = premises.postcode,
        localAuthorityAreaId = premises.localAuthorityArea?.id!!,
        probationRegionId = premises.probationRegion.id,
        characteristicIds = listOf(nonExistCharacteristicId),
        notes = premises.notes,
        probationDeliveryUnitId = premises.probationDeliveryUnit?.id!!,
        turnaroundWorkingDays = 2,
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.characteristics[0]", "doesNotExist")
    }

    @Test
    fun `When update a bedspace with a wrong model scope characteristic returns FieldValidationError with the correct message`() {
      val premises = updatePremisesEntity()
      val characteristicEntityFactory = CharacteristicEntityFactory()

      val premisesCharacteristic = characteristicEntityFactory
        .withModelScope("room")
        .withServiceScope(ServiceName.temporaryAccommodation.value)
        .produce()

      mockCommonPremisesDependencies(premises)
      every { characteristicServiceMock.getCharacteristic(premisesCharacteristic.id) } returns premisesCharacteristic
      every { characteristicServiceMock.modelScopeMatches(premisesCharacteristic, any()) } returns false

      val result = premisesService.updatePremises(
        premises = premises,
        reference = premises.name,
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = premises.postcode,
        localAuthorityAreaId = premises.localAuthorityArea?.id!!,
        probationRegionId = premises.probationRegion.id,
        characteristicIds = listOf(premisesCharacteristic.id),
        notes = premises.notes,
        probationDeliveryUnitId = premises.probationDeliveryUnit?.id!!,
        turnaroundWorkingDays = 2,
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.characteristics[0]", "incorrectCharacteristicModelScope")
    }

    @Test
    fun `When create a new bedspace with a wrong service scope characteristic returns FieldValidationError with the correct message`() {
      val premises = updatePremisesEntity()
      val characteristicEntityFactory = CharacteristicEntityFactory()

      val premisesCharacteristic = characteristicEntityFactory
        .withModelScope("premises")
        .withServiceScope(ServiceName.approvedPremises.value)
        .produce()

      mockCommonPremisesDependencies(premises)
      every { characteristicServiceMock.getCharacteristic(premisesCharacteristic.id) } returns premisesCharacteristic
      every { characteristicServiceMock.modelScopeMatches(premisesCharacteristic, any()) } returns true
      every { characteristicServiceMock.serviceScopeMatches(premisesCharacteristic, any()) } returns false

      val result = premisesService.updatePremises(
        premises = premises,
        reference = premises.name,
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = premises.postcode,
        localAuthorityAreaId = premises.localAuthorityArea?.id!!,
        probationRegionId = premises.probationRegion.id,
        characteristicIds = listOf(premisesCharacteristic.id),
        notes = premises.notes,
        probationDeliveryUnitId = premises.probationDeliveryUnit?.id!!,
        turnaroundWorkingDays = 2,
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.characteristics[0]", "incorrectCharacteristicServiceScope")
    }

    private fun updatePremisesEntity(): TemporaryAccommodationPremisesEntity {
      val probationRegion = ProbationRegionEntityFactory()
        .withApArea(ApAreaEntityFactory().produce())
        .produce()

      val localAuthority = LocalAuthorityEntityFactory()
        .produce()

      val probationDeliveryUnit = ProbationDeliveryUnitEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      return temporaryAccommodationPremisesFactory
        .withService(ServiceName.temporaryAccommodation.value)
        .withName("Reference Name")
        .withAddressLine1("address 1")
        .withProbationRegion(probationRegion)
        .withProbationDeliveryUnit(probationDeliveryUnit)
        .withLocalAuthorityArea(localAuthority)
        .produce()
    }

    private fun mockCommonPremisesDependencies(premises: TemporaryAccommodationPremisesEntity) {
      val probationRegion = premises.probationRegion
      val localAuthorityArea = premises.localAuthorityArea!!
      val probationDeliveryUnit = premises.probationDeliveryUnit!!
      every { premisesRepositoryMock.nameIsUniqueForType(premises.name, TemporaryAccommodationPremisesEntity::class.java, premises.id) } returns true
      every { probationRegionRepositoryMock.findByIdOrNull(probationRegion.id) } returns probationRegion
      every { localAuthorityAreaRepositoryMock.findByIdOrNull(localAuthorityArea.id) } returns localAuthorityArea
      every { probationDeliveryUnitRepositoryMock.findByIdAndProbationRegionId(probationDeliveryUnit.id, probationRegion.id) } returns probationDeliveryUnit
    }
  }

  @Nested
  inner class GetBedspace {
    @Test
    fun `When get a bedspace returns Success with correct result when validation passed`() {
      val premises = temporaryAccommodationPremisesFactory.produce()

      val room = RoomEntityFactory()
        .withName(randomStringMultiCaseWithNumbers(10))
        .withPremises(premises)
        .produce()

      val bedspace = BedEntityFactory()
        .withRoom(room)
        .withStartDate(LocalDate.now().minusDays(5))
        .produce()

      every { bedRepositoryMock.findCas3Bedspace(premises.id, bedspace.id) } returns bedspace

      val result = premisesService.getBedspace(premises.id, bedspace.id)

      assertThatCasResult(result).isSuccess().with { bed ->
        assertThat(bed.name).isEqualTo(bedspace.name)
        assertThat(bed.startDate).isEqualTo(bedspace.startDate)
        assertThat(bed.room).isEqualTo(room)
        assertThat(bed.room.premises).isEqualTo(premises)
      }
    }

    @Test
    fun `When get a bedspace returns not found as bedspace does not exist`() {
      val nonExistingBedspaceId = UUID.randomUUID()

      val premises = temporaryAccommodationPremisesFactory.produce()

      every { bedRepositoryMock.findCas3Bedspace(premises.id, nonExistingBedspaceId) } returns null

      val result = premisesService.getBedspace(premises.id, nonExistingBedspaceId)

      assertThatCasResult(result).isNotFound("Bedspace", nonExistingBedspaceId)
    }
  }

  @Nested
  inner class GetArchiveHistory {
    @Test
    fun `When getBedspaceArchiveHistory returns Success with empty list of histories`() {
      val bedspaceId = UUID.randomUUID()

      every { cas3DomainEventServiceMock.getBedspaceDomainEvents(bedspaceId, listOf(DomainEventType.CAS3_BEDSPACE_ARCHIVED, DomainEventType.CAS3_BEDSPACE_UNARCHIVED)) } returns emptyList()

      val result = premisesService.getBedspaceArchiveHistory(bedspaceId)

      assertThatCasResult(result).isSuccess().with { archiveHistory ->
        assertThat(archiveHistory).isEqualTo(emptyList<Cas3BedspaceArchiveAction>())
      }
    }

    @Test
    fun `When getBedspaceArchiveHistory returns Success with list of events`() {
      val bedspaceId = UUID.randomUUID()
      val premisesId = UUID.randomUUID()
      val userId = UUID.randomUUID()

      // Archive event scheduled for tomorrow
      val endDateTomorrowArchive = LocalDate.now().plusDays(1)
      val dataTomorrowArchive = createCAS3BedspaceArchiveEvent(premisesId = premisesId, bedspaceId = bedspaceId, userId = userId, endDate = endDateTomorrowArchive)
      val domainEventTomorrow = createArchiveDomainEvent(dataTomorrowArchive)

      // Archive event scheduled for today
      val endDateTodayArchive = LocalDate.now()
      val dataTodayArchive = createCAS3BedspaceArchiveEvent(premisesId = premisesId, bedspaceId = bedspaceId, userId = userId, endDate = endDateTodayArchive)
      val domainEventToday = createArchiveDomainEvent(dataTodayArchive)

      // Archive event scheduled for yesterday
      val endDateYesterdayArchive = LocalDate.now().minusDays(1)
      val dataYesterdayArchive = createCAS3BedspaceArchiveEvent(premisesId = premisesId, bedspaceId = bedspaceId, userId = userId, endDate = endDateYesterdayArchive)
      val domainEventYesterday = createArchiveDomainEvent(dataYesterdayArchive)

      // Unarchive event scheduled for in 2 days
      val newStartDateIn2DaysUnarchive = LocalDate.now().plusDays(2)
      val dataIn2DaysUnarchive = createCAS3BedspaceUnarchiveEvent(premisesId = premisesId, bedspaceId = bedspaceId, userId = userId, newStartDate = newStartDateIn2DaysUnarchive)
      val domainEventIn2Days = createUnarchiveDomainEvent(dataIn2DaysUnarchive)

      // Unarchive event scheduled for 2 days ago
      val newStartDate2DaysAgoUnarchive = LocalDate.now().minusDays(2)
      val data2DaysAgoUnarchive = createCAS3BedspaceUnarchiveEvent(premisesId = premisesId, bedspaceId = bedspaceId, userId = userId, newStartDate = newStartDate2DaysAgoUnarchive)
      val domainEvent2DaysAgo = createUnarchiveDomainEvent(data2DaysAgoUnarchive)

      // Unarchive event scheduled for 3 days ago
      val newStartDate3DaysAgoUnarchive = LocalDate.now().minusDays(3)
      val data3DaysAgoUnarchive = createCAS3BedspaceUnarchiveEvent(premisesId = premisesId, bedspaceId = bedspaceId, userId = userId, newStartDate = newStartDate3DaysAgoUnarchive)
      val domainEvent3DaysAgo = createUnarchiveDomainEvent(data3DaysAgoUnarchive)

      every { cas3DomainEventServiceMock.getBedspaceDomainEvents(bedspaceId, listOf(DomainEventType.CAS3_BEDSPACE_ARCHIVED, DomainEventType.CAS3_BEDSPACE_UNARCHIVED)) } returns
        listOf(
          domainEventTomorrow,
          domainEventToday,
          domainEventYesterday,
          domainEventIn2Days,
          domainEvent2DaysAgo,
          domainEvent3DaysAgo,
        )

      val result = premisesService.getBedspaceArchiveHistory(bedspaceId)

      assertThatCasResult(result).isSuccess().with { archiveHistory ->
        assertThat(archiveHistory).isEqualTo(
          listOf(
            Cas3BedspaceArchiveAction(
              Cas3BedspaceStatus.online,
              data3DaysAgoUnarchive.eventDetails.newStartDate,
            ),
            Cas3BedspaceArchiveAction(
              Cas3BedspaceStatus.online,
              data2DaysAgoUnarchive.eventDetails.newStartDate,
            ),
            Cas3BedspaceArchiveAction(
              Cas3BedspaceStatus.archived,
              dataYesterdayArchive.eventDetails.endDate,
            ),
            Cas3BedspaceArchiveAction(
              Cas3BedspaceStatus.archived,
              dataTodayArchive.eventDetails.endDate,
            ),
          ),
        )
      }
    }

    @Test
    fun `When getBedspacesArchiveHistory returns Success with list of events`() {
      val bedspaceOneId = UUID.randomUUID()
      val bedspaceTwoId = UUID.randomUUID()
      val bedspaceThreeId = UUID.randomUUID()
      val bedspacesIds = listOf(bedspaceOneId, bedspaceTwoId, bedspaceThreeId)
      val premisesId = UUID.randomUUID()
      val userId = UUID.randomUUID()

      val bedOneArchiveEventOneMonthAgo = createCAS3BedspaceArchiveEvent(
        premisesId = premisesId,
        bedspaceId = bedspaceOneId,
        userId = userId,
        endDate = LocalDate.now().minusMonths(1),
      )
      val bedOneArchiveDomainEventOneMonthAgo = createArchiveDomainEvent(bedOneArchiveEventOneMonthAgo)

      val bedOneArchiveEventOneWeekAgo = createCAS3BedspaceArchiveEvent(
        premisesId = premisesId,
        bedspaceId = bedspaceOneId,
        userId = userId,
        endDate = LocalDate.now().minusWeeks(1),
      )
      val bedOneArchiveDomainEventOneWeekAgo = createArchiveDomainEvent(bedOneArchiveEventOneWeekAgo)

      val bedThreeArchiveEventTwoMonthsAgo = createCAS3BedspaceArchiveEvent(
        premisesId = premisesId,
        bedspaceId = bedspaceThreeId,
        userId = userId,
        endDate = LocalDate.now().minusWeeks(2),
      )
      val bedThreeArchiveDomainEventTwoMonthsAgo = createArchiveDomainEvent(bedThreeArchiveEventTwoMonthsAgo)

      val bedOneUnarchiveEventTwoWeeksAgo = createCAS3BedspaceUnarchiveEvent(
        premisesId = premisesId,
        bedspaceId = bedspaceOneId,
        userId = userId,
        newStartDate = LocalDate.now().minusWeeks(2),
      )
      val bedOneUnarchiveDomainEventTwoWeeksAgo = createUnarchiveDomainEvent(bedOneUnarchiveEventTwoWeeksAgo)

      val bedThreeUnarchiveEventTwoWeeksAgo = createCAS3BedspaceUnarchiveEvent(
        premisesId = premisesId,
        bedspaceId = bedspaceThreeId,
        userId = userId,
        newStartDate = LocalDate.now().minusWeeks(1),
      )
      val bedThreeUnarchiveDomainEventOneWeeksAgo = createUnarchiveDomainEvent(bedThreeUnarchiveEventTwoWeeksAgo)

      every {
        cas3DomainEventServiceMock.getBedspacesDomainEvents(
          bedspacesIds,
          listOf(DomainEventType.CAS3_BEDSPACE_ARCHIVED, DomainEventType.CAS3_BEDSPACE_UNARCHIVED),
        )
      } returns
        listOf(
          bedOneArchiveDomainEventOneMonthAgo,
          bedOneArchiveDomainEventOneWeekAgo,
          bedThreeArchiveDomainEventTwoMonthsAgo,
          bedOneUnarchiveDomainEventTwoWeeksAgo,
          bedThreeUnarchiveDomainEventOneWeeksAgo,
        )

      val result = premisesService.getBedspacesArchiveHistory(bedspacesIds)

      assertAll(
        {
          assertThat(result.count()).isEqualTo(3)
          assertThat(result[0].bedspaceId).isEqualTo(bedspaceOneId)
          assertThat(result[0].actions).isEqualTo(
            listOf(
              Cas3BedspaceArchiveAction(
                Cas3BedspaceStatus.archived,
                bedOneArchiveEventOneMonthAgo.eventDetails.endDate,
              ),
              Cas3BedspaceArchiveAction(
                Cas3BedspaceStatus.online,
                bedOneUnarchiveEventTwoWeeksAgo.eventDetails.newStartDate,
              ),
              Cas3BedspaceArchiveAction(
                Cas3BedspaceStatus.archived,
                bedOneArchiveEventOneWeekAgo.eventDetails.endDate,
              ),
            ),
          )
          assertThat(result[1].bedspaceId).isEqualTo(bedspaceTwoId)
          assertThat(result[1].actions).isEmpty()
          assertThat(result[2].bedspaceId).isEqualTo(bedspaceThreeId)
          assertThat(result[2].actions).isEqualTo(
            listOf(
              Cas3BedspaceArchiveAction(
                Cas3BedspaceStatus.archived,
                bedThreeArchiveEventTwoMonthsAgo.eventDetails.endDate,
              ),
              Cas3BedspaceArchiveAction(
                Cas3BedspaceStatus.online,
                bedThreeUnarchiveEventTwoWeeksAgo.eventDetails.newStartDate,
              ),
            ),
          )
        },
      )
    }

    private fun createArchiveDomainEvent(data: CAS3BedspaceArchiveEvent) = createBedspaceDomainEvent(
      data.id,
      data.eventDetails.bedspaceId,
      data.timestamp.atOffset(ZoneOffset.UTC),
      objectMapper.writeValueAsString(data),
      DomainEventType.CAS3_BEDSPACE_ARCHIVED,
    )

    private fun createCAS3BedspaceArchiveEvent(premisesId: UUID, bedspaceId: UUID, userId: UUID, endDate: LocalDate): CAS3BedspaceArchiveEvent {
      val eventId = UUID.randomUUID()
      val occurredAt = OffsetDateTime.now()
      return CAS3BedspaceArchiveEvent(
        id = eventId,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.bedspaceArchived,
        eventDetails = CAS3BedspaceArchiveEventDetails(
          bedspaceId = bedspaceId,
          userId = userId,
          premisesId = premisesId,
          endDate = endDate,
        ),
      )
    }

    private fun createUnarchiveDomainEvent(data: CAS3BedspaceUnarchiveEvent) = createBedspaceDomainEvent(
      data.id,
      data.eventDetails.bedspaceId,
      data.timestamp.atOffset(ZoneOffset.UTC),
      objectMapper.writeValueAsString(data),
      DomainEventType.CAS3_BEDSPACE_UNARCHIVED,
    )

    private fun createCAS3BedspaceUnarchiveEvent(premisesId: UUID, bedspaceId: UUID, userId: UUID, newStartDate: LocalDate): CAS3BedspaceUnarchiveEvent {
      val eventId = UUID.randomUUID()
      val occurredAt = OffsetDateTime.now()
      return CAS3BedspaceUnarchiveEvent(
        id = eventId,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.bedspaceUnarchived,
        eventDetails = CAS3BedspaceUnarchiveEventDetails(
          bedspaceId = bedspaceId,
          premisesId = premisesId,
          userId = userId,
          currentStartDate = LocalDate.now(),
          currentEndDate = LocalDate.now(),
          newStartDate = newStartDate,
        ),
      )
    }

    private fun createBedspaceDomainEvent(id: UUID, bedspaceId: UUID, occurredAt: OffsetDateTime, data: String, type: DomainEventType) = DomainEventEntityFactory()
      .withId(id)
      .withCas3BedspaceId(bedspaceId)
      .withType(type)
      .withData(data)
      .withOccurredAt(occurredAt)
      .produce()
  }

  @Nested
  inner class CreateBedspace {
    @Test
    fun `When create a new bedspace returns Success with correct result when validation passed`() {
      val premises = temporaryAccommodationPremisesFactory.produce()

      val room = RoomEntityFactory()
        .withName(randomStringMultiCaseWithNumbers(10))
        .withPremises(premises)
        .produce()

      val bedspace = BedEntityFactory()
        .withRoom(room)
        .withStartDate(LocalDate.now().minusDays(5))
        .produce()

      every { roomRepositoryMock.save(any()) } returns room
      every { bedRepositoryMock.save(any()) } returns bedspace

      val result = premisesService.createBedspace(premises, room.name, bedspace.startDate!!, null, emptyList())

      assertThatCasResult(result).isSuccess().with { bed ->
        assertThat(bed.name).isEqualTo("default-bed")
        assertThat(bed.startDate).isEqualTo(bedspace.startDate)
        assertThat(bed.room).isEqualTo(room)
        assertThat(bed.room.premises).isEqualTo(premises)
      }
    }

    @Test
    fun `When create a new bedspace with empty bedspace reference returns FieldValidationError with the correct message`() {
      val premises = temporaryAccommodationPremisesFactory.produce()

      val result = premisesService.createBedspace(premises, "", LocalDate.now().minusDays(7), null, emptyList())

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.reference", "empty")
    }

    @Test
    fun `When create a new bedspace with a start date more than 7 days in the past returns FieldValidationError with the correct message`() {
      val premises = temporaryAccommodationPremisesFactory.produce()

      val result = premisesService.createBedspace(premises, randomStringMultiCaseWithNumbers(10), LocalDate.now().minusDays(10), null, emptyList())

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.startDate", "invalidStartDateInThePast")
    }

    @Test
    fun `When create a new bedspace with a start date more than 7 days in the future returns FieldValidationError with the correct message`() {
      val premises = temporaryAccommodationPremisesFactory.produce()

      val result = premisesService.createBedspace(premises, randomStringMultiCaseWithNumbers(10), LocalDate.now().plusDays(15), null, emptyList())

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.startDate", "invalidStartDateInTheFuture")
    }

    @Test
    fun `When create a new bedspace with a non exist characteristic returns FieldValidationError with the correct message`() {
      val premises = temporaryAccommodationPremisesFactory.produce()

      val room = RoomEntityFactory()
        .withName(randomStringMultiCaseWithNumbers(10))
        .withPremises(premises)
        .produce()

      val bedspace = BedEntityFactory()
        .withRoom(room)
        .withStartDate(LocalDate.now().plusDays(3))
        .produce()

      val nonExistCharacteristicId = UUID.randomUUID()

      every { characteristicServiceMock.getCharacteristic(nonExistCharacteristicId) } returns null

      val result = premisesService.createBedspace(premises, bedspace.name, bedspace.startDate!!, null, listOf(nonExistCharacteristicId))

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.characteristics[0]", "doesNotExist")
    }

    @Test
    fun `When create a new bedspace with a wrong model scope characteristic returns FieldValidationError with the correct message`() {
      val premises = temporaryAccommodationPremisesFactory.produce()
      val characteristicEntityFactory = CharacteristicEntityFactory()

      val room = RoomEntityFactory()
        .withName(randomStringMultiCaseWithNumbers(10))
        .withPremises(premises)
        .produce()

      val bedspace = BedEntityFactory()
        .withRoom(room)
        .withStartDate(LocalDate.now().plusDays(3))
        .produce()

      val premisesCharacteristic = characteristicEntityFactory
        .withModelScope("premises")
        .withServiceScope(ServiceName.temporaryAccommodation.value)
        .produce()

      every { characteristicServiceMock.getCharacteristic(premisesCharacteristic.id) } returns premisesCharacteristic
      every { characteristicServiceMock.modelScopeMatches(premisesCharacteristic, any()) } returns false

      val result = premisesService.createBedspace(premises, bedspace.name, bedspace.startDate!!, null, listOf(premisesCharacteristic.id))

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.characteristics[0]", "incorrectCharacteristicModelScope")
    }

    @Test
    fun `When create a new bedspace with a wrong service scope characteristic returns FieldValidationError with the correct message`() {
      val premises = temporaryAccommodationPremisesFactory.produce()
      val characteristicEntityFactory = CharacteristicEntityFactory()

      val room = RoomEntityFactory()
        .withName(randomStringMultiCaseWithNumbers(10))
        .withPremises(premises)
        .produce()

      val bedspace = BedEntityFactory()
        .withRoom(room)
        .withStartDate(LocalDate.now().plusDays(3))
        .produce()

      val premisesCharacteristic = characteristicEntityFactory
        .withModelScope("room")
        .withServiceScope(ServiceName.approvedPremises.value)
        .produce()

      every { characteristicServiceMock.getCharacteristic(premisesCharacteristic.id) } returns premisesCharacteristic
      every { characteristicServiceMock.modelScopeMatches(premisesCharacteristic, any()) } returns true
      every { characteristicServiceMock.serviceScopeMatches(premisesCharacteristic, any()) } returns false

      val result = premisesService.createBedspace(premises, bedspace.name, bedspace.startDate!!, null, listOf(premisesCharacteristic.id))

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.characteristics[0]", "incorrectCharacteristicServiceScope")
    }

    @Test
    fun `When create a new bedspace with reference less than 3 characters returns FieldValidationError with the correct message`() {
      val premises = temporaryAccommodationPremisesFactory.produce()

      val result = premisesService.createBedspace(premises, "AB", LocalDate.now().minusDays(1), null, emptyList())

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.reference", "bedspaceReferenceNotMeetMinimumLength")
    }

    @Test
    fun `When create a new bedspace with reference containing only special characters returns FieldValidationError with the correct message`() {
      val premises = temporaryAccommodationPremisesFactory.produce()

      val result = premisesService.createBedspace(premises, "!@#$%", LocalDate.now().minusDays(1), null, emptyList())

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.reference", "bedspaceReferenceMustIncludeLetterOrNumber")
    }

    @Test
    fun `When create a new bedspace with duplicate reference returns FieldValidationError with the correct message`() {
      val premises = temporaryAccommodationPremisesFactory.produce()

      val existingRoom = RoomEntityFactory()
        .withName("EXISTING_REF")
        .withPremises(premises)
        .produce()

      premises.rooms.add(existingRoom)

      val result = premisesService.createBedspace(premises, "existing_ref", LocalDate.now().minusDays(1), null, emptyList())

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.reference", "bedspaceReferenceExists")
    }
  }

  @Nested
  inner class UpdateBedspace {
    @Test
    fun `When update a bedspace returns Success with correct result when validation passed`() {
      val characteristicEntityFactory = CharacteristicEntityFactory()
      val (premises, bedspace) = createPremisesAndBedspace()
      val updateBedspaceReference = randomStringMultiCaseWithNumbers(10)
      val updateBedspaceNotes = randomStringMultiCaseWithNumbers(100)
      val updateBedspaceCharacteristic = characteristicEntityFactory
        .withModelScope("room")
        .withServiceScope(ServiceName.temporaryAccommodation.value)
        .produceMany()
        .take(3)
        .toList()

      val updatedRoom = RoomEntity(
        id = bedspace.room.id,
        name = updateBedspaceReference,
        code = bedspace.room.code,
        notes = updateBedspaceNotes,
        characteristics = updateBedspaceCharacteristic.toMutableList(),
        premises = premises,
        beds = mutableListOf(),
      )

      val updatedBedspace = BedEntity(
        id = bedspace.id,
        name = "default-bed",
        code = bedspace.code,
        startDate = bedspace.startDate,
        endDate = bedspace.endDate,
        room = updatedRoom,
        createdAt = bedspace.createdAt,
      )

      updatedRoom.beds.add(updatedBedspace)

      every { bedRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace
      every { roomRepositoryMock.findByIdOrNull(bedspace.room.id) } returns bedspace.room
      every { characteristicServiceMock.getCharacteristic(any()) } answers {
        val characteristicId = it.invocation.args[0] as UUID
        updateBedspaceCharacteristic.firstOrNull { it.id == characteristicId }
      }
      every { characteristicServiceMock.modelScopeMatches(any(), any()) } returns true
      every { characteristicServiceMock.serviceScopeMatches(any(), any()) } returns true
      every { roomRepositoryMock.save(any()) } returns updatedRoom

      val result = premisesService.updateBedspace(premises, bedspace.id, updateBedspaceReference, updateBedspaceNotes, updateBedspaceCharacteristic.map { it.id })

      assertThatCasResult(result).isSuccess().with { bed ->
        assertThat(bed.room.name).isEqualTo(updateBedspaceReference)
        assertThat(bed.room.notes).isEqualTo(updateBedspaceNotes)
        assertThat(bed.room).isEqualTo(updatedRoom)
        assertThat(bed.room.premises).isEqualTo(premises)
      }
    }

    @Test
    fun `When updating a non existing bedspace returns a NotFound with the correct message`() {
      val (premises, _) = createPremisesAndBedspace()
      val nonExistingBedspaceId = UUID.randomUUID()

      every { bedRepositoryMock.findByIdOrNull(nonExistingBedspaceId) } returns null

      val result = premisesService.updateBedspace(premises, nonExistingBedspaceId, randomStringMultiCaseWithNumbers(10), randomStringMultiCaseWithNumbers(100), emptyList())

      assertThatCasResult(result).isNotFound("Bedspace", nonExistingBedspaceId)
    }

    @Test
    fun `When updating a bedspace that belongs to different premises returns a NotFound with the correct message`() {
      val anotherPremises = temporaryAccommodationPremisesFactory.produce()
      val (_, bedspace) = createPremisesAndBedspace()

      every { bedRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace
      every { roomRepositoryMock.findByIdOrNull(bedspace.room.id) } returns bedspace.room

      val result = premisesService.updateBedspace(anotherPremises, bedspace.id, randomStringMultiCaseWithNumbers(10), randomStringMultiCaseWithNumbers(100), emptyList())

      assertThatCasResult(result).isGeneralValidationError("The bedspace does not belong to the specified premises.")
    }

    @Test
    fun `When update a bedspace with an empty bedspace reference returns FieldValidationError with the correct message`() {
      val (premises, bedspace) = createPremisesAndBedspace()

      every { bedRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace
      every { roomRepositoryMock.findByIdOrNull(bedspace.room.id) } returns bedspace.room

      val result = premisesService.updateBedspace(premises, bedspace.id, "", randomStringMultiCaseWithNumbers(100), emptyList())

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.reference", "empty")
    }

    @Test
    fun `When update a bedspace with a wrong model scope characteristic returns FieldValidationError with the correct message`() {
      val characteristicEntityFactory = CharacteristicEntityFactory()
      val (premises, bedspace) = createPremisesAndBedspace()

      val premisesCharacteristic = characteristicEntityFactory
        .withModelScope("premises")
        .withServiceScope(ServiceName.temporaryAccommodation.value)
        .produce()

      every { bedRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace
      every { roomRepositoryMock.findByIdOrNull(bedspace.room.id) } returns bedspace.room
      every { characteristicServiceMock.getCharacteristic(premisesCharacteristic.id) } returns premisesCharacteristic
      every { characteristicServiceMock.modelScopeMatches(premisesCharacteristic, any()) } returns false

      val result = premisesService.updateBedspace(premises, bedspace.id, randomStringMultiCaseWithNumbers(10), null, listOf(premisesCharacteristic.id))

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.characteristics[0]", "incorrectCharacteristicModelScope")
    }

    @Test
    fun `When update a bedspace with a wrong service scope characteristic returns FieldValidationError with the correct message`() {
      val characteristicEntityFactory = CharacteristicEntityFactory()
      val (premises, bedspace) = createPremisesAndBedspace()

      val premisesCharacteristic = characteristicEntityFactory
        .withModelScope("room")
        .withServiceScope(ServiceName.approvedPremises.value)
        .produce()

      every { bedRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace
      every { roomRepositoryMock.findByIdOrNull(bedspace.room.id) } returns bedspace.room
      every { characteristicServiceMock.getCharacteristic(premisesCharacteristic.id) } returns premisesCharacteristic
      every { characteristicServiceMock.modelScopeMatches(premisesCharacteristic, any()) } returns true
      every { characteristicServiceMock.serviceScopeMatches(premisesCharacteristic, any()) } returns false

      val result = premisesService.updateBedspace(premises, bedspace.id, randomStringMultiCaseWithNumbers(10), null, listOf(premisesCharacteristic.id))

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.characteristics[0]", "incorrectCharacteristicServiceScope")
    }

    @Test
    fun `When update a bedspace with reference less than 3 characters returns FieldValidationError with the correct message`() {
      val (premises, bedspace) = createPremisesAndBedspace()

      every { bedRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace

      val result = premisesService.updateBedspace(premises, bedspace.id, "AB", null, emptyList())

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.reference", "bedspaceReferenceNotMeetMinimumLength")
    }

    @Test
    fun `When update a bedspace with reference containing only special characters returns FieldValidationError with the correct message`() {
      val (premises, bedspace) = createPremisesAndBedspace()

      every { bedRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace

      val result = premisesService.updateBedspace(premises, bedspace.id, "!@#$%", null, emptyList())

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.reference", "bedspaceReferenceMustIncludeLetterOrNumber")
    }

    @Test
    fun `When update a bedspace with duplicate reference returns FieldValidationError with the correct message`() {
      val (premises, bedspace) = createPremisesAndBedspace()

      val existingRoom = RoomEntityFactory()
        .withName("EXISTING_REF")
        .withPremises(premises)
        .produce()

      premises.rooms.add(existingRoom)

      every { bedRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace

      val result = premisesService.updateBedspace(premises, bedspace.id, "existing_ref", null, emptyList())

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.reference", "bedspaceReferenceExists")
    }
  }

  @Nested
  inner class ArchiveBedspace {
    @Test
    fun `When archive a bedspace returns Success with correct result when validation passed`() {
      val (premises, bedspaceOne) = createPremisesAndBedspace()
      val bedspaceTwo = createBedspace(premises)

      val archiveDate = LocalDate.now().plusDays(3)
      val archivedBedspaceOne = bedspaceOne.copy(
        endDate = archiveDate,
      )

      every { bedRepositoryMock.findByIdOrNull(bedspaceOne.id) } returns bedspaceOne
      every { bookingRepositoryMock.findActiveOverlappingBookingByBed(bedspaceOne.id, archiveDate) } returns emptyList()
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDate(bedspaceOne.id, archiveDate) } returns emptyList()
      every { bedRepositoryMock.save(any()) } returns bedspaceOne
      every { cas3DomainEventServiceMock.saveBedspaceArchiveEvent(bedspaceOne) } returns Unit
      every { bedRepositoryMock.findByRoomPremisesId(premises.id) } returns listOf(archivedBedspaceOne, bedspaceTwo)

      val result = premisesService.archiveBedspace(bedspaceOne.id, premises, archiveDate)

      assertThatCasResult(result).isSuccess().with { bed ->
        assertThat(bed.name).isEqualTo(bedspaceOne.name)
        assertThat(bed.startDate).isEqualTo(bedspaceOne.startDate)
        assertThat(bed.endDate).isEqualTo(archiveDate)
        assertThat(bed.room).isEqualTo(bedspaceOne.room)
        assertThat(bed.room.premises).isEqualTo(bedspaceOne.room.premises)
      }
    }

    @Test
    fun `When archive the last online bedspace returns Success with correct result and archive the premises`() {
      val (premises, bedspaceOne) = createPremisesAndBedspace()
      val latestBedspaceArchiveDate = LocalDate.now().plusDays(100)
      val bedspaceTwo = createBedspace(premises, LocalDate.now().minusDays(5))
      val bedspaceThree = createBedspace(premises, latestBedspaceArchiveDate)

      val archiveDate = LocalDate.now().plusDays(3)
      val archivedBedspaceOne = bedspaceOne.copy(
        endDate = archiveDate,
      )

      every { bedRepositoryMock.findByIdOrNull(bedspaceOne.id) } returns bedspaceOne
      every { bookingRepositoryMock.findActiveOverlappingBookingByBed(bedspaceOne.id, archiveDate) } returns emptyList()
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDate(bedspaceOne.id, archiveDate) } returns emptyList()
      every { bedRepositoryMock.save(match { it.id == bedspaceOne.id }) } returns bedspaceOne
      every { cas3DomainEventServiceMock.saveBedspaceArchiveEvent(bedspaceOne) } returns Unit
      every { bedRepositoryMock.findByRoomPremisesId(premises.id) } returns listOf(archivedBedspaceOne, bedspaceTwo, bedspaceThree)
      every { premisesRepositoryMock.save(match { it.id == premises.id && it.status == PropertyStatus.archived }) } returns premises
      every { cas3DomainEventServiceMock.savePremisesArchiveEvent(match { it.id == premises.id }, latestBedspaceArchiveDate) } returns Unit

      val result = premisesService.archiveBedspace(bedspaceOne.id, premises, archiveDate)

      assertThatCasResult(result).isSuccess().with { bed ->
        assertThat(bed.name).isEqualTo(bedspaceOne.name)
        assertThat(bed.startDate).isEqualTo(bedspaceOne.startDate)
        assertThat(bed.endDate).isEqualTo(archiveDate)
        assertThat(bed.room).isEqualTo(bedspaceOne.room)
        assertThat(bed.room.premises).isEqualTo(bedspaceOne.room.premises)
      }

      verify(exactly = 1) {
        premisesRepositoryMock.save(
          match<TemporaryAccommodationPremisesEntity> {
            it.id == premises.id &&
              it.status == PropertyStatus.archived &&
              it.endDate == latestBedspaceArchiveDate
          },
        )

        cas3DomainEventServiceMock.savePremisesArchiveEvent(
          match {
            it.id == premises.id
          },
          latestBedspaceArchiveDate,
        )
      }
    }

    @Test
    fun `When archive a non existing bedspace returns a NotFound with the correct message`() {
      val (premises, _) = createPremisesAndBedspace()
      val nonExistingBedspaceId = UUID.randomUUID()

      every { bedRepositoryMock.findByIdOrNull(nonExistingBedspaceId) } returns null

      val result = premisesService.archiveBedspace(nonExistingBedspaceId, premises, LocalDate.now())

      assertThatCasResult(result).isNotFound("Bedspace", nonExistingBedspaceId)
    }

    @Test
    fun `When archive a bedspace with an end date in the past returns FieldValidationError with the correct message`() {
      val (premises, bedspace) = createPremisesAndBedspace()

      every { bedRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace

      val result = premisesService.archiveBedspace(bedspace.id, premises, LocalDate.now().minusDays(8))

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.endDate", "invalidEndDateInThePast")
    }

    @Test
    fun `When archive a bedspace with an end date in the future more than three months returns FieldValidationError with the correct message`() {
      val (premises, bedspace) = createPremisesAndBedspace()

      every { bedRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace

      val result = premisesService.archiveBedspace(bedspace.id, premises, LocalDate.now().plusMonths(3).plusDays(1))

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.endDate", "invalidEndDateInTheFuture")
    }

    @Test
    fun `When archive a bedspace with a void that has an end date after bedspace archive date returns Cas3FieldValidationError with the correct message`() {
      val (premises, bedspace) = createPremisesAndBedspace()
      val archiveDate = LocalDate.now().plusDays(7)
      val voidBedspace = Cas3VoidBedspaceEntityFactory()
        .withPremises(bedspace.room.premises)
        .withStartDate(archiveDate.minusDays(3))
        .withEndDate(archiveDate.plusDays(1))
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .withYieldedBed { bedspace }
        .produce()

      every { bedRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace
      every { bookingRepositoryMock.findActiveOverlappingBookingByBed(bedspace.id, archiveDate) } returns listOf()
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDate(bedspace.id, archiveDate) } returns listOf(voidBedspace)

      val result = premisesService.archiveBedspace(bedspace.id, premises, archiveDate)

      assertThatCasResult(result).isCas3FieldValidationError().hasMessage("$.endDate", bedspace.id.toString(), "existingVoid", voidBedspace.endDate.plusDays(1).toString())
    }

    @Test
    fun `When archive a bedspace with a booking departure date after bedspace archive date returns Cas3FieldValidationError with the correct message`() {
      val (premises, bedspace) = createPremisesAndBedspace()
      val archiveDate = LocalDate.now().plusDays(7)
      val booking = BookingEntityFactory()
        .withPremises(bedspace.room.premises)
        .withBed(bedspace)
        .withArrivalDate(archiveDate.minusDays(3))
        .withDepartureDate(archiveDate.plusDays(4))
        .produce()

      every { bedRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace
      every { bookingRepositoryMock.findActiveOverlappingBookingByBed(bedspace.id, archiveDate) } returns listOf(booking)
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDate(bedspace.id, archiveDate) } returns listOf()
      every { workingDayServiceMock.addWorkingDays(booking.departureDate, any()) } returns booking.departureDate

      val result = premisesService.archiveBedspace(bedspace.id, premises, archiveDate)

      assertThatCasResult(result).isCas3FieldValidationError().hasMessage("$.endDate", bedspace.id.toString(), "existingBookings", booking.departureDate.plusDays(1).toString())
    }

    @Test
    fun `When archive a bedspace with a booking turnaround date after bedspace archive date returns Cas3FieldValidationError with the correct message`() {
      val (premises, bedspace) = createPremisesAndBedspace()
      val archiveDate = LocalDate.now().plusDays(7)
      val booking = BookingEntityFactory()
        .withPremises(bedspace.room.premises)
        .withBed(bedspace)
        .withArrivalDate(archiveDate.minusDays(3))
        .withDepartureDate(archiveDate.plusDays(4))
        .produce()

      Cas3TurnaroundEntityFactory()
        .withBooking(booking)
        .withWorkingDayCount(2)
        .produce()

      every { bedRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace
      every { bookingRepositoryMock.findActiveOverlappingBookingByBed(bedspace.id, archiveDate) } returns listOf(booking)
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDate(bedspace.id, archiveDate) } returns listOf()
      every { workingDayServiceMock.addWorkingDays(booking.departureDate, any()) } returns booking.departureDate.plusDays(2)

      val result = premisesService.archiveBedspace(bedspace.id, premises, archiveDate)

      assertThatCasResult(result).isCas3FieldValidationError().hasMessage("$.endDate", bedspace.id.toString(), "existingTurnaround", booking.departureDate.plusDays(3).toString())
    }

    @Test
    fun `When archive a bedspace with a booking and void dates after bedspace archive date returns Cas3FieldValidationError with the correct message and last blocking date`() {
      val (premises, bedspace) = createPremisesAndBedspace()
      val archiveDate = LocalDate.now().plusDays(7)
      val booking = BookingEntityFactory()
        .withPremises(bedspace.room.premises)
        .withBed(bedspace)
        .withArrivalDate(archiveDate.minusDays(3))
        .withDepartureDate(archiveDate.plusDays(1))
        .produce()

      val voidBedspace = Cas3VoidBedspaceEntityFactory()
        .withPremises(bedspace.room.premises)
        .withStartDate(archiveDate.minusDays(4))
        .withEndDate(archiveDate.plusDays(2))
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .withYieldedBed { bedspace }
        .produce()

      every { bedRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace
      every { bookingRepositoryMock.findActiveOverlappingBookingByBed(bedspace.id, archiveDate) } returns listOf(booking)
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDate(bedspace.id, archiveDate) } returns listOf(voidBedspace)
      every { workingDayServiceMock.addWorkingDays(booking.departureDate, any()) } returns booking.departureDate

      val result = premisesService.archiveBedspace(bedspace.id, premises, archiveDate)

      assertThatCasResult(result).isCas3FieldValidationError().hasMessage("$.endDate", bedspace.id.toString(), "existingVoid", voidBedspace.endDate.plusDays(1).toString())
    }

    @Test
    fun `When archive a bedspace with void and booking that has the same end date both after the bedspace archive date returns Cas3FieldValidationError with the correct message and last blocking date`() {
      val (premises, bedspace) = createPremisesAndBedspace()
      val archiveDate = LocalDate.now().plusDays(7)
      val booking = BookingEntityFactory()
        .withPremises(bedspace.room.premises)
        .withBed(bedspace)
        .withArrivalDate(archiveDate.minusDays(3))
        .withDepartureDate(archiveDate.plusDays(4))
        .produce()

      Cas3TurnaroundEntityFactory()
        .withBooking(booking)
        .withWorkingDayCount(2)
        .produce()

      every { bedRepositoryMock.findByIdOrNull(bedspace.id) } returns bedspace
      every { bookingRepositoryMock.findActiveOverlappingBookingByBed(bedspace.id, archiveDate) } returns listOf(booking)
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDate(bedspace.id, archiveDate) } returns listOf()
      every { workingDayServiceMock.addWorkingDays(booking.departureDate, any()) } returns booking.departureDate.plusDays(2)

      val result = premisesService.archiveBedspace(bedspace.id, premises, archiveDate)

      assertThatCasResult(result).isCas3FieldValidationError().hasMessage("$.endDate", bedspace.id.toString(), "existingTurnaround", booking.departureDate.plusDays(3).toString())
    }
  }

  @Nested
  inner class GetAvailability {
    @Test
    fun `getAvailabilityForRange returns correctly for Temporary Accommodation premises`() {
      val startDate = LocalDate.now()
      val endDate = LocalDate.now().plusDays(6)

      val premises = temporaryAccommodationPremisesFactory.produce()

      val room = RoomEntityFactory()
        .withYieldedPremises { premises }
        .produce()

      val bed = BedEntityFactory()
        .withYieldedRoom { room }
        .produce()

      val voidBedspaceEntity = Cas3VoidBedspaceEntityFactory()
        .withPremises(premises)
        .withStartDate(startDate.plusDays(1))
        .withEndDate(startDate.plusDays(2))
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .withYieldedBed { bed }
        .produce()

      val pendingBookingEntity = BookingSummaryForAvailabilityFactory()
        .withArrivalDate(startDate.plusDays(1))
        .withDepartureDate(startDate.plusDays(3))
        .withCancelled(false)
        .withArrived(false)
        .withIsNotArrived(false)
        .produce()

      val arrivedBookingEntity = BookingSummaryForAvailabilityFactory()
        .withArrivalDate(startDate)
        .withDepartureDate(startDate.plusDays(2))
        .withCancelled(false)
        .withArrived(true)
        .withIsNotArrived(false)
        .produce()

      val nonArrivedBookingEntity = BookingSummaryForAvailabilityFactory()
        .withArrivalDate(startDate.plusDays(3))
        .withDepartureDate(startDate.plusDays(5))
        .withCancelled(false)
        .withArrived(false)
        .withIsNotArrived(true)
        .produce()

      val cancelledBookingEntity = BookingSummaryForAvailabilityFactory()
        .withArrivalDate(startDate.plusDays(4))
        .withDepartureDate(startDate.plusDays(6))
        .withCancelled(true)
        .withArrived(false)
        .withIsNotArrived(false)
        .produce()

      every {
        bookingRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate)
      } returns mutableListOf(
        pendingBookingEntity,
        arrivedBookingEntity,
        nonArrivedBookingEntity,
        cancelledBookingEntity,
      )
      every {
        cas3VoidBedspacesRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate)
      } returns mutableListOf(
        voidBedspaceEntity,
      )

      val result = premisesService.getAvailabilityForRange(premises, startDate, endDate)

      assertThat(result).containsValues(
        Availability(date = startDate, pendingBookings = 0, arrivedBookings = 1, nonArrivedBookings = 0, cancelledBookings = 0, voidBedspaces = 0),
        Availability(date = startDate.plusDays(1), pendingBookings = 1, arrivedBookings = 1, nonArrivedBookings = 0, cancelledBookings = 0, voidBedspaces = 1),
        Availability(date = startDate.plusDays(2), pendingBookings = 1, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, voidBedspaces = 0),
        Availability(date = startDate.plusDays(3), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 1, cancelledBookings = 0, voidBedspaces = 0),
        Availability(date = startDate.plusDays(4), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 1, cancelledBookings = 1, voidBedspaces = 0),
        Availability(date = startDate.plusDays(5), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 1, voidBedspaces = 0),
      )
    }

    @Test
    fun `getAvailabilityForRange returns correctly when there are no bookings or void bedspaces`() {
      val startDate = LocalDate.now()
      val endDate = LocalDate.now().plusDays(3)

      val premises = temporaryAccommodationPremisesFactory.produce()

      every {
        bookingRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate)
      } returns mutableListOf()
      every {
        cas3VoidBedspacesRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate)
      } returns mutableListOf()

      val result = premisesService.getAvailabilityForRange(premises, startDate, endDate)

      assertThat(result).containsValues(
        Availability(date = startDate, 0, 0, 0, 0, 0),
        Availability(date = startDate.plusDays(1), 0, 0, 0, 0, 0),
        Availability(date = startDate.plusDays(2), 0, 0, 0, 0, 0),
      )
    }

    @Test
    fun `getAvailabilityForRange returns correctly when there are bookings`() {
      val startDate = LocalDate.now()
      val endDate = LocalDate.now().plusDays(6)

      val premises = temporaryAccommodationPremisesFactory.produce()

      val voidBedspaceEntityOne = Cas3VoidBedspaceEntityFactory()
        .withPremises(premises)
        .withStartDate(startDate.plusDays(1))
        .withEndDate(startDate.plusDays(2))
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .withBed(
          BedEntityFactory().apply {
            withYieldedRoom {
              RoomEntityFactory().apply {
                withYieldedPremises { premises }
              }.produce()
            }
          }.produce(),
        )
        .produce()

      val voidBedspaceEntityTwo = Cas3VoidBedspaceEntityFactory()
        .withPremises(premises)
        .withStartDate(startDate.plusDays(1))
        .withEndDate(startDate.plusDays(2))
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .withBed(
          BedEntityFactory().apply {
            withYieldedRoom {
              RoomEntityFactory().apply {
                withYieldedPremises { premises }
              }.produce()
            }
          }.produce(),
        )
        .produce()

      val pendingBookingEntity = BookingSummaryForAvailabilityFactory()
        .withArrivalDate(startDate.plusDays(1))
        .withDepartureDate(startDate.plusDays(3))
        .withArrived(false)
        .withCancelled(false)
        .withIsNotArrived(false)
        .produce()

      val arrivedBookingEntity = BookingSummaryForAvailabilityFactory()
        .withArrivalDate(startDate)
        .withDepartureDate(startDate.plusDays(2))
        .withArrived(true)
        .withCancelled(false)
        .withIsNotArrived(false)
        .produce()

      val nonArrivedBookingEntity = BookingSummaryForAvailabilityFactory()
        .withArrivalDate(startDate.plusDays(3))
        .withDepartureDate(startDate.plusDays(5))
        .withArrived(false)
        .withCancelled(false)
        .withIsNotArrived(true)
        .produce()

      val cancelledBookingEntity = BookingSummaryForAvailabilityFactory()
        .withArrivalDate(startDate.plusDays(4))
        .withDepartureDate(startDate.plusDays(6))
        .withArrived(false)
        .withCancelled(true)
        .withIsNotArrived(false)
        .produce()

      every {
        bookingRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate)
      } returns mutableListOf(
        pendingBookingEntity,
        arrivedBookingEntity,
        nonArrivedBookingEntity,
        cancelledBookingEntity,
      )
      every {
        cas3VoidBedspacesRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate)
      } returns mutableListOf(
        voidBedspaceEntityOne,
        voidBedspaceEntityTwo,
      )

      val result = premisesService.getAvailabilityForRange(premises, startDate, endDate)

      assertThat(result).containsValues(
        Availability(date = startDate, pendingBookings = 0, arrivedBookings = 1, nonArrivedBookings = 0, cancelledBookings = 0, voidBedspaces = 0),
        Availability(date = startDate.plusDays(1), pendingBookings = 1, arrivedBookings = 1, nonArrivedBookings = 0, cancelledBookings = 0, voidBedspaces = 2),
        Availability(date = startDate.plusDays(2), pendingBookings = 1, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, voidBedspaces = 0),
        Availability(date = startDate.plusDays(3), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 1, cancelledBookings = 0, voidBedspaces = 0),
        Availability(date = startDate.plusDays(4), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 1, cancelledBookings = 1, voidBedspaces = 0),
        Availability(date = startDate.plusDays(5), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 1, voidBedspaces = 0),
      )
    }

    @Test
    fun `getAvailabilityForRange returns correctly when there are cancelled void bedspaces`() {
      val startDate = LocalDate.now()
      val endDate = LocalDate.now().plusDays(6)

      val premises = temporaryAccommodationPremisesFactory.produce()

      val voidBedspaceEntity = Cas3VoidBedspaceEntityFactory()
        .withPremises(premises)
        .withStartDate(startDate.plusDays(1))
        .withEndDate(startDate.plusDays(2))
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .withBed(
          BedEntityFactory().apply {
            withYieldedRoom {
              RoomEntityFactory().apply {
                withYieldedPremises { premises }
              }.produce()
            }
          }.produce(),
        )
        .produce()

      val voidBedspaceCancellation = Cas3VoidBedspaceCancellationEntityFactory()
        .withYieldedVoidBedspace { voidBedspaceEntity }
        .produce()

      voidBedspaceEntity.cancellation = voidBedspaceCancellation

      every {
        bookingRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate)
      } returns mutableListOf()
      every {
        cas3VoidBedspacesRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate)
      } returns mutableListOf(
        voidBedspaceEntity,
      )

      val result = premisesService.getAvailabilityForRange(premises, startDate, endDate)

      assertThat(result).containsValues(
        Availability(date = startDate, pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, voidBedspaces = 0),
        Availability(date = startDate.plusDays(1), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, voidBedspaces = 0),
        Availability(date = startDate.plusDays(2), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, voidBedspaces = 0),
        Availability(date = startDate.plusDays(3), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, voidBedspaces = 0),
        Availability(date = startDate.plusDays(4), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, voidBedspaces = 0),
        Availability(date = startDate.plusDays(5), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, voidBedspaces = 0),
      )
    }
  }

  @Nested
  inner class CreateVoidBedspace {
    @Test
    fun `createVoidBedspaces returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
      val premisesEntity = temporaryAccommodationPremisesFactory.produce()

      val reasonId = UUID.randomUUID()

      every { cas3VoidBedspaceReasonRepositoryMock.findByIdOrNull(reasonId) } returns null

      val result = premisesService.createVoidBedspaces(
        premises = premisesEntity,
        startDate = LocalDate.parse("2022-08-28"),
        endDate = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        referenceNumber = "12345",
        notes = "notes",
        bedId = UUID.randomUUID(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.endDate", "beforeStartDate"),
        entry("$.reason", "doesNotExist"),
      )
    }

    @Test
    fun `createVoidBedspaces returns Success with correct result when validation passed`() {
      val premisesEntity = temporaryAccommodationPremisesFactory.produce()

      val room = RoomEntityFactory()
        .withPremises(premisesEntity)
        .produce()

      val bed = BedEntityFactory()
        .withYieldedRoom { room }
        .produce()

      premisesEntity.rooms += room
      room.beds += bed

      val voidBedspaceReason = Cas3VoidBedspaceReasonEntityFactory()
        .produce()

      every { cas3VoidBedspaceReasonRepositoryMock.findByIdOrNull(voidBedspaceReason.id) } returns voidBedspaceReason

      every { cas3VoidBedspacesRepositoryMock.save(any()) } answers { it.invocation.args[0] as Cas3VoidBedspaceEntity }

      val result = premisesService.createVoidBedspaces(
        premises = premisesEntity,
        startDate = LocalDate.parse("2022-08-25"),
        endDate = LocalDate.parse("2022-08-28"),
        reasonId = voidBedspaceReason.id,
        referenceNumber = "12345",
        notes = "notes",
        bedId = bed.id,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.premises).isEqualTo(premisesEntity)
      assertThat(result.entity.reason).isEqualTo(voidBedspaceReason)
      assertThat(result.entity.startDate).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.endDate).isEqualTo(LocalDate.parse("2022-08-28"))
      assertThat(result.entity.referenceNumber).isEqualTo("12345")
      assertThat(result.entity.notes).isEqualTo("notes")
    }

    @Test
    fun `createVoidBedspaces returns error when void start date is after bed end date`() {
      val premisesEntity = temporaryAccommodationPremisesFactory.produce()

      val room = RoomEntityFactory()
        .withPremises(premisesEntity)
        .produce()

      val bed = BedEntityFactory()
        .withYieldedRoom { room }
        .withStartDate(LocalDate.parse("2022-08-10"))
        .withEndDate(LocalDate.parse("2022-08-24"))
        .produce()

      premisesEntity.rooms += room
      room.beds += bed

      val voidBedspaceReason = Cas3VoidBedspaceReasonEntityFactory()
        .produce()

      every { cas3VoidBedspaceReasonRepositoryMock.findByIdOrNull(voidBedspaceReason.id) } returns voidBedspaceReason

      every { cas3VoidBedspacesRepositoryMock.save(any()) } answers { it.invocation.args[0] as Cas3VoidBedspaceEntity }

      val result = premisesService.createVoidBedspaces(
        premises = premisesEntity,
        startDate = LocalDate.parse("2022-08-25"),
        endDate = LocalDate.parse("2022-08-28"),
        reasonId = voidBedspaceReason.id,
        referenceNumber = "12345",
        notes = "notes",
        bedId = bed.id,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.startDate", "voidStartDateAfterBedspaceEndDate"),
      )
    }

    @Test
    fun `createVoidBedspaces returns validation error when void start date is before bedspace start date`() {
      val premisesEntity = temporaryAccommodationPremisesFactory.produce()

      val room = RoomEntityFactory()
        .withPremises(premisesEntity)
        .produce()

      val bed = BedEntityFactory()
        .withYieldedRoom { room }
        .withStartDate(LocalDate.parse("2022-08-10"))
        .withEndDate(LocalDate.parse("2022-08-30"))
        .produce()

      premisesEntity.rooms += room
      room.beds += bed

      val voidBedspaceReason = Cas3VoidBedspaceReasonEntityFactory()
        .produce()

      every { cas3VoidBedspaceReasonRepositoryMock.findByIdOrNull(voidBedspaceReason.id) } returns voidBedspaceReason

      every { cas3VoidBedspacesRepositoryMock.save(any()) } answers { it.invocation.args[0] as Cas3VoidBedspaceEntity }

      val result = premisesService.createVoidBedspaces(
        premises = premisesEntity,
        startDate = LocalDate.parse("2022-08-05"),
        endDate = LocalDate.parse("2022-08-15"),
        reasonId = voidBedspaceReason.id,
        referenceNumber = "12345",
        notes = "notes",
        bedId = bed.id,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.startDate", "voidStartDateBeforeBedspaceStartDate"),
      )
    }

    @Test
    fun `createVoidBedspaces returns validation error when void end date is after bedspace end date`() {
      val premisesEntity = temporaryAccommodationPremisesFactory.produce()

      val room = RoomEntityFactory()
        .withPremises(premisesEntity)
        .produce()

      val bed = BedEntityFactory()
        .withYieldedRoom { room }
        .withStartDate(LocalDate.parse("2022-08-10"))
        .withEndDate(LocalDate.parse("2022-08-24"))
        .produce()

      premisesEntity.rooms += room
      room.beds += bed

      val voidBedspaceReason = Cas3VoidBedspaceReasonEntityFactory()
        .produce()

      every { cas3VoidBedspaceReasonRepositoryMock.findByIdOrNull(voidBedspaceReason.id) } returns voidBedspaceReason

      every { cas3VoidBedspacesRepositoryMock.save(any()) } answers { it.invocation.args[0] as Cas3VoidBedspaceEntity }

      val result = premisesService.createVoidBedspaces(
        premises = premisesEntity,
        startDate = LocalDate.parse("2022-08-15"),
        endDate = LocalDate.parse("2022-08-30"),
        reasonId = voidBedspaceReason.id,
        referenceNumber = "12345",
        notes = "notes",
        bedId = bed.id,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.endDate", "voidEndDateAfterBedspaceEndDate"),
      )
    }
  }

  @Nested
  inner class UpdateVoidBedspace {
    @Test
    fun `updateVoidBedspaces returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
      val premisesEntity = temporaryAccommodationPremisesFactory.produce()

      val reasonId = UUID.randomUUID()

      val voidBedspaceEntity = Cas3VoidBedspaceEntityFactory()
        .withYieldedPremises { premisesEntity }
        .withYieldedReason {
          Cas3VoidBedspaceReasonEntityFactory()
            .produce()
        }
        .withBed(
          BedEntityFactory().apply {
            withYieldedRoom {
              RoomEntityFactory().apply {
                withPremises(premisesEntity)
              }.produce()
            }
          }.produce(),
        )
        .produce()

      every { cas3VoidBedspacesRepositoryMock.findByIdOrNull(voidBedspaceEntity.id) } returns voidBedspaceEntity
      every { cas3VoidBedspaceReasonRepositoryMock.findByIdOrNull(reasonId) } returns null

      val result = premisesService.updateVoidBedspaces(
        voidBedspaceId = voidBedspaceEntity.id,
        startDate = LocalDate.parse("2022-08-28"),
        endDate = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        referenceNumber = "12345",
        notes = "notes",
      )

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      val resultEntity = (result as AuthorisableActionResult.Success).entity
      assertThat(resultEntity).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((resultEntity as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.endDate", "beforeStartDate"),
        entry("$.reason", "doesNotExist"),
      )
    }

    @Test
    fun `updateVoidBedspaces returns Success with correct result when validation passed`() {
      val premisesEntity = temporaryAccommodationPremisesFactory.produce()

      val voidBedspaceReason = Cas3VoidBedspaceReasonEntityFactory()
        .produce()

      val voidBedspacesEntity = Cas3VoidBedspaceEntityFactory()
        .withYieldedPremises { premisesEntity }
        .withYieldedReason {
          Cas3VoidBedspaceReasonEntityFactory()
            .produce()
        }
        .withBed(
          BedEntityFactory().apply {
            withYieldedRoom {
              RoomEntityFactory().apply {
                withPremises(premisesEntity)
              }.produce()
            }
          }.produce(),
        )
        .produce()

      every { cas3VoidBedspacesRepositoryMock.findByIdOrNull(voidBedspacesEntity.id) } returns voidBedspacesEntity
      every { cas3VoidBedspaceReasonRepositoryMock.findByIdOrNull(voidBedspaceReason.id) } returns voidBedspaceReason

      every { cas3VoidBedspacesRepositoryMock.save(any()) } answers { it.invocation.args[0] as Cas3VoidBedspaceEntity }

      val result = premisesService.updateVoidBedspaces(
        voidBedspaceId = voidBedspacesEntity.id,
        startDate = LocalDate.parse("2022-08-25"),
        endDate = LocalDate.parse("2022-08-28"),
        reasonId = voidBedspaceReason.id,
        referenceNumber = "12345",
        notes = "notes",
      )
      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      val resultEntity = (result as AuthorisableActionResult.Success).entity
      assertThat(resultEntity).isInstanceOf(ValidatableActionResult.Success::class.java)
      resultEntity as ValidatableActionResult.Success
      assertThat(resultEntity.entity.premises).isEqualTo(premisesEntity)
      assertThat(resultEntity.entity.reason).isEqualTo(voidBedspaceReason)
      assertThat(resultEntity.entity.startDate).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(resultEntity.entity.endDate).isEqualTo(LocalDate.parse("2022-08-28"))
      assertThat(resultEntity.entity.referenceNumber).isEqualTo("12345")
      assertThat(resultEntity.entity.notes).isEqualTo("notes")
    }

    @Test
    fun `updateVoidBedspaces returns validation error when void start date is before bedspace start date`() {
      val premisesEntity = temporaryAccommodationPremisesFactory.produce()

      val voidBedspaceReason = Cas3VoidBedspaceReasonEntityFactory()
        .produce()

      val bed = BedEntityFactory().apply {
        withYieldedRoom {
          RoomEntityFactory().apply {
            withPremises(premisesEntity)
          }.produce()
        }
        withStartDate(LocalDate.parse("2022-08-10"))
        withEndDate(LocalDate.parse("2022-08-30"))
      }.produce()

      val voidBedspaceEntity = Cas3VoidBedspaceEntityFactory()
        .withYieldedPremises { premisesEntity }
        .withYieldedReason { voidBedspaceReason }
        .withBed(bed)
        .produce()

      every { cas3VoidBedspacesRepositoryMock.findByIdOrNull(voidBedspaceEntity.id) } returns voidBedspaceEntity
      every { cas3VoidBedspaceReasonRepositoryMock.findByIdOrNull(voidBedspaceReason.id) } returns voidBedspaceReason

      val result = premisesService.updateVoidBedspaces(
        voidBedspaceId = voidBedspaceEntity.id,
        startDate = LocalDate.parse("2022-08-05"),
        endDate = LocalDate.parse("2022-08-15"),
        reasonId = voidBedspaceReason.id,
        referenceNumber = "12345",
        notes = "notes",
      )

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      val resultEntity = (result as AuthorisableActionResult.Success).entity
      assertThat(resultEntity).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((resultEntity as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.startDate", "voidStartDateBeforeBedspaceStartDate"),
      )
    }

    @Test
    fun `updateVoidBedspaces returns validation error when void start date is after bedspace end date`() {
      val premisesEntity = temporaryAccommodationPremisesFactory.produce()

      val voidBedspaceReason = Cas3VoidBedspaceReasonEntityFactory()
        .produce()

      val bed = BedEntityFactory().apply {
        withYieldedRoom {
          RoomEntityFactory().apply {
            withPremises(premisesEntity)
          }.produce()
        }
        withStartDate(LocalDate.parse("2022-08-10"))
        withEndDate(LocalDate.parse("2022-08-24"))
      }.produce()

      val voidBedspaceEntity = Cas3VoidBedspaceEntityFactory()
        .withYieldedPremises { premisesEntity }
        .withYieldedReason { voidBedspaceReason }
        .withBed(bed)
        .produce()

      every { cas3VoidBedspacesRepositoryMock.findByIdOrNull(voidBedspaceEntity.id) } returns voidBedspaceEntity
      every { cas3VoidBedspaceReasonRepositoryMock.findByIdOrNull(voidBedspaceReason.id) } returns voidBedspaceReason

      val result = premisesService.updateVoidBedspaces(
        voidBedspaceId = voidBedspaceEntity.id,
        startDate = LocalDate.parse("2022-08-25"),
        endDate = LocalDate.parse("2022-08-28"),
        reasonId = voidBedspaceReason.id,
        referenceNumber = "12345",
        notes = "notes",
      )

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      val resultEntity = (result as AuthorisableActionResult.Success).entity
      assertThat(resultEntity).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((resultEntity as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.startDate", "voidStartDateAfterBedspaceEndDate"),
      )
    }

    @Test
    fun `updateVoidBedspaces returns validation error when void end date is after bedspace end date`() {
      val premisesEntity = temporaryAccommodationPremisesFactory.produce()

      val voidBedspaceReason = Cas3VoidBedspaceReasonEntityFactory()
        .produce()

      val bed = BedEntityFactory().apply {
        withYieldedRoom {
          RoomEntityFactory().apply {
            withPremises(premisesEntity)
          }.produce()
        }
        withStartDate(LocalDate.parse("2022-08-10"))
        withEndDate(LocalDate.parse("2022-08-24"))
      }.produce()

      val voidBedspaceEntity = Cas3VoidBedspaceEntityFactory()
        .withYieldedPremises { premisesEntity }
        .withYieldedReason { voidBedspaceReason }
        .withBed(bed)
        .produce()

      every { cas3VoidBedspacesRepositoryMock.findByIdOrNull(voidBedspaceEntity.id) } returns voidBedspaceEntity
      every { cas3VoidBedspaceReasonRepositoryMock.findByIdOrNull(voidBedspaceReason.id) } returns voidBedspaceReason

      val result = premisesService.updateVoidBedspaces(
        voidBedspaceId = voidBedspaceEntity.id,
        startDate = LocalDate.parse("2022-08-15"),
        endDate = LocalDate.parse("2022-08-30"),
        reasonId = voidBedspaceReason.id,
        referenceNumber = "12345",
        notes = "notes",
      )

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      val resultEntity = (result as AuthorisableActionResult.Success).entity
      assertThat(resultEntity).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((resultEntity as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.endDate", "voidEndDateAfterBedspaceEndDate"),
      )
    }
  }

  @Nested
  inner class RenamePremises {
    @Test
    fun `renamePremises returns NotFound if the premises does not exist`() {
      every { premisesRepositoryMock.findTemporaryAccommodationPremisesByIdOrNull(any()) } returns null

      val result = premisesService.renamePremises(UUID.randomUUID(), "unknown-premises")

      assertThat(result).isInstanceOf(AuthorisableActionResult.NotFound::class.java)
    }

    @Test
    fun `renamePremises returns FieldValidationError if the new name is not unique for the service`() {
      val premises = temporaryAccommodationPremisesFactory.produce()

      every { premisesRepositoryMock.findTemporaryAccommodationPremisesByIdOrNull(any()) } returns premises
      every { premisesRepositoryMock.nameIsUniqueForType<TemporaryAccommodationPremisesEntity>(any(), any()) } returns false

      val result = premisesService.renamePremises(premises.id, "non-unique-name-premises")

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      result as AuthorisableActionResult.Success
      assertThat(result.entity).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      val resultEntity = result.entity as ValidatableActionResult.FieldValidationError
      assertThat(resultEntity.validationMessages).contains(
        entry("$.name", "notUnique"),
      )
    }

    @Test
    fun `renamePremises returns Success containing updated premises otherwise`() {
      val premises = temporaryAccommodationPremisesFactory.produce()

      every { premisesRepositoryMock.findTemporaryAccommodationPremisesByIdOrNull(any()) } returns premises
      every { premisesRepositoryMock.nameIsUniqueForType<TemporaryAccommodationPremisesEntity>(any(), any()) } returns true
      every { premisesRepositoryMock.save(any()) } returnsArgument 0

      val result = premisesService.renamePremises(premises.id, "renamed-premises")

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      result as AuthorisableActionResult.Success
      assertThat(result.entity).isInstanceOf(ValidatableActionResult.Success::class.java)
      val resultEntity = result.entity as ValidatableActionResult.Success
      assertThat(resultEntity.entity).matches {
        it.id == premises.id &&
          it.name == "renamed-premises"
      }

      verify(exactly = 1) {
        premisesRepositoryMock.save(
          match {
            it.id == premises.id &&
              it.name == "renamed-premises"
          },
        )
      }
    }
  }

  @Nested
  inner class ArchivePremises {
    @Test
    fun `When archive a premises returns Success with correct result when validation passed`() {
      val (premises, bedspaceOne) = createPremisesAndBedspace()
      val bedspaceTwo = createBedspace(premises, LocalDate.now().minusDays(90))
      val allBedspaces = listOf(bedspaceOne, bedspaceTwo)

      val archiveDate = LocalDate.now().plusDays(3)

      every { bookingRepositoryMock.findActiveOverlappingBookingByPremisesId(premises.id, archiveDate) } returns emptyList()
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDateByPremisesId(premises.id, archiveDate) } returns emptyList()
      every { bedRepositoryMock.save(any()) } returns bedspaceOne
      every { premisesRepositoryMock.save(any()) } returns premises
      every { cas3DomainEventServiceMock.savePremisesArchiveEvent(premises, archiveDate) } returns Unit
      every { cas3DomainEventServiceMock.saveBedspaceArchiveEvent(any()) } returns Unit

      val result = premisesService.archivePremises(premises, archiveDate)

      assertThatCasResult(result).isSuccess().with { updatedPremises ->
        assertThat(updatedPremises.id).isEqualTo(premises.id)
        assertThat(updatedPremises.startDate).isEqualTo(premises.startDate)
        assertThat(updatedPremises.endDate).isEqualTo(archiveDate)
        assertThat(updatedPremises.status).isEqualTo(PropertyStatus.archived)

        updatedPremises.rooms.forEach { room ->
          room.beds.forEach { bed ->
            val archivedBedspace = allBedspaces.firstOrNull { it.id == bed.id }
            assertThat(archivedBedspace).isNotNull
            assertThat(bed.id).isEqualTo(archivedBedspace?.id)
            assertThat(bed.startDate).isEqualTo(archivedBedspace?.startDate)
            assertThat(bed.endDate).isEqualTo(archiveDate)
          }
        }
      }

      verify(exactly = 1) {
        cas3DomainEventServiceMock.savePremisesArchiveEvent(
          match<TemporaryAccommodationPremisesEntity> {
            it.id == premises.id
          },
          archiveDate,
        )
      }

      verify(exactly = 2) {
        cas3DomainEventServiceMock.saveBedspaceArchiveEvent(
          any(),
        )
      }
    }

    @Test
    fun `When archive a premises with archived bedspaces returns Success with correct result`() {
      val (premises, bedspaceOne) = createPremisesAndBedspace()
      // archived bedspace
      val bedspaceTwo = createBedspace(premises, LocalDate.now().minusDays(40), LocalDate.now().minusDays(5))
      val bedspaceThree = createBedspace(premises, LocalDate.now().minusDays(90))

      val archiveDate = LocalDate.now().plusDays(3)

      every { bookingRepositoryMock.findActiveOverlappingBookingByPremisesId(premises.id, archiveDate) } returns emptyList()
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDateByPremisesId(premises.id, archiveDate) } returns emptyList()
      every { bedRepositoryMock.save(any()) } returns bedspaceOne
      every { premisesRepositoryMock.save(any()) } returns premises
      every { cas3DomainEventServiceMock.saveBedspaceArchiveEvent(any()) } returns Unit
      every { cas3DomainEventServiceMock.savePremisesArchiveEvent(premises, archiveDate) } returns Unit
      every { cas3DomainEventServiceMock.saveBedspaceArchiveEvent(any()) } returns Unit

      val result = premisesService.archivePremises(premises, archiveDate)

      val expectedBedspaceOne = bedspaceOne.copy(
        endDate = archiveDate,
      )
      val expectedBedspaceThree = bedspaceThree.copy(
        endDate = archiveDate,
      )

      val allBedspaces = listOf(expectedBedspaceOne, bedspaceTwo, expectedBedspaceThree)

      assertThatCasResult(result).isSuccess().with { updatedPremises ->
        assertThat(updatedPremises.id).isEqualTo(premises.id)
        assertThat(updatedPremises.startDate).isEqualTo(premises.startDate)
        assertThat(updatedPremises.endDate).isEqualTo(archiveDate)
        assertThat(updatedPremises.status).isEqualTo(PropertyStatus.archived)

        updatedPremises.rooms.forEach { room ->
          room.beds.forEach { bed ->
            val archivedBedspace = allBedspaces.firstOrNull { it.id == bed.id }
            assertThat(archivedBedspace).isNotNull
            assertThat(bed.id).isEqualTo(archivedBedspace?.id)
            assertThat(bed.startDate).isEqualTo(archivedBedspace?.startDate)
            assertThat(bed.endDate).isEqualTo(archivedBedspace?.endDate)
          }
        }
      }
    }

    @Test
    fun `When archive a premises with an end date before seven days returns FieldValidationError with the correct message`() {
      val (premises, _) = createPremisesAndBedspace()

      val result = premisesService.archivePremises(premises, LocalDate.now().minusDays(8))

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.endDate", "invalidEndDateInThePast")
    }

    @Test
    fun `When archive a premises with an end date in the future more than three months returns FieldValidationError with the correct message`() {
      val (premises, _) = createPremisesAndBedspace()

      val result = premisesService.archivePremises(premises, LocalDate.now().plusMonths(3).plusDays(1))

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.endDate", "invalidEndDateInTheFuture")
    }

    @Test
    fun `When archive a premises with an upcoming bedspace returns Cas3FieldValidationError with the correct message`() {
      val (premises, _) = createPremisesAndBedspace()
      val archiveDate = LocalDate.now().plusDays(5)
      val bedspaceTwo = createBedspace(premises, archiveDate.plusDays(2))

      val result = premisesService.archivePremises(premises, archiveDate)

      assertThatCasResult(result).isCas3FieldValidationError().hasMessage("$.endDate", bedspaceTwo.id.toString(), "existingUpcomingBedspace", bedspaceTwo.startDate!!.plusDays(1).toString())
    }

    @Test
    fun `When archive a premises with a bedspace that has a void end date after premises archive date returns Cas3FieldValidationError with the correct message`() {
      val (premises, bedspace) = createPremisesAndBedspace()
      val archiveDate = LocalDate.now().plusDays(3)
      val voidBedspace = Cas3VoidBedspaceEntityFactory()
        .withPremises(premises)
        .withStartDate(archiveDate.minusDays(3))
        .withEndDate(archiveDate.plusDays(1))
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .withYieldedBed { bedspace }
        .produce()

      every { bookingRepositoryMock.findActiveOverlappingBookingByPremisesId(premises.id, archiveDate) } returns listOf()
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDateByPremisesId(premises.id, archiveDate) } returns listOf(voidBedspace)

      val result = premisesService.archivePremises(premises, archiveDate)

      assertThatCasResult(result).isCas3FieldValidationError().hasMessage("$.endDate", bedspace.id.toString(), "existingVoid", voidBedspace.endDate.plusDays(1).toString())
    }

    @Test
    fun `When archive a premises with multiple bedspaces that have void and booking end date after premises archive date returns Cas3FieldValidationError with the correct message`() {
      val (premises, bedspaceOne) = createPremisesAndBedspace()
      val archiveDate = LocalDate.now().plusDays(5)
      val bedspaceTwo = createBedspace(premises, LocalDate.now().minusDays(180))
      val voidBedspace = Cas3VoidBedspaceEntityFactory()
        .withPremises(premises)
        .withStartDate(archiveDate.minusDays(3))
        .withEndDate(archiveDate.plusDays(5))
        .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
        .withYieldedBed { bedspaceOne }
        .produce()

      val booking = BookingEntityFactory()
        .withPremises(premises)
        .withBed(bedspaceTwo)
        .withArrivalDate(archiveDate.minusDays(3))
        .withDepartureDate(archiveDate.plusDays(1))
        .produce()

      every { bookingRepositoryMock.findActiveOverlappingBookingByPremisesId(premises.id, archiveDate) } returns listOf(booking)
      every { cas3VoidBedspacesRepositoryMock.findOverlappingBedspaceEndDateByPremisesId(premises.id, archiveDate) } returns listOf(voidBedspace)
      every { workingDayServiceMock.addWorkingDays(booking.departureDate, any()) } returns booking.departureDate

      val result = premisesService.archivePremises(premises, archiveDate)

      assertThatCasResult(result).isCas3FieldValidationError().hasMessage("$.endDate", bedspaceOne.id.toString(), "existingVoid", voidBedspace.endDate.plusDays(1).toString())
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "provisional",
        "confirmed",
        "arrived",
      ],
    )
    fun `Archive premises returns FieldValidationError if there a booking in provisional, confirmed or arrived status`(bookingStatus: BookingStatus) {
      val probationRegion = ProbationRegionEntityFactory()
        .withApArea(ApAreaEntityFactory().produce())
        .produce()

      val localAuthority = LocalAuthorityEntityFactory()
        .produce()

      val probationDeliveryUnit = ProbationDeliveryUnitEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val premises = temporaryAccommodationPremisesFactory
        .withService(ServiceName.temporaryAccommodation.value)
        .withProbationRegion(probationRegion)
        .withProbationDeliveryUnit(probationDeliveryUnit)
        .withLocalAuthorityArea(localAuthority)
        .produce()

      val booking = bookingEntityFactory
        .withPremises(premises)
        .withStatus(bookingStatus)
        .withArrivalDate(LocalDate.now().plusDays(3))
        .produce()

      every { premisesRepositoryMock.findTemporaryAccommodationPremisesByIdOrNull(premises.id) } returns premises
      every { localAuthorityAreaRepositoryMock.findByIdOrNull(premises.localAuthorityArea!!.id) } returns localAuthority
      every { probationRegionRepositoryMock.findByIdOrNull(premises.probationRegion.id) } returns probationRegion
      every {
        probationDeliveryUnitRepositoryMock.findByIdAndProbationRegionId(probationDeliveryUnit.id, probationRegion.id)
      } returns probationDeliveryUnit
      every {
        bookingRepositoryMock.findFutureBookingsByPremisesIdAndStatus(ServiceName.temporaryAccommodation.value, premises.id, any(), any())
      } returns listOf(booking)

      val result = premisesService.updatePremises(
        premisesId = premises.id,
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        postcode = premises.postcode,
        town = premises.town,
        probationRegionId = premises.probationRegion.id,
        localAuthorityAreaId = premises.localAuthorityArea?.id,
        probationDeliveryUnitIdentifier = Ior.fromNullables(
          premises.probationDeliveryUnit?.name,
          premises.probationDeliveryUnit?.id,
        )?.toEither(),
        characteristicIds = premises.characteristics.map { it.id },
        status = PropertyStatus.archived,
        turnaroundWorkingDays = premises.turnaroundWorkingDays,
        notes = premises.notes,
      )

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      result as AuthorisableActionResult.Success
      assertThat(result.entity).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      val resultEntity = result.entity as ValidatableActionResult.FieldValidationError
      assertThat(resultEntity.validationMessages).contains(
        entry("$.status", "existingBookings"),
      )
    }

    private fun createPremisesAndBedspace(): Pair<TemporaryAccommodationPremisesEntity, BedEntity> {
      val premises = temporaryAccommodationPremisesFactory.produce()
      val bedspace = createBedspace(premises, LocalDate.now().minusDays(180))

      return Pair(premises, bedspace)
    }

    private fun createBedspace(premises: TemporaryAccommodationPremisesEntity, startDate: LocalDate, endDate: LocalDate? = null): BedEntity {
      val room = RoomEntityFactory()
        .withName(randomStringMultiCaseWithNumbers(10))
        .withPremises(premises)
        .produce()

      val bedspace = BedEntityFactory()
        .withRoom(room)
        .withStartDate(startDate)
        .withEndDate(endDate)
        .produce()

      premises.rooms.add(room)
      room.beds.add(bedspace)

      return bedspace
    }
  }

  @Nested
  inner class UnarchivePremises {
    @Test
    fun `unarchivePremises returns Success when premises is successfully unarchived`() {
      val probationRegion = ProbationRegionEntityFactory()
        .withApArea(ApAreaEntityFactory().produce())
        .produce()

      val localAuthority = LocalAuthorityEntityFactory()
        .produce()

      val probationDeliveryUnit = ProbationDeliveryUnitEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val archivedPremises = temporaryAccommodationPremisesFactory
        .withProbationRegion(probationRegion)
        .withProbationDeliveryUnit(probationDeliveryUnit)
        .withLocalAuthorityArea(localAuthority)
        .withStartDate(LocalDate.now().minusDays(30))
        .withEndDate(LocalDate.now().minusDays(1))
        .withStatus(PropertyStatus.archived)
        .produce()

      val room = RoomEntityFactory()
        .withPremises(archivedPremises)
        .produce()

      val archivedBedspace = BedEntityFactory()
        .withRoom(room)
        .withStartDate(LocalDate.now().minusDays(30))
        .withEndDate(LocalDate.now().minusDays(1))
        .produce()

      archivedPremises.rooms.add(room)
      room.beds.add(archivedBedspace)

      val restartDate = LocalDate.now().plusDays(1)

      val updatedPremises = temporaryAccommodationPremisesFactory
        .withService(ServiceName.temporaryAccommodation.value)
        .withProbationRegion(probationRegion)
        .withProbationDeliveryUnit(probationDeliveryUnit)
        .withLocalAuthorityArea(localAuthority)
        .withStartDate(restartDate)
        .withEndDate(null)
        .withStatus(PropertyStatus.active)
        .produce()

      val updatedBedspace = BedEntityFactory()
        .withRoom(room)
        .withStartDate(restartDate)
        .withEndDate(null)
        .produce()

      every { premisesRepositoryMock.findByIdOrNull(archivedPremises.id) } returns archivedPremises
      every { premisesRepositoryMock.save(any()) } returns updatedPremises
      every { bedRepositoryMock.findCas3Bedspace(archivedPremises.id, archivedBedspace.id) } returns archivedBedspace
      every { bedRepositoryMock.save(any()) } returns updatedBedspace
      every { cas3DomainEventServiceMock.saveBedspaceUnarchiveEvent(any(), any(), any()) } returns Unit
      every { cas3DomainEventServiceMock.savePremisesUnarchiveEvent(any(), any(), any()) } returns Unit
      every { bedRepositoryMock.findByRoomPremisesId(archivedPremises.id) } returns listOf(archivedBedspace)

      val result = premisesService.unarchivePremises(archivedPremises, restartDate)

      assertThatCasResult(result).isSuccess().with { resultEntity ->
        assertThat(resultEntity.startDate).isEqualTo(restartDate)
        assertThat(resultEntity.endDate).isNull()
        assertThat(resultEntity.status).isEqualTo(PropertyStatus.active)

        resultEntity.rooms.forEach { roomEntity ->
          roomEntity.beds.forEach { bedEntity ->
            assertThat(bedEntity.startDate).isEqualTo(restartDate)
            assertThat(bedEntity.endDate).isNull()
          }
        }
      }

      verify(exactly = 1) {
        premisesRepositoryMock.save(
          match<TemporaryAccommodationPremisesEntity> {
            it.id == archivedPremises.id
          },
        )
      }

      verify(exactly = 1) {
        bedRepositoryMock.save(
          match<BedEntity> {
            it.id == archivedBedspace.id
          },
        )
      }

      verify(exactly = 1) {
        cas3DomainEventServiceMock.savePremisesUnarchiveEvent(
          match<TemporaryAccommodationPremisesEntity> {
            it.id == archivedPremises.id
          },
          any(),
          any(),
        )
      }

      verify(exactly = 1) {
        cas3DomainEventServiceMock.saveBedspaceUnarchiveEvent(
          any(),
          any(),
          any(),
        )
      }
    }

    @Test
    fun `unarchivePremises returns FieldValidationError when premises is not archived`() {
      val activePremises = temporaryAccommodationPremisesFactory
        .withStartDate(LocalDate.now().minusDays(30))
        .withEndDate(null)
        .withStatus(PropertyStatus.active)
        .produce()

      val restartDate = LocalDate.now().plusDays(1)

      every { premisesRepositoryMock.findByIdOrNull(activePremises.id) } returns activePremises

      val result = premisesService.unarchivePremises(activePremises, restartDate)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.premisesId", "premisesNotArchived")
    }

    @Test
    fun `unarchivePremises returns FieldValidationError when restart date is more than 7 days in the past`() {
      val archivedPremises = temporaryAccommodationPremisesFactory
        .withStartDate(LocalDate.now().minusDays(30))
        .withEndDate(LocalDate.now().minusDays(10))
        .withStatus(PropertyStatus.archived)
        .produce()

      val restartDate = LocalDate.now().minusDays(8)

      every { premisesRepositoryMock.findByIdOrNull(archivedPremises.id) } returns archivedPremises

      val result = premisesService.unarchivePremises(archivedPremises, restartDate)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.restartDate", "invalidRestartDateInThePast")
    }

    @Test
    fun `unarchivePremises returns FieldValidationError when restart date is more than 7 days in the future`() {
      val archivedPremises = temporaryAccommodationPremisesFactory
        .withStartDate(LocalDate.now().minusDays(30))
        .withEndDate(LocalDate.now().minusDays(1))
        .withStatus(PropertyStatus.archived)
        .produce()

      val restartDate = LocalDate.now().plusDays(8)

      every { premisesRepositoryMock.findByIdOrNull(archivedPremises.id) } returns archivedPremises

      val result = premisesService.unarchivePremises(archivedPremises, restartDate)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.restartDate", "invalidRestartDateInTheFuture")
    }

    @ParameterizedTest
    @CsvSource("0", "1")
    fun `unarchivePremises returns FieldValidationError when restart date is equal or before last archive end date`(minusDays: Long) {
      val lastArchiveEndDate = LocalDate.now().minusDays(1)
      val archivedPremises = temporaryAccommodationPremisesFactory
        .withStartDate(LocalDate.now().minusDays(30))
        .withEndDate(lastArchiveEndDate)
        .withStatus(PropertyStatus.archived)
        .produce()

      val restartDate = lastArchiveEndDate.minusDays(minusDays)

      every { premisesRepositoryMock.findByIdOrNull(archivedPremises.id) } returns archivedPremises

      val result = premisesService.unarchivePremises(archivedPremises, restartDate)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.restartDate", "beforeLastPremisesArchivedDate")
    }

    @Test
    fun `unarchivePremises allows restart date exactly 7 days in the past`() {
      val archivedPremises = temporaryAccommodationPremisesFactory
        .withStartDate(LocalDate.now().minusDays(30))
        .withEndDate(LocalDate.now().minusDays(8))
        .withStatus(PropertyStatus.archived)
        .produce()

      val restartDate = LocalDate.now().minusDays(7)

      val updatedPremises = temporaryAccommodationPremisesFactory
        .withStartDate(restartDate)
        .withEndDate(null)
        .withStatus(PropertyStatus.active)
        .produce()

      every { premisesRepositoryMock.findByIdOrNull(archivedPremises.id) } returns archivedPremises
      every { premisesRepositoryMock.save(any()) } returns updatedPremises
      every { cas3DomainEventServiceMock.savePremisesUnarchiveEvent(any(), any(), any()) } returns Unit
      every { bedRepositoryMock.findByRoomPremisesId(archivedPremises.id) } returns listOf()

      val result = premisesService.unarchivePremises(archivedPremises, restartDate)

      assertThatCasResult(result).isSuccess()

      verify(exactly = 1) {
        cas3DomainEventServiceMock.savePremisesUnarchiveEvent(
          match<TemporaryAccommodationPremisesEntity> {
            it.id == archivedPremises.id
          },
          any(),
          any(),
        )
      }

      verify(exactly = 1) {
        premisesRepositoryMock.save(any())
      }
    }

    @Test
    fun `unarchivePremises allows restart date exactly 7 days in the future`() {
      val archivedPremises = temporaryAccommodationPremisesFactory
        .withStartDate(LocalDate.now().minusDays(30))
        .withEndDate(LocalDate.now().minusDays(1))
        .withStatus(PropertyStatus.archived)
        .produce()

      val restartDate = LocalDate.now().plusDays(7)

      val updatedPremises = temporaryAccommodationPremisesFactory
        .withStartDate(restartDate)
        .withEndDate(null)
        .withStatus(PropertyStatus.active)
        .produce()

      every { premisesRepositoryMock.findByIdOrNull(archivedPremises.id) } returns archivedPremises
      every { premisesRepositoryMock.save(any()) } returns updatedPremises
      every { cas3DomainEventServiceMock.savePremisesUnarchiveEvent(any(), any(), any()) } returns Unit
      every { bedRepositoryMock.findByRoomPremisesId(archivedPremises.id) } returns listOf()

      val result = premisesService.unarchivePremises(archivedPremises, restartDate)

      assertThatCasResult(result).isSuccess()

      verify(exactly = 1) {
        cas3DomainEventServiceMock.savePremisesUnarchiveEvent(
          match<TemporaryAccommodationPremisesEntity> {
            it.id == archivedPremises.id
          },
          any(),
          any(),
        )
      }

      verify(exactly = 1) {
        premisesRepositoryMock.save(any())
      }
    }
  }

  @Nested
  inner class UnarchiveBedspace {
    @Test
    fun `unarchiveBedspace returns Success when bedspace is successfully unarchived`() {
      val premises = temporaryAccommodationPremisesFactory.produce()
      val currentEndDate = LocalDate.now().minusDays(1)
      val archivedBedspace = createBedspace(premises, currentEndDate)
      val currentStartDate = archivedBedspace.startDate!!

      val restartDate = LocalDate.now().plusDays(1)
      val updatedBedspace = archivedBedspace.copy(startDate = restartDate, endDate = null)

      every { bedRepositoryMock.findCas3Bedspace(premises.id, archivedBedspace.id) } returns archivedBedspace
      every { bedRepositoryMock.save(match { it.id == archivedBedspace.id }) } returns updatedBedspace
      every { cas3DomainEventServiceMock.saveBedspaceUnarchiveEvent(match { it.id == updatedBedspace.id }, currentStartDate, currentEndDate) } returns Unit

      val result = premisesService.unarchiveBedspace(premises, archivedBedspace.id, restartDate)

      assertThatCasResult(result).isSuccess().with { resultEntity ->
        assertThat(resultEntity.startDate).isEqualTo(restartDate)
        assertThat(resultEntity.endDate).isNull()
      }

      verify(exactly = 1) {
        bedRepositoryMock.save(
          match<BedEntity> {
            it.id == archivedBedspace.id &&
              it.startDate == restartDate &&
              it.endDate == null
          },
        )
      }

      verify(exactly = 1) {
        cas3DomainEventServiceMock.saveBedspaceUnarchiveEvent(
          match<BedEntity> {
            it.id == updatedBedspace.id
          },
          currentStartDate,
          currentEndDate,
        )
      }
    }

    @Test
    fun `unarchiveBedspace returns Success when unarchive a bedspace in archived premises will unarchive premises and bedspace successfully`() {
      val premises = temporaryAccommodationPremisesFactory
        .withEndDate(LocalDate.now().minusDays(7))
        .withStatus(PropertyStatus.archived)
        .produce()
      val currentPremisesStartDate = premises.startDate
      val currentBedspaceEndDate = LocalDate.now().minusDays(1)
      val archivedBedspace = createBedspace(premises, currentBedspaceEndDate)
      val currentBedspaceStartDate = archivedBedspace.startDate!!

      val restartDate = LocalDate.now().plusDays(1)
      val updatedBedspace = archivedBedspace.copy(startDate = restartDate, endDate = null)

      every { bedRepositoryMock.findCas3Bedspace(premises.id, archivedBedspace.id) } returns archivedBedspace
      every { bedRepositoryMock.save(match { it.id == archivedBedspace.id }) } returns updatedBedspace
      every { cas3DomainEventServiceMock.saveBedspaceUnarchiveEvent(match { it.id == updatedBedspace.id }, currentBedspaceStartDate, currentBedspaceEndDate) } returns Unit
      every { premisesRepositoryMock.save(match { it.id == premises.id }) } returns premises
      every { cas3DomainEventServiceMock.savePremisesUnarchiveEvent(match { it.id == premises.id }, premises.startDate, restartDate) } returns Unit

      val result = premisesService.unarchiveBedspace(premises, archivedBedspace.id, restartDate)

      assertThatCasResult(result).isSuccess().with { resultEntity ->
        assertThat(resultEntity.startDate).isEqualTo(restartDate)
        assertThat(resultEntity.endDate).isNull()
      }

      verify(exactly = 1) {
        bedRepositoryMock.save(
          match<BedEntity> {
            it.id == archivedBedspace.id &&
              it.startDate == restartDate &&
              it.endDate == null
          },
        )

        cas3DomainEventServiceMock.saveBedspaceUnarchiveEvent(
          match<BedEntity> {
            it.id == updatedBedspace.id
          },
          currentBedspaceStartDate,
          currentBedspaceEndDate,
        )
      }

      verify(exactly = 1) {
        premisesRepositoryMock.save(
          match<TemporaryAccommodationPremisesEntity> {
            it.id == premises.id &&
              it.startDate == restartDate &&
              it.endDate == null
          },
        )

        cas3DomainEventServiceMock.savePremisesUnarchiveEvent(
          match {
            it.id == premises.id
          },
          currentPremisesStartDate,
          restartDate,
        )
      }
    }

    @Test
    fun `unarchiveBedspace returns FieldValidationError when bedspace does not exist`() {
      val premises = temporaryAccommodationPremisesFactory.produce()
      val nonExistentBedspaceId = UUID.randomUUID()

      every { bedRepositoryMock.findCas3Bedspace(premises.id, nonExistentBedspaceId) } returns null

      val result = premisesService.unarchiveBedspace(premises, nonExistentBedspaceId, LocalDate.now())

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bedspaceId", "doesNotExist")
    }

    @Test
    fun `unarchiveBedspace returns FieldValidationError when bedspace is not archived`() {
      val premises = temporaryAccommodationPremisesFactory.produce()
      val onlineBedspace = createBedspace(premises)

      every { bedRepositoryMock.findCas3Bedspace(premises.id, onlineBedspace.id) } returns onlineBedspace

      val result = premisesService.unarchiveBedspace(premises, onlineBedspace.id, LocalDate.now())

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bedspaceId", "bedspaceNotArchived")
    }

    @Test
    fun `unarchiveBedspace returns FieldValidationError when restart date is more than 7 days in the past`() {
      val premises = temporaryAccommodationPremisesFactory.produce()
      val archivedBedspace = createBedspace(premises, LocalDate.now().minusDays(10))

      val restartDate = LocalDate.now().minusDays(8)

      every { bedRepositoryMock.findCas3Bedspace(premises.id, archivedBedspace.id) } returns archivedBedspace

      val result = premisesService.unarchiveBedspace(premises, archivedBedspace.id, restartDate)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.restartDate", "invalidRestartDateInThePast")
    }

    @Test
    fun `unarchiveBedspace returns FieldValidationError when restart date is more than 7 days in the future`() {
      val premises = temporaryAccommodationPremisesFactory.produce()
      val archivedBedspace = createBedspace(premises, LocalDate.now().minusDays(1))

      val restartDate = LocalDate.now().plusDays(8)

      every { bedRepositoryMock.findCas3Bedspace(premises.id, archivedBedspace.id) } returns archivedBedspace

      val result = premisesService.unarchiveBedspace(premises, archivedBedspace.id, restartDate)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.restartDate", "invalidRestartDateInTheFuture")
    }

    @Test
    fun `unarchiveBedspace returns FieldValidationError when restart date is before last archive end date`() {
      val premises = temporaryAccommodationPremisesFactory.produce()
      val lastArchiveEndDate = LocalDate.now().minusDays(5)
      val archivedBedspace = createBedspace(premises, lastArchiveEndDate)

      val restartDate = lastArchiveEndDate.minusDays(1)

      every { bedRepositoryMock.findCas3Bedspace(premises.id, archivedBedspace.id) } returns archivedBedspace

      val result = premisesService.unarchiveBedspace(premises, archivedBedspace.id, restartDate)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.restartDate", "beforeLastBedspaceArchivedDate")
    }

    @Test
    fun `unarchiveBedspace returns FieldValidationError when restart date equals last archive end date`() {
      val premises = temporaryAccommodationPremisesFactory.produce()
      val lastArchiveEndDate = LocalDate.now().minusDays(5)
      val archivedBedspace = createBedspace(premises, lastArchiveEndDate)

      val restartDate = lastArchiveEndDate

      every { bedRepositoryMock.findCas3Bedspace(premises.id, archivedBedspace.id) } returns archivedBedspace

      val result = premisesService.unarchiveBedspace(premises, archivedBedspace.id, restartDate)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.restartDate", "beforeLastBedspaceArchivedDate")
    }

    @Test
    fun `unarchiveBedspace allows restart date exactly 7 days in the past`() {
      val premises = temporaryAccommodationPremisesFactory.produce()
      val currentEndDate = LocalDate.now().minusDays(10)
      val archivedBedspace = createBedspace(premises, currentEndDate)
      val currentStartDate = archivedBedspace.startDate!!

      val restartDate = LocalDate.now().minusDays(7)
      val updatedBedspace = archivedBedspace.copy(startDate = restartDate, endDate = null)

      every { bedRepositoryMock.findCas3Bedspace(premises.id, archivedBedspace.id) } returns archivedBedspace
      every { bedRepositoryMock.save(any()) } returns updatedBedspace
      every { cas3DomainEventServiceMock.saveBedspaceUnarchiveEvent(match { it.id == updatedBedspace.id }, currentStartDate, currentEndDate) } returns Unit

      val result = premisesService.unarchiveBedspace(premises, archivedBedspace.id, restartDate)

      assertThatCasResult(result).isSuccess()
    }

    @Test
    fun `unarchiveBedspace allows restart date exactly 7 days in the future`() {
      val premises = temporaryAccommodationPremisesFactory.produce()
      val currentEndDate = LocalDate.now().minusDays(10)
      val archivedBedspace = createBedspace(premises, currentEndDate)
      val currentStartDate = archivedBedspace.startDate!!

      val restartDate = LocalDate.now().plusDays(7)
      val updatedBedspace = archivedBedspace.copy(startDate = restartDate, endDate = null)

      every { bedRepositoryMock.findCas3Bedspace(premises.id, archivedBedspace.id) } returns archivedBedspace
      every { cas3DomainEventServiceMock.saveBedspaceUnarchiveEvent(match { it.id == archivedBedspace.id }, currentStartDate, currentEndDate) } returns Unit
      every { bedRepositoryMock.save(any()) } returns updatedBedspace

      val result = premisesService.unarchiveBedspace(premises, archivedBedspace.id, restartDate)

      assertThatCasResult(result).isSuccess()
    }
  }

  @Nested
  inner class CancelArchiveBedspace {

    @Test
    fun `cancelArchiveBedspace returns Success when scheduled archive is cancelled`() {
      val premises = temporaryAccommodationPremisesFactory.produce()
      val room = RoomEntityFactory().withYieldedPremises { premises }.produce()
      val scheduledToArchiveBedspace = BedEntityFactory()
        .withYieldedRoom { room }
        .withStartDate(LocalDate.now().minusDays(30))
        .withEndDate(LocalDate.now().plusDays(1)) // Archived tomorrow
        .produce()

      premises.rooms.add(room)
      room.beds.add(scheduledToArchiveBedspace)

      val updatedBedspace = scheduledToArchiveBedspace.copy(endDate = null)

      every { bedRepositoryMock.save(any()) } returns updatedBedspace

      val result = premisesService.cancelArchiveBedspace(premises, scheduledToArchiveBedspace.id)

      assertThatCasResult(result).isSuccess().with { bed ->
        assertThat(bed.id).isEqualTo(scheduledToArchiveBedspace.id)
        assertThat(bed.endDate).isNull()
      }

      verify(exactly = 1) {
        bedRepositoryMock.save(
          match<BedEntity> {
            it.id == scheduledToArchiveBedspace.id && it.endDate == null
          },
        )
      }
    }

    @Test
    fun `cancelArchiveBedspace returns FieldValidationError when bedspace does not exist`() {
      val premises = temporaryAccommodationPremisesFactory.produce()
      val nonExistentBedspaceId = UUID.randomUUID()

      val result = premisesService.cancelArchiveBedspace(premises, nonExistentBedspaceId)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bedspaceId", "doesNotExist")
    }

    @Test
    fun `cancelArchiveBedspace returns FieldValidationError when bedspace is not scheduled to be archived`() {
      val premises = temporaryAccommodationPremisesFactory.produce()
      val room = RoomEntityFactory().withYieldedPremises { premises }.produce()
      val onlineBedspace = BedEntityFactory()
        .withYieldedRoom { room }
        .withStartDate(LocalDate.now().minusDays(10))
        .withEndDate(null) // Not archived
        .produce()

      premises.rooms.add(room)
      room.beds.add(onlineBedspace)

      val result = premisesService.cancelArchiveBedspace(premises, onlineBedspace.id)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bedspaceId", "bedspaceNotScheduledToArchive")
    }

    @Test
    fun `cancelArchiveBedspace returns FieldValidationError when bedspace is already archived`() {
      val premises = temporaryAccommodationPremisesFactory.produce()
      val room = RoomEntityFactory().withYieldedPremises { premises }.produce()
      val onlineBedspace = BedEntityFactory()
        .withYieldedRoom { room }
        .withStartDate(LocalDate.now().minusDays(10))
        .withEndDate(LocalDate.now().minusDays(1)) // Archived yesterday
        .produce()

      premises.rooms.add(room)
      room.beds.add(onlineBedspace)

      val result = premisesService.cancelArchiveBedspace(premises, onlineBedspace.id)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bedspaceId", "bedspaceAlreadyArchived")
    }

    @Test
    fun `cancelArchiveBedspace returns FieldValidationError when bedspace is already archived (today)`() {
      val premises = temporaryAccommodationPremisesFactory.produce()
      val room = RoomEntityFactory().withYieldedPremises { premises }.produce()
      val onlineBedspace = BedEntityFactory()
        .withYieldedRoom { room }
        .withStartDate(LocalDate.now().minusDays(10))
        .withEndDate(LocalDate.now()) // Archived today
        .produce()

      premises.rooms.add(room)
      room.beds.add(onlineBedspace)

      val result = premisesService.cancelArchiveBedspace(premises, onlineBedspace.id)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bedspaceId", "bedspaceAlreadyArchived")
    }
  }

  @Nested
  inner class CancelScheduledArchivePremises {
    @Test
    fun `When cancelling a scheduled archive premise whose end date is in the future, return success`() {
      val premises = updatePremisesEntity()
      premises.endDate = LocalDate.now().plusDays(1)
      premises.status = PropertyStatus.active
      every { premisesRepositoryMock.save(any()) } returns premises
      every { premisesRepositoryMock.findTemporaryAccommodationPremisesByIdOrNull(premises.id) } returns premises

      val result = premisesService.cancelScheduledArchivePremises(
        premises.id,
      )

      assertThatCasResult(result).isSuccess()
        .with { updatedPremises ->
          updatedPremises.endDate = null
        }
    }

    @Test
    fun `When cancelling a scheduled archive premise whose end date is null, return premisesNotScheduledToArchive`() {
      val premises = updatePremisesEntity()
      premises.endDate = null

      every { premisesRepositoryMock.findTemporaryAccommodationPremisesByIdOrNull(premises.id) } returns premises

      val result = premisesService.cancelScheduledArchivePremises(
        premises.id,
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage(
        "$.premisesId",
        "premisesNotScheduledToArchive",
      )
    }

    @ParameterizedTest(name = "When cancelling a scheduled archive premise whose end date is [{0}], return premisesAlreadyArchived")
    @CsvSource(
      value = [
        "Today",
        "Before Today",
      ],
    )
    fun `When cancelling a scheduled archive premise whose end date is in the past, return premisesAlreadyArchived`(label: String) {
      val premises = updatePremisesEntity()
      premises.endDate = if (label == "Today") LocalDate.now() else LocalDate.now().minusDays(1)

      every { premisesRepositoryMock.findTemporaryAccommodationPremisesByIdOrNull(premises.id) } returns premises

      val result = premisesService.cancelScheduledArchivePremises(
        premises.id,
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage(
        "$.premisesId",
        "premisesAlreadyArchived",
      )
    }

    private fun updatePremisesEntity(): TemporaryAccommodationPremisesEntity {
      val probationRegion = ProbationRegionEntityFactory()
        .withApArea(ApAreaEntityFactory().produce())
        .produce()

      val localAuthority = LocalAuthorityEntityFactory()
        .produce()

      val probationDeliveryUnit = ProbationDeliveryUnitEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      return temporaryAccommodationPremisesFactory
        .withService(ServiceName.temporaryAccommodation.value)
        .withProbationRegion(probationRegion)
        .withProbationDeliveryUnit(probationDeliveryUnit)
        .withLocalAuthorityArea(localAuthority)
        .withStatus(PropertyStatus.archived)
        .produce()
    }
  }

  @Nested
  inner class CancelUnarchiveBedspace {
    @Test
    fun `cancelUnarchiveBedspace returns Success when scheduled unarchive is cancelled`() {
      val bedspaceId = UUID.randomUUID()
      val premises = temporaryAccommodationPremisesFactory.produce()
      val room = RoomEntityFactory().withYieldedPremises { premises }.produce()
      val scheduledToUnarchiveBedspace = BedEntityFactory()
        .withId(bedspaceId)
        .withYieldedRoom { room }
        .withStartDate(LocalDate.now().plusDays(10))
        .withEndDate(null)
        .produce()
      val originalStartDate = LocalDate.now().minusDays(5)
      val originalEndDate = LocalDate.now().minusDays(4)

      premises.rooms.add(room)
      room.beds.add(scheduledToUnarchiveBedspace)

      val updatedBedspace = scheduledToUnarchiveBedspace.copy(startDate = originalStartDate, endDate = originalEndDate)
      val eventDetails = CAS3BedspaceUnarchiveEventDetails(
        bedspaceId = bedspaceId,
        premisesId = premises.id,
        userId = UUID.randomUUID(),
        currentStartDate = originalStartDate,
        currentEndDate = originalEndDate,
        newStartDate = scheduledToUnarchiveBedspace.startDate!!,
      )

      every { bedRepositoryMock.findById(bedspaceId) } returns Optional.of(scheduledToUnarchiveBedspace)
      every { bedRepositoryMock.save(any()) } returns updatedBedspace
      every { cas3DomainEventServiceMock.getLastBedspaceUnarchiveEventDetails(bedspaceId) } returns eventDetails

      val result = premisesService.cancelUnarchiveBedspace(bedspaceId)

      assertThatCasResult(result).isSuccess().with { bed ->
        assertThat(bed.id).isEqualTo(bedspaceId)
        assertThat(bed.startDate).isEqualTo(originalStartDate)
        assertThat(bed.endDate).isEqualTo(originalEndDate)
      }

      verify(exactly = 1) {
        bedRepositoryMock.save(
          match<BedEntity> {
            it.id == bedspaceId && it.startDate == originalStartDate && it.endDate == originalEndDate
          },
        )
      }
    }

    @Test
    fun `cancelUnarchiveBedspace returns FieldValidationError when bedspace unarchive event does not exist`() {
      val bedspaceId = UUID.randomUUID()
      val premises = temporaryAccommodationPremisesFactory.produce()
      val room = RoomEntityFactory().withYieldedPremises { premises }.produce()
      val scheduledToUnarchiveBedspace = BedEntityFactory()
        .withId(bedspaceId)
        .withYieldedRoom { room }
        .withStartDate(LocalDate.now().plusDays(10))
        .withEndDate(null)
        .produce()

      premises.rooms.add(room)
      room.beds.add(scheduledToUnarchiveBedspace)

      every { cas3DomainEventServiceMock.getLastBedspaceUnarchiveEventDetails(bedspaceId) } returns null
      every { bedRepositoryMock.findById(bedspaceId) } returns Optional.of(scheduledToUnarchiveBedspace)

      val result = premisesService.cancelUnarchiveBedspace(bedspaceId)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bedspaceId", "bedspaceNotScheduledToUnarchive")
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service.Cas3PremisesServiceTest#startDateProvider")
    fun `cancelUnarchiveBedspace returns FieldValidationError when bedspace startDate (already online)`(startDate: LocalDate) {
      val bedspaceId = UUID.randomUUID()
      val premises = temporaryAccommodationPremisesFactory.produce()
      val room = RoomEntityFactory().withYieldedPremises { premises }.produce()
      val scheduledToUnarchiveBedspace = BedEntityFactory()
        .withId(bedspaceId)
        .withYieldedRoom { room }
        .withStartDate(startDate)
        .withEndDate(null)
        .produce()

      premises.rooms.add(room)
      room.beds.add(scheduledToUnarchiveBedspace)

      every { bedRepositoryMock.findById(bedspaceId) } returns Optional.of(scheduledToUnarchiveBedspace)

      val result = premisesService.cancelUnarchiveBedspace(bedspaceId)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bedspaceId", "bedspaceAlreadyOnline")
    }

    @Test
    fun `cancelUnarchiveBedspace returns FieldValidationError when bedspace does not exist`() {
      val bedspaceId = UUID.randomUUID()
      val premises = temporaryAccommodationPremisesFactory.produce()
      val room = RoomEntityFactory().withYieldedPremises { premises }.produce()
      val scheduledToUnarchiveBedspace = BedEntityFactory()
        .withYieldedRoom { room }
        .withStartDate(null)
        .withEndDate(null)
        .produce()

      every { bedRepositoryMock.findById(bedspaceId) } returns Optional.empty()

      premises.rooms.add(room)
      room.beds.add(scheduledToUnarchiveBedspace)

      val result = premisesService.cancelUnarchiveBedspace(bedspaceId)

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.bedspaceId", "doesNotExist")
    }
  }

  private fun createPremisesAndBedspace(): Pair<TemporaryAccommodationPremisesEntity, BedEntity> {
    val premises = temporaryAccommodationPremisesFactory.produce()
    val bedspace = createBedspace(premises)

    return Pair(premises, bedspace)
  }

  private fun createBedspace(premises: TemporaryAccommodationPremisesEntity, endDate: LocalDate? = null): BedEntity {
    val room = RoomEntityFactory()
      .withName(randomStringMultiCaseWithNumbers(10))
      .withPremises(premises)
      .produce()

    return BedEntityFactory()
      .withRoom(room)
      .withStartDate(LocalDate.now().minusDays(180))
      .withEndDate(endDate)
      .produce()
  }

  companion object {
    @JvmStatic
    fun startDateProvider(): Stream<Arguments> {
      val startDateNow = LocalDate.now().toString()
      val starDatePast = LocalDate.now().minusDays(1).toString()
      return Stream.of(
        Arguments.of(startDateNow),
        Arguments.of(starDatePast),
      )
    }
  }
}
