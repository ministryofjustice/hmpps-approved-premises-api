package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import arrow.core.Either
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Availability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilExclusiveEnd
import java.time.LocalDate
import java.util.UUID

@Service
class PremisesService(
  private val premisesRepository: PremisesRepository,
  private val cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository,
  private val bookingRepository: BookingRepository,
  private val localAuthorityAreaRepository: LocalAuthorityAreaRepository,
  private val probationRegionRepository: ProbationRegionRepository,
  private val probationDeliveryUnitRepository: ProbationDeliveryUnitRepository,
  private val characteristicService: CharacteristicService,
  private val roomRepository: RoomRepository,
  private val bedRepository: BedRepository,
) {
  fun getPremises(premisesId: UUID): PremisesEntity? = premisesRepository.findByIdOrNull(premisesId)

  fun getAvailabilityForRange(
    premises: PremisesEntity,
    startDate: LocalDate,
    endDate: LocalDate,
  ): Map<LocalDate, Availability> {
    if (endDate.isBefore(startDate)) throw RuntimeException("startDate must be before endDate when calculating availability for range")

    val bookings = bookingRepository.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate)
    val lostBeds = cas3VoidBedspacesRepository.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate)

    return startDate.getDaysUntilExclusiveEnd(endDate).map { date ->
      val bookingsOnDay = bookings.filter { booking -> booking.getArrivalDate() <= date && booking.getDepartureDate() > date }
      val lostBedsOnDay = lostBeds.filter { lostBed -> lostBed.startDate <= date && lostBed.endDate > date && lostBed.cancellation == null }

      Availability(
        date = date,
        pendingBookings = bookingsOnDay.count { !it.getArrived() && !it.getIsNotArrived() && !it.getCancelled() },
        arrivedBookings = bookingsOnDay.count { it.getArrived() },
        nonArrivedBookings = bookingsOnDay.count { it.getIsNotArrived() },
        cancelledBookings = bookingsOnDay.count { it.getCancelled() },
        voidBedspaces = lostBedsOnDay.size,
      )
    }.associateBy { it.date }
  }

  @SuppressWarnings("CyclomaticComplexMethod")
  fun createNewPremises(
    addressLine1: String,
    addressLine2: String?,
    town: String?,
    postcode: String,
    latitude: Double?,
    longitude: Double?,
    service: String,
    localAuthorityAreaId: UUID?,
    probationRegionId: UUID,
    name: String,
    notes: String?,
    characteristicIds: List<UUID>,
    status: PropertyStatus,
    probationDeliveryUnitIdentifier: Either<String, UUID>?,
    turnaroundWorkingDays: Int?,
  ) = validated {
    val probationRegion = probationRegionRepository.findByIdOrNull(probationRegionId)
    if (probationRegion == null) {
      "$.probationRegionId" hasValidationError "doesNotExist"
    }

    val localAuthorityArea = if (localAuthorityAreaId == null) {
      if (service == ServiceName.approvedPremises.value) {
        "$.localAuthorityAreaId" hasValidationError "empty"
      }
      null
    } else {
      val localAuthorityArea = localAuthorityAreaRepository.findByIdOrNull(localAuthorityAreaId)
      if (localAuthorityArea == null) {
        "$.localAuthorityAreaId" hasValidationError "doesNotExist"
      }
      localAuthorityArea
    }

    // start of validation
    if (addressLine1.isEmpty()) {
      "$.address" hasValidationError "empty"
    }

    if (postcode.isEmpty()) {
      "$.postcode" hasValidationError "empty"
    }

    if (service.isEmpty()) {
      "$.service" hasValidationError "empty"
    } else if (service != ServiceName.temporaryAccommodation.value) {
      "$.service" hasValidationError "onlyCas3Supported"
    }

    if (name.isEmpty()) {
      "$.name" hasValidationError "empty"
    }

    if (!premisesRepository.nameIsUniqueForType(name, TemporaryAccommodationPremisesEntity::class.java)) {
      "$.name" hasValidationError "notUnique"
    }

    val probationDeliveryUnit = tryGetProbationDeliveryUnit(probationDeliveryUnitIdentifier, probationRegionId) { property, err ->
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
      probationDeliveryUnit = probationDeliveryUnit,
      turnaroundWorkingDays = turnaroundWorkingDays ?: 2,
      endDate = null,
    )

    val characteristicEntities = characteristicIds.mapIndexed { index, uuid ->
      val entity = characteristicService.getCharacteristic(uuid)

      if (entity == null) {
        "$.characteristics[$index]" hasValidationError "doesNotExist"
      } else {
        if (!characteristicService.modelScopeMatches(entity, premises)) {
          "$.characteristics[$index]" hasValidationError "incorrectCharacteristicModelScope"
        }
        if (!characteristicService.serviceScopeMatches(entity, premises)) {
          "$.characteristics[$index]" hasValidationError "incorrectCharacteristicServiceScope"
        }
      }

      entity
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }
    // end of validation
    premises.characteristics.addAll(characteristicEntities.map { it!! })
    premisesRepository.save(premises)

    return success(premises)
  }

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
  ): AuthorisableActionResult<ValidatableActionResult<PremisesEntity>> {
    val premises = premisesRepository.findByIdOrNull(premisesId)
      ?: return AuthorisableActionResult.NotFound()

    val validationErrors = ValidationErrors()

    val localAuthorityArea = if (localAuthorityAreaId == null) {
      if (premises is ApprovedPremisesEntity) {
        validationErrors["$.localAuthorityAreaId"] = "empty"
      }
      null
    } else {
      val localAuthorityArea = localAuthorityAreaRepository.findByIdOrNull(localAuthorityAreaId)
      if (localAuthorityArea == null) {
        validationErrors["$.localAuthorityAreaId"] = "doesNotExist"
      }
      localAuthorityArea
    }

    val probationRegion = probationRegionRepository.findByIdOrNull(probationRegionId)

    if (probationRegion == null) {
      validationErrors["$.probationRegionId"] = "doesNotExist"
    }

    val characteristicEntities = characteristicIds.mapIndexed { index, uuid ->
      val entity = characteristicService.getCharacteristic(uuid)

      if (entity == null) {
        validationErrors["$.characteristics[$index]"] = "doesNotExist"
      } else {
        if (!characteristicService.modelScopeMatches(entity, premises)) {
          validationErrors["$.characteristics[$index]"] = "incorrectCharacteristicModelScope"
        }
        if (!characteristicService.serviceScopeMatches(entity, premises)) {
          validationErrors["$.characteristics[$index]"] = "incorrectCharacteristicServiceScope"
        }
      }

      entity
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
    }

    val savedPremises = premisesRepository.save(premises)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedPremises),
    )
  }

  @Transactional
  fun deletePremises(premises: PremisesEntity): ValidatableActionResult<Unit> = validated {
    if (premises.bookings.any()) {
      return premises.id hasConflictError "A premises cannot be hard-deleted if it has any bookings associated with it"
    }

    premises.voidBedspaces.forEach { lostBed ->
      cas3VoidBedspacesRepository.delete(lostBed)
    }

    premises.rooms.forEach { room ->
      room.beds.forEach { bed ->
        bedRepository.delete(bed)
      }
      roomRepository.delete(room)
    }
    premisesRepository.delete(premises)

    success(Unit)
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
      else -> probationDeliveryUnitIdentifier.fold({ name ->
        if (name.isBlank()) {
          onValidationError("$.pdu", "empty")
        }

        val result = probationDeliveryUnitRepository.findByNameAndProbationRegionId(name, probationRegionId)

        if (result == null) {
          onValidationError("$.pdu", "doesNotExist")
        }

        result
      }, { id ->
        val result = probationDeliveryUnitRepository.findByIdAndProbationRegionId(id, probationRegionId)

        if (result == null) {
          onValidationError("$.probationDeliveryUnitId", "doesNotExist")
        }

        result
      })
    }

    return probationDeliveryUnit
  }

  fun getBedCount(premises: PremisesEntity): Int = premisesRepository.getBedCount(premises)
}
