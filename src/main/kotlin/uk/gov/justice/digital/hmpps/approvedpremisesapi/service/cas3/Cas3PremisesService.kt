package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3

import arrow.core.Either
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspaceCancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspaceCancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspaceReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Availability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilExclusiveEnd
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas3PremisesService(
  private val premisesRepository: PremisesRepository,
  private val cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository,
  private val cas3VoidBedspaceReasonRepository: Cas3VoidBedspaceReasonRepository,
  private val cas3VoidBedspaceCancellationRepository: Cas3VoidBedspaceCancellationRepository,
  private val bookingRepository: BookingRepository,
  private val localAuthorityAreaRepository: LocalAuthorityAreaRepository,
  private val probationRegionRepository: ProbationRegionRepository,
  private val probationDeliveryUnitRepository: ProbationDeliveryUnitRepository,
  private val roomRepository: RoomRepository,
  private val bedspaceRepository: BedRepository,
  private val characteristicService: CharacteristicService,
) {
  fun getPremises(premisesId: UUID): TemporaryAccommodationPremisesEntity? = premisesRepository.findTemporaryAccommodationPremisesByIdOrNull(premisesId)

  fun getAllPremisesSummaries(regionId: UUID, postcodeOrAddress: String?, premisesStatus: Cas3PremisesStatus?): List<TemporaryAccommodationPremisesSummary> {
    val postcodeOrAddressWithoutWhitespace = postcodeOrAddress?.filter { !it.isWhitespace() }
    return premisesRepository.findAllCas3PremisesSummary(regionId, postcodeOrAddress, postcodeOrAddressWithoutWhitespace, premisesStatus?.transformStatus())
  }

  private fun Cas3PremisesStatus.transformStatus() = when (this) {
    Cas3PremisesStatus.archived -> PropertyStatus.archived.toString()
    Cas3PremisesStatus.online -> PropertyStatus.active.toString()
  }

  @Deprecated("This function is replaced by createNewPremises in the same class")
  @SuppressWarnings("CyclomaticComplexMethod")
  fun createNewPremises(
    addressLine1: String,
    addressLine2: String?,
    town: String?,
    postcode: String,
    localAuthorityAreaId: UUID?,
    probationRegionId: UUID,
    name: String,
    notes: String?,
    characteristicIds: List<UUID>,
    status: PropertyStatus,
    probationDeliveryUnitIdentifier: Either<String, UUID>?,
    turnaroundWorkingDays: Int?,
  ): CasResult<TemporaryAccommodationPremisesEntity> = validatedCasResult {
    val probationRegion = probationRegionRepository.findByIdOrNull(probationRegionId)
    if (probationRegion == null) {
      "$.probationRegionId" hasValidationError "doesNotExist"
    }

    val localAuthorityArea = when (localAuthorityAreaId) {
      null -> null
      else -> {
        val localAuthorityArea = localAuthorityAreaRepository.findByIdOrNull(localAuthorityAreaId)
        if (localAuthorityArea == null) {
          "$.localAuthorityAreaId" hasValidationError "doesNotExist"
        }
        localAuthorityArea
      }
    }

    // start of validation
    if (addressLine1.isEmpty()) {
      "$.address" hasValidationError "empty"
    }

    if (postcode.isEmpty()) {
      "$.postcode" hasValidationError "empty"
    }

    if (name.isEmpty()) {
      "$.name" hasValidationError "empty"
    }

    if (!premisesRepository.nameIsUniqueForType(name, TemporaryAccommodationPremisesEntity::class.java)) {
      "$.name" hasValidationError "notUnique"
    }

    val probationDeliveryUnit =
      tryGetProbationDeliveryUnit(probationDeliveryUnitIdentifier, probationRegionId) { property, err ->
        property hasValidationError err
      }

    if (turnaroundWorkingDays != null && turnaroundWorkingDays < 0) {
      "$.turnaroundWorkingDayCount" hasValidationError "isNotAPositiveInteger"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    val premises = TemporaryAccommodationPremisesEntity(
      id = UUID.randomUUID(),
      name = name,
      addressLine1 = addressLine1,
      addressLine2 = addressLine2,
      town = town,
      postcode = postcode,
      latitude = null,
      longitude = null,
      probationRegion = probationRegion!!,
      localAuthorityArea = localAuthorityArea,
      startDate = LocalDate.now(),
      bookings = mutableListOf(),
      lostBeds = mutableListOf(),
      notes = if (notes.isNullOrEmpty()) "" else notes,
      emailAddress = null,
      rooms = mutableListOf(),
      characteristics = mutableListOf(),
      status = status,
      probationDeliveryUnit = probationDeliveryUnit!!,
      turnaroundWorkingDays = turnaroundWorkingDays ?: 2,
    )

    val characteristicEntities = getAndValidateCharacteristics(characteristicIds, premises, validationErrors)

    if (validationErrors.any()) {
      return fieldValidationError
    }
    // end of validation
    premises.characteristics.addAll(characteristicEntities.map { it!! })
    premisesRepository.save(premises)

    return success(premises)
  }

  @SuppressWarnings("CyclomaticComplexMethod")
  fun createNewPremises(
    reference: String,
    addressLine1: String,
    addressLine2: String?,
    town: String?,
    postcode: String,
    localAuthorityAreaId: UUID?,
    probationRegionId: UUID,
    probationDeliveryUnitId: UUID,
    characteristicIds: List<UUID>,
    notes: String?,
    turnaroundWorkingDays: Int?,
  ): CasResult<TemporaryAccommodationPremisesEntity> = validatedCasResult {
    val probationRegion = probationRegionRepository.findByIdOrNull(probationRegionId)
    if (probationRegion == null) {
      "$.probationRegionId" hasValidationError "doesNotExist"
    }

    val localAuthorityArea = when (localAuthorityAreaId) {
      null -> null
      else -> {
        val localAuthorityArea = localAuthorityAreaRepository.findByIdOrNull(localAuthorityAreaId)
        if (localAuthorityArea == null) {
          "$.localAuthorityAreaId" hasValidationError "doesNotExist"
        }
        localAuthorityArea
      }
    }

    val probationDeliveryUnit = probationDeliveryUnitRepository.findByIdAndProbationRegionId(probationDeliveryUnitId, probationRegionId)

    if (probationDeliveryUnit == null) {
      "$.probationDeliveryUnitId" hasValidationError "doesNotExist"
    }

    // start of validation
    if (reference.isEmpty()) {
      "$.reference" hasValidationError "empty"
    } else if (!premisesRepository.nameIsUniqueForType(reference, TemporaryAccommodationPremisesEntity::class.java)) {
      "$.reference" hasValidationError "notUnique"
    }

    if (addressLine1.isEmpty()) {
      "$.address" hasValidationError "empty"
    }

    if (postcode.isEmpty()) {
      "$.postcode" hasValidationError "empty"
    }

    if (turnaroundWorkingDays != null && turnaroundWorkingDays < 0) {
      "$.turnaroundWorkingDays" hasValidationError "isNotAPositiveInteger"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    val premises = TemporaryAccommodationPremisesEntity(
      id = UUID.randomUUID(),
      name = reference,
      addressLine1 = addressLine1,
      addressLine2 = addressLine2,
      town = town,
      postcode = postcode,
      latitude = null,
      longitude = null,
      probationRegion = probationRegion!!,
      localAuthorityArea = localAuthorityArea,
      startDate = LocalDate.now(),
      bookings = mutableListOf(),
      lostBeds = mutableListOf(),
      notes = if (notes.isNullOrEmpty()) "" else notes,
      emailAddress = null,
      rooms = mutableListOf(),
      characteristics = mutableListOf(),
      status = PropertyStatus.active,
      probationDeliveryUnit = probationDeliveryUnit!!,
      turnaroundWorkingDays = turnaroundWorkingDays ?: 2,
    )

    val characteristicEntities = getAndValidateCharacteristics(characteristicIds, premises, validationErrors)

    if (validationErrors.any()) {
      return fieldValidationError
    }
    // end of validation

    premises.characteristics.addAll(characteristicEntities.map { it!! })
    premisesRepository.save(premises)

    return success(premises)
  }

  @SuppressWarnings("CyclomaticComplexMethod")
  fun updatePremises(
    premisesId: UUID,
    addressLine1: String,
    addressLine2: String?,
    town: String?,
    postcode: String,
    localAuthorityAreaId: UUID?,
    probationRegionId: UUID,
    characteristicIds: List<UUID>,
    notes: String?,
    status: PropertyStatus,
    probationDeliveryUnitIdentifier: Either<String, UUID>?,
    turnaroundWorkingDays: Int?,
  ): AuthorisableActionResult<ValidatableActionResult<TemporaryAccommodationPremisesEntity>> {
    val premises = premisesRepository.findTemporaryAccommodationPremisesByIdOrNull(premisesId)
      ?: return AuthorisableActionResult.NotFound()

    val validationErrors = ValidationErrors()

    val localAuthorityArea = when (localAuthorityAreaId) {
      null -> null
      else -> {
        val localAuthorityArea = localAuthorityAreaRepository.findByIdOrNull(localAuthorityAreaId)
        if (localAuthorityArea == null) {
          validationErrors["$.localAuthorityAreaId"] = "doesNotExist"
        }
        localAuthorityArea
      }
    }

    val probationRegion = probationRegionRepository.findByIdOrNull(probationRegionId)

    if (probationRegion == null) {
      validationErrors["$.probationRegionId"] = "doesNotExist"
    }

    val probationDeliveryUnit =
      tryGetProbationDeliveryUnit(probationDeliveryUnitIdentifier, probationRegionId) { property, err ->
        validationErrors[property] = err
      }

    val characteristicEntities = getAndValidateCharacteristics(characteristicIds, premises, validationErrors)

    if (turnaroundWorkingDays != null && turnaroundWorkingDays < 0) {
      validationErrors["$.turnaroundWorkingDayCount"] = "isNotAPositiveInteger"
    }

    if (status == PropertyStatus.archived) {
      val futureBookings = bookingRepository.findFutureBookingsByPremisesIdAndStatus(
        ServiceName.temporaryAccommodation.value,
        premisesId,
        LocalDate.now(),
        listOf(BookingStatus.arrived, BookingStatus.confirmed, BookingStatus.provisional),
      )

      if (futureBookings.any()) {
        validationErrors["$.status"] = "existingBookings"
      }
    }

    if (validationErrors.any()) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.FieldValidationError(validationErrors),
      )
    }

    premises.let {
      it.addressLine1 = addressLine1
      it.addressLine2 = addressLine2
      it.town = town
      it.postcode = postcode
      it.localAuthorityArea = localAuthorityArea
      it.probationRegion = probationRegion!!
      it.characteristics = characteristicEntities.map { it!! }.toMutableList()
      it.notes = if (notes.isNullOrEmpty()) "" else notes
      it.status = status
      it.probationDeliveryUnit = probationDeliveryUnit!!
      if (turnaroundWorkingDays != null) {
        it.turnaroundWorkingDays = turnaroundWorkingDays
      }
    }

    if (status == PropertyStatus.archived) {
      premises.rooms.forEach { room ->
        room.beds.forEach { bed ->
          bed.endDate = LocalDate.now()
        }
      }
    }

    val savedPremises = premisesRepository.save(premises)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedPremises),
    )
  }

  fun renamePremises(
    premisesId: UUID,
    name: String,
  ): AuthorisableActionResult<ValidatableActionResult<TemporaryAccommodationPremisesEntity>> {
    val premises = premisesRepository.findTemporaryAccommodationPremisesByIdOrNull(premisesId) ?: return AuthorisableActionResult.NotFound()

    return AuthorisableActionResult.Success(
      validated {
        if (!premisesRepository.nameIsUniqueForType(name, premises::class.java)) {
          "$.name" hasValidationError "notUnique"
        }

        if (validationErrors.any()) {
          return@validated fieldValidationError
        }

        premises.name = name

        return@validated success(premisesRepository.save(premises))
      },
    )
  }

  fun getBedspaceCount(premises: PremisesEntity): Int = premisesRepository.getBedCount(premises)

  @SuppressWarnings("TooGenericExceptionThrown")
  fun getAvailabilityForRange(
    premises: PremisesEntity,
    startDate: LocalDate,
    endDate: LocalDate,
  ): Map<LocalDate, Availability> {
    if (endDate.isBefore(startDate)) throw RuntimeException("startDate must be before endDate when calculating availability for range")

    val bookings = bookingRepository.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate)
    val voidBedspaces = cas3VoidBedspacesRepository.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate)

    return startDate.getDaysUntilExclusiveEnd(endDate).map { date ->
      val bookingsOnDay = bookings.filter { booking -> booking.getArrivalDate() <= date && booking.getDepartureDate() > date }
      val voidBedspacesOnDay = voidBedspaces.filter { voidBedspace -> voidBedspace.startDate <= date && voidBedspace.endDate > date && voidBedspace.cancellation == null }

      Availability(
        date = date,
        pendingBookings = bookingsOnDay.count { !it.getArrived() && !it.getIsNotArrived() && !it.getCancelled() },
        arrivedBookings = bookingsOnDay.count { it.getArrived() },
        nonArrivedBookings = bookingsOnDay.count { it.getIsNotArrived() },
        cancelledBookings = bookingsOnDay.count { it.getCancelled() },
        voidBedspaces = voidBedspacesOnDay.size,
      )
    }.associateBy { it.date }
  }

  @SuppressWarnings("MagicNumber")
  fun createBedspace(
    premises: PremisesEntity,
    bedspaceReference: String,
    startDate: LocalDate,
    notes: String?,
    characteristicIds: List<UUID>,
  ): CasResult<BedEntity> = validatedCasResult {
    var room = RoomEntity(
      id = UUID.randomUUID(),
      name = bedspaceReference.trim(),
      code = null,
      notes = notes,
      premises = premises,
      beds = mutableListOf(),
      characteristics = mutableListOf(),
    )

    if (bedspaceReference.isEmpty()) {
      "$.reference" hasValidationError "empty"
    }

    if (startDate.isBefore(LocalDate.now().minusDays(7))) {
      "$.startDate" hasValidationError "invalidStartDateInThePast"
    }

    if (startDate.isAfter(LocalDate.now().plusDays(7))) {
      "$.startDate" hasValidationError "invalidStartDateInTheFuture"
    }

    val characteristicEntities = getAndValidateCharacteristics(characteristicIds, room, validationErrors)

    if (validationErrors.any()) {
      return fieldValidationError
    }

    room.characteristics.addAll(characteristicEntities.map { it!! })
    room = roomRepository.save(room)

    val bedspace = BedEntity(
      id = UUID.randomUUID(),
      name = "default-bed",
      code = null,
      room = room,
      startDate = startDate,
      endDate = null,
      createdAt = OffsetDateTime.now(),
    )
    bedspaceRepository.save(bedspace)
    room.beds.add(bedspace)

    return success(bedspace)
  }

  fun updateBedspace(
    premises: PremisesEntity,
    bedspaceId: UUID,
    bedspaceReference: String,
    notes: String?,
    characteristicIds: List<UUID>,
  ): CasResult<BedEntity> = validatedCasResult {
    val bedspace = bedspaceRepository.findByIdOrNull(bedspaceId)
      ?: return CasResult.NotFound("Bedspace", bedspaceId.toString())

    var room = bedspace.room

    if (bedspace.room.premises != premises) {
      return CasResult.GeneralValidationError("The bedspace does not belong to the specified premises.")
    }

    if (bedspaceReference.isEmpty()) {
      "$.reference" hasValidationError "empty"
    }

    val characteristicEntities = getAndValidateCharacteristics(characteristicIds, room, validationErrors)

    if (validationErrors.any()) {
      return fieldValidationError
    }

    room.name = bedspaceReference
    room.notes = notes
    room.characteristics = characteristicEntities.map { it!! }.toMutableList()

    val updatedRoom = roomRepository.save(room)

    return success(updatedRoom.beds.first())
  }

  @Suppress("ComplexCondition")
  fun createVoidBedspaces(
    premises: PremisesEntity,
    startDate: LocalDate,
    endDate: LocalDate,
    reasonId: UUID,
    referenceNumber: String?,
    notes: String?,
    bedId: UUID,
  ): ValidatableActionResult<Cas3VoidBedspaceEntity> = validated {
    if (endDate.isBefore(startDate)) {
      "$.endDate" hasValidationError "beforeStartDate"
    }

    val bed = premises.rooms.flatMap { it.beds }.firstOrNull { it.id == bedId }
    if (bed == null) {
      "$.bedId" hasValidationError "doesNotExist"
    } else if (bed.endDate != null && startDate >= bed.endDate) {
      "$.startDate" hasValidationError "voidStartDateAfterBedspaceEndDate"
    }

    val reason = cas3VoidBedspaceReasonRepository.findByIdOrNull(reasonId)
    if (reason == null) {
      "$.reason" hasValidationError "doesNotExist"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    val voidBedspacesEntity = cas3VoidBedspacesRepository.save(
      Cas3VoidBedspaceEntity(
        id = UUID.randomUUID(),
        premises = premises,
        startDate = startDate,
        endDate = endDate,
        bed = bed!!,
        reason = reason!!,
        referenceNumber = referenceNumber,
        notes = notes,
        cancellation = null,
        cancellationDate = null,
        cancellationNotes = null,
        bedspace = null,
      ),
    )

    return success(voidBedspacesEntity)
  }

  fun updateVoidBedspaces(
    voidBedspaceId: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    reasonId: UUID,
    referenceNumber: String?,
    notes: String?,
  ): AuthorisableActionResult<ValidatableActionResult<Cas3VoidBedspaceEntity>> {
    val voidBedspace = cas3VoidBedspacesRepository.findByIdOrNull(voidBedspaceId)
      ?: return AuthorisableActionResult.NotFound()

    return AuthorisableActionResult.Success(
      validated {
        if (endDate.isBefore(startDate)) {
          "$.endDate" hasValidationError "beforeStartDate"
        }

        val reason = cas3VoidBedspaceReasonRepository.findByIdOrNull(reasonId)
        if (reason == null) {
          "$.reason" hasValidationError "doesNotExist"
        }

        if (validationErrors.any()) {
          return@validated fieldValidationError
        }

        val updatedVoidBedspacesEntity = cas3VoidBedspacesRepository.save(
          voidBedspace.apply {
            this.startDate = startDate
            this.endDate = endDate
            this.reason = reason!!
            this.referenceNumber = referenceNumber
            this.notes = notes
          },
        )

        success(updatedVoidBedspacesEntity)
      },
    )
  }

  fun cancelVoidBedspace(
    voidBedspace: Cas3VoidBedspaceEntity,
    notes: String?,
  ) = validated<Cas3VoidBedspaceCancellationEntity> {
    if (voidBedspace.cancellation != null) {
      return generalError("This Void Bedspace already has a cancellation set")
    }

    val cancellationEntity = cas3VoidBedspaceCancellationRepository.save(
      Cas3VoidBedspaceCancellationEntity(
        id = UUID.randomUUID(),
        voidBedspace = voidBedspace,
        notes = notes,
        createdAt = OffsetDateTime.now(),
      ),
    )

    return success(cancellationEntity)
  }

  private fun tryGetProbationDeliveryUnit(
    probationDeliveryUnitIdentifier: Either<String, UUID>?,
    probationRegionId: UUID,
    onValidationError: (property: String, err: String) -> Unit,
  ): ProbationDeliveryUnitEntity? {
    val probationDeliveryUnit = when (probationDeliveryUnitIdentifier) {
      null -> {
        onValidationError("$.probationDeliveryUnitId", "empty")
        null
      }

      else -> probationDeliveryUnitIdentifier.fold(
        { name ->
          if (name.isBlank()) {
            onValidationError("$.pdu", "empty")
          }

          val result = probationDeliveryUnitRepository.findByNameAndProbationRegionId(name, probationRegionId)

          if (result == null) {
            onValidationError("$.pdu", "doesNotExist")
          }

          result
        },
        { id ->
          val result = probationDeliveryUnitRepository.findByIdAndProbationRegionId(id, probationRegionId)

          if (result == null) {
            onValidationError("$.probationDeliveryUnitId", "doesNotExist")
          }

          result
        },
      )
    }

    return probationDeliveryUnit
  }

  private fun getAndValidateCharacteristics(
    characteristicIds: List<UUID>,
    modelScopeTarget: Any,
    validationErrors: ValidationErrors,
  ): List<CharacteristicEntity?> = characteristicIds.mapIndexed { index, uuid ->
    val entity = characteristicService.getCharacteristic(uuid)

    if (entity == null) {
      validationErrors["$.characteristics[$index]"] = "doesNotExist"
    } else {
      if (!characteristicService.modelScopeMatches(entity, modelScopeTarget)) {
        validationErrors["$.characteristics[$index]"] = "incorrectCharacteristicModelScope"
      } else if (!characteristicService.serviceScopeMatches(entity, modelScopeTarget)) {
        validationErrors["$.characteristics[$index]"] = "incorrectCharacteristicServiceScope"
      }
    }

    entity
  }
}
