package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas3

import arrow.core.Ior
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingSummaryForAvailabilityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3VoidBedspaceCancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3VoidBedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3VoidBedspaceReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspaceCancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspaceReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Availability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

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
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = premises.postcode,
        localAuthorityAreaId = premises.localAuthorityArea?.id,
        probationRegionId = premises.probationRegion.id,
        characteristicIds = emptyList(),
        notes = premises.notes,
        probationDeliveryUnitId = premises.probationDeliveryUnit?.id!!,
        turnaroundWorkingDayCount = premises.turnaroundWorkingDays,
      )

      assertThatCasResult(result).isSuccess()
        .with { updatedPremises ->
          assertThat(updatedPremises.id).isNotNull()
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

      every { probationRegionRepositoryMock.findByIdOrNull(nonExistProbationRegionId) } returns null
      every { localAuthorityAreaRepositoryMock.findByIdOrNull(localAuthorityArea.id) } returns localAuthorityArea
      every { probationDeliveryUnitRepositoryMock.findByIdAndProbationRegionId(probationDeliveryUnit.id, any()) } returns probationDeliveryUnit

      val result = premisesService.updatePremises(
        premises = premises,
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = premises.postcode,
        localAuthorityAreaId = premises.localAuthorityArea?.id,
        probationRegionId = nonExistProbationRegionId,
        characteristicIds = emptyList(),
        notes = premises.notes,
        probationDeliveryUnitId = premises.probationDeliveryUnit?.id!!,
        turnaroundWorkingDayCount = premises.turnaroundWorkingDays,
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.probationRegionId", "doesNotExist")
    }

    @Test
    fun `When update a premises with a non exist local authority returns a FieldValidationError with the correct message`() {
      val premises = updatePremisesEntity()
      val probationRegion = premises.probationRegion
      val probationDeliveryUnit = premises.probationDeliveryUnit!!
      val nonExistLocalAuthorityId = UUID.randomUUID()

      every { probationRegionRepositoryMock.findByIdOrNull(premises.probationRegion.id) } returns probationRegion
      every { localAuthorityAreaRepositoryMock.findByIdOrNull(nonExistLocalAuthorityId) } returns null
      every { probationDeliveryUnitRepositoryMock.findByIdAndProbationRegionId(probationDeliveryUnit.id, any()) } returns probationDeliveryUnit

      val result = premisesService.updatePremises(
        premises = premises,
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = premises.postcode,
        localAuthorityAreaId = nonExistLocalAuthorityId,
        probationRegionId = premises.probationRegion.id,
        characteristicIds = emptyList(),
        notes = premises.notes,
        probationDeliveryUnitId = premises.probationDeliveryUnit?.id!!,
        turnaroundWorkingDayCount = premises.turnaroundWorkingDays,
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.localAuthorityAreaId", "doesNotExist")
    }

    @Test
    fun `When update a premises with a non exist probation delivery unit returns a FieldValidationError with the correct message`() {
      val premises = updatePremisesEntity()
      val probationRegion = premises.probationRegion
      val localAuthorityArea = premises.localAuthorityArea!!
      val nonExistPduId = UUID.randomUUID()

      every { probationRegionRepositoryMock.findByIdOrNull(probationRegion.id) } returns probationRegion
      every { localAuthorityAreaRepositoryMock.findByIdOrNull(localAuthorityArea.id) } returns localAuthorityArea
      every { probationDeliveryUnitRepositoryMock.findByIdAndProbationRegionId(nonExistPduId, probationRegion.id) } returns null

      val result = premisesService.updatePremises(
        premises = premises,
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = premises.postcode,
        localAuthorityAreaId = premises.localAuthorityArea?.id!!,
        probationRegionId = premises.probationRegion.id,
        characteristicIds = emptyList(),
        notes = premises.notes,
        probationDeliveryUnitId = nonExistPduId,
        turnaroundWorkingDayCount = premises.turnaroundWorkingDays,
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.probationDeliveryUnitId", "doesNotExist")
    }

    @Test
    fun `When update a premises with empty address returns a FieldValidationError with the correct message`() {
      val premises = updatePremisesEntity()

      mockCommonPremisesDependencies(premises)

      val result = premisesService.updatePremises(
        premises = premises,
        addressLine1 = "",
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = premises.postcode,
        localAuthorityAreaId = premises.localAuthorityArea?.id!!,
        probationRegionId = premises.probationRegion.id,
        characteristicIds = emptyList(),
        notes = premises.notes,
        probationDeliveryUnitId = premises.probationDeliveryUnit?.id!!,
        turnaroundWorkingDayCount = premises.turnaroundWorkingDays,
      )
      assertThatCasResult(result).isFieldValidationError().hasMessage("$.address", "empty")
    }

    @Test
    fun `When update a premises with empty postcode returns a FieldValidationError with the correct message`() {
      val premises = updatePremisesEntity()

      mockCommonPremisesDependencies(premises)

      val result = premisesService.updatePremises(
        premises = premises,
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = "",
        localAuthorityAreaId = premises.localAuthorityArea?.id!!,
        probationRegionId = premises.probationRegion.id,
        characteristicIds = emptyList(),
        notes = premises.notes,
        probationDeliveryUnitId = premises.probationDeliveryUnit?.id!!,
        turnaroundWorkingDayCount = premises.turnaroundWorkingDays,
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.postcode", "empty")
    }

    @Test
    fun `When update a premises with turnaround working days in negative returns a FieldValidationError with the correct message`() {
      val premises = updatePremisesEntity()

      mockCommonPremisesDependencies(premises)

      val result = premisesService.updatePremises(
        premises = premises,
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = premises.postcode,
        localAuthorityAreaId = premises.localAuthorityArea?.id!!,
        probationRegionId = premises.probationRegion.id,
        characteristicIds = emptyList(),
        notes = premises.notes,
        probationDeliveryUnitId = premises.probationDeliveryUnit?.id!!,
        turnaroundWorkingDayCount = -2,
      )

      assertThatCasResult(result).isFieldValidationError().hasMessage("$.turnaroundWorkingDayCount", "isNotAPositiveInteger")
    }

    @Test
    fun `When update a premises with a non exist characteristic returns FieldValidationError with the correct message`() {
      val premises = updatePremisesEntity()
      val nonExistCharacteristicId = UUID.randomUUID()

      mockCommonPremisesDependencies(premises)
      every { characteristicServiceMock.getCharacteristic(nonExistCharacteristicId) } returns null

      val result = premisesService.updatePremises(
        premises = premises,
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = premises.postcode,
        localAuthorityAreaId = premises.localAuthorityArea?.id!!,
        probationRegionId = premises.probationRegion.id,
        characteristicIds = listOf(nonExistCharacteristicId),
        notes = premises.notes,
        probationDeliveryUnitId = premises.probationDeliveryUnit?.id!!,
        turnaroundWorkingDayCount = -2,
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
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = premises.postcode,
        localAuthorityAreaId = premises.localAuthorityArea?.id!!,
        probationRegionId = premises.probationRegion.id,
        characteristicIds = listOf(premisesCharacteristic.id),
        notes = premises.notes,
        probationDeliveryUnitId = premises.probationDeliveryUnit?.id!!,
        turnaroundWorkingDayCount = -2,
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
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = premises.postcode,
        localAuthorityAreaId = premises.localAuthorityArea?.id!!,
        probationRegionId = premises.probationRegion.id,
        characteristicIds = listOf(premisesCharacteristic.id),
        notes = premises.notes,
        probationDeliveryUnitId = premises.probationDeliveryUnit?.id!!,
        turnaroundWorkingDayCount = -2,
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
        .withProbationRegion(probationRegion)
        .withProbationDeliveryUnit(probationDeliveryUnit)
        .withLocalAuthorityArea(localAuthority)
        .produce()
    }

    private fun mockCommonPremisesDependencies(premises: TemporaryAccommodationPremisesEntity) {
      val probationRegion = premises.probationRegion
      val localAuthorityArea = premises.localAuthorityArea!!
      val probationDeliveryUnit = premises.probationDeliveryUnit!!
      every { probationRegionRepositoryMock.findByIdOrNull(probationRegion.id) } returns probationRegion
      every { localAuthorityAreaRepositoryMock.findByIdOrNull(localAuthorityArea.id) } returns localAuthorityArea
      every { probationDeliveryUnitRepositoryMock.findByIdAndProbationRegionId(probationDeliveryUnit.id, probationRegion.id) } returns probationDeliveryUnit
    }
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

    private fun createPremisesAndBedspace(): Pair<TemporaryAccommodationPremisesEntity, BedEntity> {
      val premises = temporaryAccommodationPremisesFactory.produce()

      val room = RoomEntityFactory()
        .withName(randomStringMultiCaseWithNumbers(10))
        .withPremises(premises)
        .produce()

      val bedspace = BedEntityFactory()
        .withRoom(room)
        .withStartDate(LocalDate.now().minusDays(5))
        .produce()

      return Pair(premises, bedspace)
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
    fun `createVoidBedspaces returns validation error message when void start date is the same as bedspace end date`() {
      val premisesEntity = temporaryAccommodationPremisesFactory.produce()

      val room = RoomEntityFactory()
        .withPremises(premisesEntity)
        .produce()

      val bed = BedEntityFactory()
        .withYieldedRoom { room }
        .withStartDate(LocalDate.parse("2022-08-20"))
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
        startDate = LocalDate.parse("2022-08-24"),
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
  }
}
