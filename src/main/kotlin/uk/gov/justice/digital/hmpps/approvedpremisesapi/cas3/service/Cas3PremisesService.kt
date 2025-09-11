package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceCancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceCancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceArchiveAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceArchiveActions
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesArchiveAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ValidationMessage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ValidationResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ValidationResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_PREMISES_ARCHIVED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType.CAS3_PREMISES_UNARCHIVED
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesTotalBedspacesByStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Availability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.CasResultValidatedScope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidatedScope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult.Cas3FieldValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilExclusiveEnd
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Suppress("TooManyFunctions", "LargeClass")
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
  private val domainEventRepository: DomainEventRepository,
  private val characteristicService: CharacteristicService,
  private val workingDayService: WorkingDayService,
  private val cas3DomainEventService: Cas3DomainEventService,
  private val objectMapper: ObjectMapper,
  private val clock: Clock,
) {

  companion object {
    const val MAX_LENGTH_BEDSPACE_REFERENCE: Long = 3
    const val MAX_DAYS_UNARCHIVE_PREMISES: Long = 7
    const val MAX_DAYS_ARCHIVE_PREMISES_IN_PAST: Long = 7
    const val MAX_MONTHS_ARCHIVE_PREMISES_IN_FUTURE: Long = 3
    const val MAX_DAYS_CREATE_BEDSPACE: Long = 7
    const val MAX_DAYS_UNARCHIVE_BEDSPACE: Long = 7
    const val MAX_DAYS_ARCHIVE_BEDSPACE_IN_PAST: Long = 7
    const val MAX_MONTHS_ARCHIVE_BEDSPACE_IN_FUTURE: Long = 3
  }

  fun getPremises(premisesId: UUID): TemporaryAccommodationPremisesEntity? = premisesRepository.findTemporaryAccommodationPremisesByIdOrNull(premisesId)

  fun getAllPremisesSummaries(regionId: UUID, postcodeOrAddress: String?, premisesStatus: Cas3PremisesStatus?): List<TemporaryAccommodationPremisesSummary> {
    val postcodeOrAddressWithoutWhitespace = postcodeOrAddress?.filter { !it.isWhitespace() }
    return premisesRepository.findAllCas3PremisesSummary(regionId, postcodeOrAddress, postcodeOrAddressWithoutWhitespace, premisesStatus?.transformStatus())
  }

  fun getBedspace(premisesId: UUID, bedspaceId: UUID): CasResult<BedEntity> = validatedCasResult {
    val bedspace = bedspaceRepository.findCas3Bedspace(premisesId, bedspaceId) ?: return CasResult.NotFound("Bedspace", bedspaceId.toString())

    return success(bedspace)
  }

  fun getPremisesBedspaces(premisesId: UUID): List<BedEntity> = bedspaceRepository.findByRoomPremisesId(premisesId)

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
      endDate = null,
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

  @Transactional
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
      endDate = null,
    )

    val characteristicEntities = getAndValidateCharacteristics(characteristicIds, premises, validationErrors)

    if (validationErrors.any()) {
      return fieldValidationError
    }

    premises.characteristics.addAll(characteristicEntities.map { it!! })
    premisesRepository.save(premises)

    return success(premises)
  }

  @Deprecated("This function is deprecated use updatePremises in the same class")
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

  @SuppressWarnings("CyclomaticComplexMethod")
  fun updatePremises(
    premises: TemporaryAccommodationPremisesEntity,
    reference: String,
    addressLine1: String,
    addressLine2: String?,
    town: String?,
    postcode: String,
    localAuthorityAreaId: UUID?,
    probationRegionId: UUID,
    characteristicIds: List<UUID>,
    notes: String?,
    probationDeliveryUnitId: UUID,
    turnaroundWorkingDays: Int,
  ): CasResult<TemporaryAccommodationPremisesEntity> = validatedCasResult {
    val localAuthorityArea = localAuthorityAreaId?.let { id ->
      localAuthorityAreaRepository.findByIdOrNull(id).also { result ->
        if (result == null) {
          "$.localAuthorityAreaId" hasValidationError "doesNotExist"
        }
      }
    }
    val probationRegion = probationRegionRepository.findByIdOrNull(probationRegionId)
    if (probationRegion == null) {
      "$.probationRegionId" hasValidationError "doesNotExist"
    }
    val probationDeliveryUnit = probationDeliveryUnitRepository.findByIdAndProbationRegionId(probationDeliveryUnitId, probationRegionId)
    if (probationDeliveryUnit == null) {
      "$.probationDeliveryUnitId" hasValidationError "doesNotExist"
    }

    if (reference.isEmpty()) {
      "$.reference" hasValidationError "empty"
    } else if (!premisesRepository.nameIsUniqueForType(reference, TemporaryAccommodationPremisesEntity::class.java, premises.id)) {
      "$.reference" hasValidationError "notUnique"
    }

    if (addressLine1.isEmpty()) {
      "$.address" hasValidationError "empty"
    }

    if (postcode.isEmpty()) {
      "$.postcode" hasValidationError "empty"
    }

    val characteristicEntities = getAndValidateCharacteristics(characteristicIds, premises, validationErrors)
    if (turnaroundWorkingDays < 0) {
      "$.turnaroundWorkingDays" hasValidationError "isNotAPositiveInteger"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    premises
      .apply {
        premises.name = reference
        premises.addressLine1 = addressLine1
        premises.addressLine2 = addressLine2
        premises.town = town
        premises.postcode = postcode
        premises.localAuthorityArea = localAuthorityArea
        premises.probationRegion = probationRegion!!
        premises.characteristics = characteristicEntities.map { it!! }.toMutableList()
        premises.notes = if (notes.isNullOrEmpty()) "" else notes
        premises.probationDeliveryUnit = probationDeliveryUnit!!
        premises.turnaroundWorkingDays = turnaroundWorkingDays
      }

    val savedPremises = premisesRepository.save(premises)

    return success(savedPremises)
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

  @SuppressWarnings("MagicNumber")
  fun createBedspace(
    premises: TemporaryAccommodationPremisesEntity,
    bedspaceReference: String,
    startDate: LocalDate,
    notes: String?,
    characteristicIds: List<UUID>,
  ): CasResult<BedEntity> = validatedCasResult {
    val trimmedReference = bedspaceReference.trim()

    if (isValidBedspaceReference(trimmedReference) &&
      premises.rooms.any { room -> room.name.equals(trimmedReference, ignoreCase = true) }
    ) {
      "$.reference" hasValidationError "bedspaceReferenceExists"
    }

    if (startDate.isBefore(LocalDate.now().minusDays(MAX_DAYS_CREATE_BEDSPACE))) {
      "$.startDate" hasValidationError "invalidStartDateInThePast"
    }

    if (startDate.isAfter(LocalDate.now().plusDays(MAX_DAYS_CREATE_BEDSPACE))) {
      "$.startDate" hasValidationError "invalidStartDateInTheFuture"
    }

    if (startDate.isBefore(premises.startDate)) {
      return Cas3FieldValidationError(
        mapOf(
          "$.startDate" to Cas3ValidationMessage(
            entityId = premises.id.toString(),
            message = "startDateBeforePremisesStartDate",
            value = premises.startDate.toString(),
          ),
        ),
      )
    }

    var room = RoomEntity(
      id = UUID.randomUUID(),
      name = trimmedReference,
      code = null,
      notes = notes,
      premises = premises,
      beds = mutableListOf(),
      characteristics = mutableListOf(),
    )

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

    if (premises.isPremisesScheduledToArchive()) {
      unarchivePremisesAndSaveDomainEvent(premises, startDate)
    }

    return success(bedspace)
  }

  fun updateBedspace(
    premises: TemporaryAccommodationPremisesEntity,
    bedspaceId: UUID,
    bedspaceReference: String,
    notes: String?,
    characteristicIds: List<UUID>,
  ): CasResult<BedEntity> = validatedCasResult {
    val bedspace = bedspaceRepository.findCas3Bedspace(premises.id, bedspaceId) ?: return CasResult.NotFound("Bedspace", bedspaceId.toString())

    val room = bedspace.room
    val trimmedReference = bedspaceReference.trim()

    if (isValidBedspaceReference(trimmedReference) &&
      premises.rooms.any { existingRoom -> existingRoom.id != room.id && existingRoom.name.equals(trimmedReference, ignoreCase = true) }
    ) {
      "$.reference" hasValidationError "bedspaceReferenceExists"
    }

    val characteristicEntities = getAndValidateCharacteristics(characteristicIds, room, validationErrors)

    if (validationErrors.any()) {
      return fieldValidationError
    }

    room.name = trimmedReference
    room.notes = notes
    room.characteristics = characteristicEntities.map { it!! }.toMutableList()

    val updatedRoom = roomRepository.save(room)

    return success(updatedRoom.beds.first())
  }

  fun canArchivePremisesInFuture(premisesId: UUID): Cas3ValidationResults {
    val maximumPremisesArchiveDate = LocalDate.now(clock).plusMonths(MAX_MONTHS_ARCHIVE_PREMISES_IN_FUTURE)
    var affectedBedspaces = mutableListOf<Cas3ValidationResult>()

    val overlapBookings = bookingRepository.findActiveOverlappingBookingByPremisesId(premisesId, LocalDate.now(clock))

    overlapBookings.map {
      val bookingTurnaround = workingDayService.addWorkingDays(it.departureDate, it.turnaround?.workingDayCount ?: 0)
      if (bookingTurnaround >= maximumPremisesArchiveDate) {
        affectedBedspaces.add(
          Cas3ValidationResult(
            entityId = it.bed!!.id,
            entityReference = it.bed!!.room.name,
            date = bookingTurnaround,
          ),
        )
      }
    }

    val overlappingVoids = cas3VoidBedspacesRepository.findOverlappingBedspaceEndDateByPremisesId(premisesId, maximumPremisesArchiveDate)

    overlappingVoids.map {
      affectedBedspaces.add(
        Cas3ValidationResult(
          entityId = it.bed!!.id,
          entityReference = it.bed!!.room.name,
          date = it.endDate,
        ),
      )
    }

    affectedBedspaces = affectedBedspaces
      .groupBy { it.entityId }
      .mapValues { it.value.sortedByDescending { it.date }.take(1) }
      .map { it.value.first() }
      .toMutableList()

    return Cas3ValidationResults(
      items = affectedBedspaces,
    )
  }

  @Suppress("CyclomaticComplexMethod")
  @Transactional
  fun archivePremises(
    premises: TemporaryAccommodationPremisesEntity,
    endDate: LocalDate,
  ): CasResult<TemporaryAccommodationPremisesEntity> = validatedCasResult {
    if (endDate.isBefore(LocalDate.now().minusDays(MAX_DAYS_ARCHIVE_PREMISES_IN_PAST))) {
      return "$.endDate" hasSingleValidationError "invalidEndDateInThePast"
    }

    if (endDate.isAfter(LocalDate.now().plusMonths(MAX_MONTHS_ARCHIVE_PREMISES_IN_FUTURE))) {
      return "$.endDate" hasSingleValidationError "invalidEndDateInTheFuture"
    }

    if (endDate.isBefore(premises.startDate)) {
      return Cas3FieldValidationError(
        mapOf(
          "$.endDate" to Cas3ValidationMessage(
            entityId = premises.id.toString(),
            message = "endDateBeforePremisesStartDate",
            value = premises.startDate.toString(),
          ),
        ),
      )
    }

    cas3DomainEventService.getPremisesActiveDomainEvents(premises.id, listOf(CAS3_PREMISES_ARCHIVED))
      .sortedByDescending { it.createdAt }
      .asSequence()
      .map { objectMapper.readValue(it.data, CAS3PremisesArchiveEvent::class.java).eventDetails.endDate }
      .firstOrNull { it >= endDate }
      ?.let { archiveDate ->
        return Cas3FieldValidationError(
          mapOf(
            "$.endDate" to Cas3ValidationMessage(
              entityId = premises.id.toString(),
              message = "endDateOverlapPreviousPremisesArchiveEndDate",
              value = archiveDate.toString(),
            ),
          ),
        )
      }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    val activeBedspaces = bedspaceRepository.findByRoomPremisesId(premises.id)
      .asSequence()
      .filter { (it.isCas3BedspaceOnline() || isCas3BedspaceUpcoming(it)) && (it.endDate == null || it.endDate!! > endDate) }

    if (activeBedspaces.any()) {
      val lastUpcomingBedspace = activeBedspaces.maxByOrNull { it.startDate!! }

      if (lastUpcomingBedspace != null && lastUpcomingBedspace.startDate!! > endDate) {
        return Cas3FieldValidationError(
          mapOf(
            "$.endDate" to Cas3ValidationMessage(
              entityId = lastUpcomingBedspace.id.toString(),
              message = "existingUpcomingBedspace",
              value = lastUpcomingBedspace.startDate!!.plusDays(1).toString(),
            ),
          ),
        )
      }

      canArchivePremisesBedspaces(premises.id, endDate)?.let {
        return Cas3FieldValidationError(it.validationMessages.entries.associate { entry -> entry.key to entry.value })
      }
    }

    // archive premises
    val archivedPremises = archivePremisesAndSaveDomainEvent(premises, endDate)

    if (activeBedspaces.any()) {
      // archive all online bedspaces
      activeBedspaces.forEach { bedspace ->
        archiveBedspaceAndSaveDomainEvent(bedspace, endDate)
      }
    }

    return success(archivedPremises)
  }

  fun canArchiveBedspaceInFuture(premisesId: UUID, bedspaceId: UUID): CasResult<Cas3ValidationResult?> {
    val bedEntity = bedspaceRepository.findCas3Bedspace(premisesId, bedspaceId) ?: return CasResult.NotFound("Bedspace", bedspaceId.toString())

    val threeMonthsFromToday = LocalDate.now(clock).plusMonths(MAX_MONTHS_ARCHIVE_PREMISES_IN_FUTURE)
    val blockingArchiveDates = mutableListOf<LocalDate>()

    val overlapBookings = bookingRepository.findActiveOverlappingBookingByBed(bedspaceId, LocalDate.now(clock))

    overlapBookings.map {
      val bookingTurnaround = workingDayService.addWorkingDays(it.departureDate, it.turnaround?.workingDayCount ?: 0)
      if (bookingTurnaround >= threeMonthsFromToday) {
        blockingArchiveDates += bookingTurnaround
      }
    }

    val overlappingVoid = cas3VoidBedspacesRepository.findOverlappingBedspaceEndDate(
      bedspaceId,
      threeMonthsFromToday,
    ).maxByOrNull { it.endDate }

    if (overlappingVoid != null) {
      blockingArchiveDates += overlappingVoid.endDate
    }

    return if (blockingArchiveDates.isEmpty()) {
      CasResult.Success(null)
    } else {
      CasResult.Success(Cas3ValidationResult(bedspaceId, bedEntity.room.name, blockingArchiveDates.max()))
    }
  }

  @Transactional
  fun archiveBedspace(
    bedspaceId: UUID,
    premises: TemporaryAccommodationPremisesEntity,
    endDate: LocalDate,
  ): CasResult<BedEntity> = validatedCasResult {
    val bedspace = bedspaceRepository.findByIdOrNull(bedspaceId)
      ?: return CasResult.NotFound("Bedspace", bedspaceId.toString())

    if (endDate.isBefore(LocalDate.now(clock).minusDays(MAX_DAYS_ARCHIVE_BEDSPACE_IN_PAST))) {
      return "$.endDate" hasSingleValidationError "invalidEndDateInThePast"
    }

    if (endDate.isAfter(LocalDate.now(clock).plusMonths(MAX_MONTHS_ARCHIVE_BEDSPACE_IN_FUTURE))) {
      return "$.endDate" hasSingleValidationError "invalidEndDateInTheFuture"
    }

    if (endDate.isBefore(bedspace.startDate)) {
      return Cas3FieldValidationError(
        mapOf(
          "$.endDate" to Cas3ValidationMessage(
            entityId = bedspace.id.toString(),
            message = "endDateBeforeBedspaceStartDate",
            value = bedspace.startDate.toString(),
          ),
        ),
      )
    }

    cas3DomainEventService.getBedspaceActiveDomainEvents(bedspace.id, listOf(DomainEventType.CAS3_BEDSPACE_ARCHIVED))
      .sortedByDescending { it.createdAt }
      .asSequence()
      .map { objectMapper.readValue(it.data, CAS3BedspaceArchiveEvent::class.java).eventDetails.endDate }
      .firstOrNull { it >= endDate }
      ?.let { archiveDate ->
        return Cas3FieldValidationError(
          mapOf(
            "$.endDate" to Cas3ValidationMessage(
              entityId = bedspace.id.toString(),
              message = "endDateOverlapPreviousBedspaceArchiveEndDate",
              value = archiveDate.toString(),
            ),
          ),
        )
      }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    canArchiveBedspace(bedspaceId, endDate)?.let { return it }

    val updatedBedspace = archiveBedspaceAndSaveDomainEvent(bedspace, endDate)

    archivePremisesIfAllBedspacesArchived(premises)

    return success(updatedBedspace)
  }

  fun unarchivePremises(
    premises: TemporaryAccommodationPremisesEntity,
    restartDate: LocalDate,
  ): CasResult<TemporaryAccommodationPremisesEntity> = validatedCasResult {
    if (!premises.isPremisesArchived()) {
      return@validatedCasResult "$.premisesId" hasSingleValidationError "premisesNotArchived"
    }

    val today = LocalDate.now()

    if (restartDate.isBefore(today.minusDays(MAX_DAYS_UNARCHIVE_PREMISES))) {
      "$.restartDate" hasValidationError "invalidRestartDateInThePast"
    }

    if (restartDate.isAfter(today.plusDays(MAX_DAYS_UNARCHIVE_PREMISES))) {
      "$.restartDate" hasValidationError "invalidRestartDateInTheFuture"
    }

    if (restartDate.isBefore(premises.endDate)) {
      "$.restartDate" hasValidationError "beforeLastPremisesArchivedDate"
    }

    if (hasErrors()) {
      return@validatedCasResult errors()
    }

    val unarchivePremises = unarchivePremisesAndSaveDomainEvent(premises, restartDate)

    val bedspaces = bedspaceRepository.findByRoomPremisesId(premises.id)
    val uniqueBedspaces = bedspaces.groupBy { b -> b.room.name }
    uniqueBedspaces.forEach { bedspaces ->
      val lastBedspace = bedspaces.value.sortedByDescending { it.createdAt }.first()
      unarchiveBedspaceAndSaveDomainEvent(lastBedspace, restartDate)
    }

    success(unarchivePremises)
  }

  fun unarchiveBedspace(
    premises: TemporaryAccommodationPremisesEntity,
    bedspaceId: UUID,
    restartDate: LocalDate,
  ): CasResult<BedEntity> = validatedCasResult {
    val bedspace = bedspaceRepository.findCas3Bedspace(premises.id, bedspaceId)
      ?: return@validatedCasResult "$.bedspaceId" hasSingleValidationError "doesNotExist"

    if (!isCas3BedspaceArchived(bedspace)) {
      return@validatedCasResult "$.bedspaceId" hasSingleValidationError "bedspaceNotArchived"
    }

    val today = LocalDate.now()

    if (restartDate.isBefore(today.minusDays(MAX_DAYS_UNARCHIVE_BEDSPACE))) {
      "$.restartDate" hasValidationError "invalidRestartDateInThePast"
    }

    if (restartDate.isAfter(today.plusDays(MAX_DAYS_UNARCHIVE_BEDSPACE))) {
      "$.restartDate" hasValidationError "invalidRestartDateInTheFuture"
    }

    if (restartDate.isBefore(bedspace.endDate)) {
      "$.restartDate" hasValidationError "beforeLastBedspaceArchivedDate"
    }

    if (hasErrors()) {
      return@validatedCasResult errors()
    }

    val unarchivedBedspace = unarchiveBedspaceAndSaveDomainEvent(bedspace, restartDate)

    if (premises.status == PropertyStatus.archived) {
      unarchivePremisesAndSaveDomainEvent(premises, restartDate)
    }

    success(unarchivedBedspace)
  }

  @Transactional
  fun cancelScheduledArchivePremises(
    premisesId: UUID,
  ): CasResult<TemporaryAccommodationPremisesEntity> = validatedCasResult {
    val premises = premisesRepository.findTemporaryAccommodationPremisesByIdOrNull(premisesId)
      ?: return CasResult.NotFound("Premises", premisesId.toString())

    if (premises.endDate == null) {
      return@validatedCasResult "$.premisesId" hasSingleValidationError "premisesNotScheduledToArchive"
    }

    if (premises.endDate!! <= LocalDate.now(clock)) {
      return@validatedCasResult "$.premisesId" hasSingleValidationError "premisesAlreadyArchived"
    }

    val latestPremisesArchiveDomainEvent = domainEventRepository.findFirstByCas3PremisesIdAndTypeOrderByCreatedAtDesc(
      premisesId,
      CAS3_PREMISES_ARCHIVED,
    ) ?: return@validatedCasResult "$.premisesId" hasSingleValidationError "premisesNotScheduledToArchive"

    val premisesArchiveDomainEventData = objectMapper.readValue(latestPremisesArchiveDomainEvent.data, CAS3PremisesArchiveEvent::class.java)

    if (premisesArchiveDomainEventData.eventDetails.endDate <= LocalDate.now(clock)) {
      return@validatedCasResult "$.premisesId" hasSingleValidationError "premisesArchiveDateInThePast"
    }

    val bedspaces = bedspaceRepository.findByRoomPremisesId(premises.id)
    bedspaces.forEach { bedspace ->
      val latestBedspaceArchiveDomainEvent = domainEventRepository.findFirstByCas3BedspaceIdAndTypeOrderByCreatedAtDesc(
        bedspace.id,
        DomainEventType.CAS3_BEDSPACE_ARCHIVED,
      )

      if (latestBedspaceArchiveDomainEvent != null && latestBedspaceArchiveDomainEvent.createdAt >= latestPremisesArchiveDomainEvent.createdAt) {
        val bedspaceArchiveDomainEventData = objectMapper.readValue(latestBedspaceArchiveDomainEvent.data, CAS3BedspaceArchiveEvent::class.java)

        bedspaceRepository.save(
          bedspace.copy(
            endDate = bedspaceArchiveDomainEventData.eventDetails.currentEndDate,
          ),
        )

        domainEventRepository.save(
          latestBedspaceArchiveDomainEvent.copy(
            cas3CancelledAt = OffsetDateTime.now(clock),
          ),
        )
      }
    }

    domainEventRepository.save(
      latestPremisesArchiveDomainEvent.copy(
        cas3CancelledAt = OffsetDateTime.now(clock),
      ),
    )

    premises.endDate = null
    premises.status = PropertyStatus.active

    return success(premisesRepository.save(premises))
  }

  @Transactional
  fun cancelScheduledArchiveBedspace(
    premises: TemporaryAccommodationPremisesEntity,
    bedspaceId: UUID,
  ): CasResult<BedEntity> = validatedCasResult {
    val bedspace = bedspaceRepository.findCas3Bedspace(premises.id, bedspaceId) ?: return@validatedCasResult "$.bedspaceId" hasSingleValidationError "doesNotExist"

    // Check if bedspace not scheduled to archive
    if (bedspace.endDate == null) {
      return@validatedCasResult "$.bedspaceId" hasSingleValidationError "bedspaceNotScheduledToArchive"
    }

    // Check if bedspace is already archived
    if (isCas3BedspaceArchived(bedspace)) {
      return@validatedCasResult "$.bedspaceId" hasSingleValidationError "bedspaceAlreadyArchived"
    }

    val latestBedspaceArchiveDomainEvent = domainEventRepository.findFirstByCas3BedspaceIdAndTypeOrderByCreatedAtDesc(
      bedspace.id,
      DomainEventType.CAS3_BEDSPACE_ARCHIVED,
    ) ?: return@validatedCasResult "$.premisesId" hasSingleValidationError "bedspaceNotScheduledToArchive"

    if (premises.endDate != null && premises.endDate!!.isAfter(LocalDate.now())) {
      domainEventRepository.findFirstByCas3PremisesIdAndTypeOrderByCreatedAtDesc(
        premises.id,
        CAS3_PREMISES_ARCHIVED,
      )?.let {
        // Premises scheduled to archive, cancel scheduled premises and bedspaces set to archive
        val result = cancelScheduledArchivePremises(premises.id)
        if (result is CasResult.FieldValidationError) {
          return Cas3FieldValidationError(result.validationMessages as Map<String, Cas3ValidationMessage>)
        }

        return success(bedspace)
      }
    }

    domainEventRepository.save(
      latestBedspaceArchiveDomainEvent.copy(
        cas3CancelledAt = OffsetDateTime.now(clock),
      ),
    )

    val bedspaceArchiveDomainEventData = objectMapper.readValue(latestBedspaceArchiveDomainEvent.data, CAS3BedspaceArchiveEvent::class.java)

    // Update the bedspace to cancel a scheduled archive
    val updatedBedspace = bedspaceRepository.save(
      bedspace.copy(
        endDate = bedspaceArchiveDomainEventData.eventDetails.currentEndDate,
      ),
    )

    success(updatedBedspace)
  }

  @Transactional
  fun cancelScheduledUnarchivePremises(
    premisesId: UUID,
  ): CasResult<TemporaryAccommodationPremisesEntity> = validatedCasResult {
    val premises = premisesRepository.findTemporaryAccommodationPremisesByIdOrNull(premisesId)
      ?: return CasResult.NotFound("Premises", premisesId.toString())

    if (!premises.isPremisesArchived() && premises.startDate <= LocalDate.now(clock)) {
      return@validatedCasResult "$.premisesId" hasSingleValidationError "premisesAlreadyOnline"
    }

    val latestUnarchivePremisesDomainEvent = domainEventRepository.findFirstByCas3PremisesIdAndTypeOrderByCreatedAtDesc(
      premisesId,
      CAS3_PREMISES_UNARCHIVED,
    ) ?: return@validatedCasResult "$.premisesId" hasSingleValidationError "premisesNotScheduledToUnarchive"

    val unarchivePremisesDomainEventDetails = objectMapper.readValue(latestUnarchivePremisesDomainEvent.data, CAS3PremisesUnarchiveEvent::class.java).eventDetails

    if (unarchivePremisesDomainEventDetails.newStartDate <= LocalDate.now(clock)) {
      return@validatedCasResult "$.premisesId" hasSingleValidationError "premisesUnarchiveDateInThePast"
    }

    domainEventRepository.save(
      latestUnarchivePremisesDomainEvent.copy(
        cas3CancelledAt = OffsetDateTime.now(clock),
      ),
    )

    premises.startDate = unarchivePremisesDomainEventDetails.currentStartDate
    premises.endDate = unarchivePremisesDomainEventDetails.currentEndDate
    premises.status = PropertyStatus.archived

    val updatedPremises = premisesRepository.save(premises)

    val bedspaces = bedspaceRepository.findByRoomPremisesId(premises.id)
    bedspaces.forEach { bedspace ->
      val latestBedspaceUnarchiveDomainEvent = domainEventRepository.findFirstByCas3BedspaceIdAndTypeOrderByCreatedAtDesc(
        bedspace.id,
        DomainEventType.CAS3_BEDSPACE_UNARCHIVED,
      )

      if (latestBedspaceUnarchiveDomainEvent != null) {
        domainEventRepository.save(
          latestBedspaceUnarchiveDomainEvent.copy(
            cas3CancelledAt = OffsetDateTime.now(clock),
          ),
        )

        val bedspaceUnarchiveEventDetails = objectMapper.readValue(latestBedspaceUnarchiveDomainEvent.data, CAS3BedspaceUnarchiveEvent::class.java).eventDetails
        val previousBedspaceStartDate = bedspaceUnarchiveEventDetails.currentStartDate
        val previousBedspaceEndDate = bedspaceUnarchiveEventDetails.currentEndDate

        bedspaceRepository.save(
          bedspace.copy(
            startDate = previousBedspaceStartDate,
            endDate = previousBedspaceEndDate,
          ),
        )
      }
    }

    success(updatedPremises)
  }

  @Transactional
  fun cancelScheduledUnarchiveBedspace(
    bedspaceId: UUID,
  ): CasResult<BedEntity> = validatedCasResult {
    val bedspace = bedspaceRepository.findByIdOrNull(bedspaceId)
      ?: return@validatedCasResult "$.bedspaceId" hasSingleValidationError "doesNotExist"

    if (bedspace.isCas3BedspaceOnline()) {
      return@validatedCasResult "$.bedspaceId" hasSingleValidationError "bedspaceAlreadyOnline"
    }

    val latestBedspaceUnarchiveDomainEvent = domainEventRepository.findFirstByCas3BedspaceIdAndTypeOrderByCreatedAtDesc(
      bedspace.id,
      DomainEventType.CAS3_BEDSPACE_UNARCHIVED,
    ) ?: return@validatedCasResult "$.bedspaceId" hasSingleValidationError "bedspaceNotScheduledToUnarchive"

    domainEventRepository.save(
      latestBedspaceUnarchiveDomainEvent.copy(
        cas3CancelledAt = OffsetDateTime.now(clock),
      ),
    )

    val eventDetails = objectMapper.readValue(latestBedspaceUnarchiveDomainEvent.data, CAS3BedspaceUnarchiveEvent::class.java).eventDetails

    val updatedBedspace = bedspaceRepository.save(
      bedspace.copy(
        startDate = eventDetails.currentStartDate,
        endDate = eventDetails.currentEndDate,
      ),
    )

    success(updatedBedspace)
  }

  fun getPremisesArchiveHistory(premisesEntity: TemporaryAccommodationPremisesEntity): CasResult<List<Cas3PremisesArchiveAction>> = validatedCasResult {
    val archiveHistory = cas3DomainEventService.getPremisesActiveDomainEvents(
      premisesEntity.id,
      listOf(CAS3_PREMISES_ARCHIVED, CAS3_PREMISES_UNARCHIVED),
    )
      .mapNotNull { domainEventEntity ->
        when (domainEventEntity.type) {
          CAS3_PREMISES_UNARCHIVED -> {
            val newStartDate = objectMapper.readValue(domainEventEntity.data, CAS3PremisesUnarchiveEvent::class.java).eventDetails.newStartDate
            if (newStartDate <= LocalDate.now()) {
              Cas3PremisesArchiveAction(
                status = Cas3PremisesStatus.online,
                date = newStartDate,
              )
            } else {
              null
            }
          }

          CAS3_PREMISES_ARCHIVED -> {
            val endDate =
              objectMapper.readValue(domainEventEntity.data, CAS3PremisesArchiveEvent::class.java).eventDetails.endDate
            if (endDate <= LocalDate.now()) {
              Cas3PremisesArchiveAction(
                status = Cas3PremisesStatus.archived,
                date = endDate,
              )
            } else {
              null
            }
          }

          else -> return CasResult.GeneralValidationError("Incorrect domain event type for archive history: ${domainEventEntity.type}, ${domainEventEntity.id}")
        }
      }
      .sortedBy { it.date }

    return CasResult.Success(archiveHistory)
  }

  fun getBedspaceArchiveHistory(bedspaceId: UUID): CasResult<List<Cas3BedspaceArchiveAction>> = validatedCasResult {
    val domainEvents = cas3DomainEventService.getBedspaceActiveDomainEvents(
      bedspaceId,
      listOf(DomainEventType.CAS3_BEDSPACE_ARCHIVED, DomainEventType.CAS3_BEDSPACE_UNARCHIVED),
    )
    return when {
      domainEvents.any() -> getBedspaceArchiveActions(domainEvents)
      else -> success(emptyList())
    }
  }

  fun getBedspacesArchiveHistory(bedspaceIds: List<UUID>): List<Cas3BedspaceArchiveActions> {
    val domainEvents = cas3DomainEventService.getBedspacesActiveDomainEvents(
      bedspaceIds,
      listOf(DomainEventType.CAS3_BEDSPACE_ARCHIVED, DomainEventType.CAS3_BEDSPACE_UNARCHIVED),
    )
    return bedspaceIds.map { bedspaceId ->
      val bedspaceDomainEvents = domainEvents.filter { it.cas3BedspaceId == bedspaceId }
      val actions = extractEntityFromCasResult(getBedspaceArchiveActions(bedspaceDomainEvents))
      Cas3BedspaceArchiveActions(bedspaceId, actions)
    }
  }

  fun getBedspaceTotals(premises: TemporaryAccommodationPremisesEntity): CasResult.Success<TemporaryAccommodationPremisesTotalBedspacesByStatus> {
    val bedspaces = bedspaceRepository.findByRoomPremisesId(premises.id)

    return CasResult.Success(
      TemporaryAccommodationPremisesTotalBedspacesByStatus(
        premisesId = premises.id,
        bedspaces.count { it.isCas3BedspaceOnline() },
        bedspaces.count { isCas3BedspaceUpcoming(it) },
        bedspaces.count { isCas3BedspaceArchived(it) },
      ),
    )
  }

  fun getBedspaceCount(premises: PremisesEntity): Int = premisesRepository.getBedCount(premises)

  fun getBedspaceStatus(bedspace: BedEntity) = when {
    isCas3BedspaceUpcoming(bedspace) -> Cas3BedspaceStatus.upcoming
    isCas3BedspaceArchived(bedspace) -> Cas3BedspaceStatus.archived
    else -> Cas3BedspaceStatus.online
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
    } else {
      validateVoidAndBedDates(startDate, endDate, bed)
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

        val bed = voidBedspace.bed

        if (bed == null) {
          "$.bed" hasValidationError "doesNotExist"
        } else {
          validateVoidAndBedDates(startDate, endDate, bed)
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

  @SuppressWarnings("TooGenericExceptionThrown")
  fun getAvailabilityForRange(
    premises: TemporaryAccommodationPremisesEntity,
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

  private fun CasResultValidatedScope<BedEntity>.isValidBedspaceReference(
    trimmedReference: String,
  ): Boolean {
    if (trimmedReference.isEmpty()) {
      "$.reference" hasValidationError "empty"
    } else {
      if (trimmedReference.length < MAX_LENGTH_BEDSPACE_REFERENCE) {
        "$.reference" hasValidationError "bedspaceReferenceNotMeetMinimumLength"
      }

      if (!trimmedReference.any { it.isLetterOrDigit() }) {
        "$.reference" hasValidationError "bedspaceReferenceMustIncludeLetterOrNumber"
      }
    }
    return !validationErrors.any()
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

  private fun ValidatedScope<Cas3VoidBedspaceEntity>.validateVoidAndBedDates(
    voidStartDate: LocalDate,
    voidEndDate: LocalDate,
    bed: BedEntity,
  ) {
    if (bed.endDate != null && voidStartDate.isAfter(bed.endDate)) {
      "$.startDate" hasValidationError "voidStartDateAfterBedspaceEndDate"
    }
    if (bed.startDate != null && voidStartDate.isBefore(bed.startDate)) {
      "$.startDate" hasValidationError "voidStartDateBeforeBedspaceStartDate"
    }
    if (bed.endDate != null && voidEndDate.isAfter(bed.endDate)) {
      "$.endDate" hasValidationError "voidEndDateAfterBedspaceEndDate"
    }
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

  private fun canArchiveBedspace(bedspaceId: UUID, endDate: LocalDate) = canArchiveBedspace(filterByPremisesId = null, filterByBedspaceId = bedspaceId, endDate = endDate)

  private fun canArchivePremisesBedspaces(premisesId: UUID, endDate: LocalDate) = canArchiveBedspace(filterByPremisesId = premisesId, filterByBedspaceId = null, endDate = endDate)

  @SuppressWarnings("CyclomaticComplexMethod")
  private fun canArchiveBedspace(filterByPremisesId: UUID?, filterByBedspaceId: UUID?, endDate: LocalDate): Cas3FieldValidationError<BedEntity>? {
    val allBookings = when {
      filterByPremisesId != null -> bookingRepository.findActiveOverlappingBookingByPremisesId(filterByPremisesId, LocalDate.now(clock)).sortedByDescending { it.departureDate }
      filterByBedspaceId != null -> bookingRepository.findActiveOverlappingBookingByBed(filterByBedspaceId, LocalDate.now(clock)).sortedByDescending { it.departureDate }
      else -> emptyList()
    }
    val overlapBookings = allBookings.filter { it.departureDate > endDate }
    val lastOverlapVoid = when {
      filterByPremisesId != null -> cas3VoidBedspacesRepository.findOverlappingBedspaceEndDateByPremisesId(filterByPremisesId, endDate).maxByOrNull { it.endDate }
      filterByBedspaceId != null -> cas3VoidBedspacesRepository.findOverlappingBedspaceEndDate(filterByBedspaceId, endDate).maxByOrNull { it.endDate }
      else -> null
    }

    val lastBookingsTurnaroundDate = allBookings
      .asSequence()
      .mapNotNull { booking ->
        val turnaroundDate = workingDayService.addWorkingDays(booking.departureDate, booking.turnaround?.workingDayCount ?: 0)
        if (turnaroundDate > endDate) {
          booking.id to turnaroundDate
        } else {
          null
        }
      }
      .toMap()

    val (lastTurnaroundBookingId, lastTurnaroundDate) = lastBookingsTurnaroundDate.entries.maxByOrNull { it.value }?.let { it.key to it.value } ?: Pair(null, null)

    return when {
      isVoidLastOverlapBedspaceArchiveDate(lastOverlapVoid, lastTurnaroundDate) -> {
        Cas3FieldValidationError(
          mapOf(
            "$.endDate" to Cas3ValidationMessage(
              entityId = lastOverlapVoid?.bed?.id.toString(),
              message = "existingVoid",
              value = lastOverlapVoid?.endDate?.plusDays(1).toString(),
            ),
          ),
        )
      }
      lastTurnaroundDate != null -> {
        val lastOverlapBooking = getLastBookingOverlapBedspaceArchiveDate(overlapBookings, lastBookingsTurnaroundDate, lastTurnaroundDate)
        if (lastOverlapBooking != null && lastOverlapBooking.departureDate == lastTurnaroundDate) {
          Cas3FieldValidationError(
            mapOf(
              "$.endDate" to Cas3ValidationMessage(
                entityId = lastOverlapBooking.bed?.id.toString(),
                message = "existingBookings",
                value = lastOverlapBooking.departureDate.plusDays(1).toString(),
              ),
            ),
          )
        } else {
          Cas3FieldValidationError(
            mapOf(
              "$.endDate" to Cas3ValidationMessage(
                entityId = allBookings.firstOrNull { it.id == lastTurnaroundBookingId }?.bed?.id.toString(),
                message = "existingTurnaround",
                value = lastTurnaroundDate.plusDays(1).toString(),
              ),
            ),
          )
        }
      }
      else -> null
    }
  }

  private fun archivePremisesIfAllBedspacesArchived(premises: TemporaryAccommodationPremisesEntity) {
    val bedspaces = bedspaceRepository.findByRoomPremisesId(premises.id)
    val lastBedspaceEndDate = bedspaces
      .asSequence()
      .mapNotNull { it.endDate }
      .maxOrNull()
    if (lastBedspaceEndDate != null && bedspaces.all { it.endDate != null }) {
      archivePremisesAndSaveDomainEvent(premises, lastBedspaceEndDate)
    }
  }

  private fun archivePremisesAndSaveDomainEvent(premises: TemporaryAccommodationPremisesEntity, endDate: LocalDate): TemporaryAccommodationPremisesEntity {
    premises.endDate = endDate
    premises.status = PropertyStatus.archived
    val updatedPremises = premisesRepository.save(premises)
    cas3DomainEventService.savePremisesArchiveEvent(premises, endDate)
    return updatedPremises
  }

  private fun archiveBedspaceAndSaveDomainEvent(bedspace: BedEntity, endDate: LocalDate): BedEntity {
    val originalEndDate = bedspace.endDate
    bedspace.endDate = endDate
    val updatedBedspace = bedspaceRepository.save(bedspace)
    cas3DomainEventService.saveBedspaceArchiveEvent(updatedBedspace, originalEndDate)
    return updatedBedspace
  }

  private fun unarchivePremisesAndSaveDomainEvent(premises: TemporaryAccommodationPremisesEntity, restartDate: LocalDate): TemporaryAccommodationPremisesEntity {
    val currentStartDate = premises.startDate
    val currentEndDate = premises.endDate!!
    premises.startDate = restartDate
    premises.endDate = null
    premises.status = PropertyStatus.active
    val updatedPremises = premisesRepository.save(premises)
    cas3DomainEventService.savePremisesUnarchiveEvent(premises, currentStartDate, restartDate, currentEndDate)
    return updatedPremises
  }

  private fun unarchiveBedspaceAndSaveDomainEvent(bedspace: BedEntity, restartDate: LocalDate): BedEntity {
    val originalStartDate = bedspace.startDate!!
    val originalEndDate = bedspace.endDate!!

    val updatedBedspace = bedspaceRepository.save(
      bedspace.copy(
        startDate = restartDate,
        endDate = null,
      ),
    )

    cas3DomainEventService.saveBedspaceUnarchiveEvent(updatedBedspace, originalStartDate, originalEndDate)
    return updatedBedspace
  }

  private fun isVoidLastOverlapBedspaceArchiveDate(
    lastOverlapVoid: Cas3VoidBedspaceEntity?,
    lastTurnaroundDate: LocalDate?,
  ) = lastOverlapVoid != null && (lastTurnaroundDate == null || (lastTurnaroundDate < lastOverlapVoid.endDate))

  private fun getLastBookingOverlapBedspaceArchiveDate(
    overlapBookings: List<BookingEntity>,
    lastBookingsTurnaroundDate: Map<UUID, LocalDate>,
    lastTurnaroundDate: LocalDate?,
  ): BookingEntity? {
    val lastOverlapBookingId = lastBookingsTurnaroundDate.entries.firstOrNull { it.value == lastTurnaroundDate }?.key
    return overlapBookings.firstOrNull { it.id == lastOverlapBookingId }
  }

  private fun getBedspaceArchiveActions(domainEvents: List<DomainEventEntity>): CasResult<List<Cas3BedspaceArchiveAction>> {
    val today = LocalDate.now()

    val archiveHistory = domainEvents
      .mapNotNull { domainEventEntity ->
        when (domainEventEntity.type) {
          DomainEventType.CAS3_BEDSPACE_UNARCHIVED -> {
            val eventDetails =
              objectMapper.readValue(domainEventEntity.data, CAS3BedspaceUnarchiveEvent::class.java).eventDetails
            val restartDate = eventDetails.newStartDate
            if (restartDate <= today) {
              Cas3BedspaceArchiveAction(
                status = Cas3BedspaceStatus.online,
                date = restartDate,
              )
            } else {
              null
            }
          }

          DomainEventType.CAS3_BEDSPACE_ARCHIVED -> {
            val eventDetails =
              objectMapper.readValue(domainEventEntity.data, CAS3BedspaceArchiveEvent::class.java).eventDetails
            val endDate = eventDetails.endDate
            if (endDate <= today) {
              Cas3BedspaceArchiveAction(
                status = Cas3BedspaceStatus.archived,
                date = endDate,
              )
            } else {
              null
            }
          }

          else -> return CasResult.GeneralValidationError("Incorrect domain event type for archive history: ${domainEventEntity.type}, ${domainEventEntity.id}")
        }
      }.sortedBy { it.date }

    return CasResult.Success(archiveHistory)
  }

  private fun Cas3PremisesStatus.transformStatus() = when (this) {
    Cas3PremisesStatus.archived -> PropertyStatus.archived.toString()
    Cas3PremisesStatus.online -> PropertyStatus.active.toString()
  }

  private fun isCas3BedspaceArchived(bedspace: BedEntity) = (bedspace.endDate != null && bedspace.endDate!! <= LocalDate.now())

  private fun isCas3BedspaceUpcoming(bedspace: BedEntity) = (bedspace.startDate?.isAfter(LocalDate.now()) ?: false)
}
