package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import java.util.UUID
import javax.transaction.Transactional

@Service
class RoomService(
  private val roomRepository: RoomRepository,
  private val bedRepository: BedRepository,
  private val bookingRepository: BookingRepository,
  private val lostBedsRepository: LostBedsRepository,
  private val characteristicService: CharacteristicService,
) {
  fun getRoom(roomId: UUID) = roomRepository.findByIdOrNull(roomId)

  fun createRoom(
    premises: PremisesEntity,
    roomName: String,
    notes: String?,
    characteristicIds: List<UUID>,
  ): ValidatableActionResult<RoomEntity> = validated {
    // RoomEntity needs to be created before the validation so that the CharacteristicService can match the
    // model and service scopes against it.
    var room = RoomEntity(
      id = UUID.randomUUID(),
      name = roomName,
      code = null,
      notes = notes,
      premises = premises,
      beds = mutableListOf(),
      characteristics = mutableListOf(),
    )

    if (roomName.isEmpty()) {
      "$.name" hasValidationError "empty"
    }

    val characteristicEntities = characteristicIds.mapIndexed { index, uuid ->
      val entity = characteristicService.getCharacteristic(uuid)

      if (entity == null) {
        "$.characteristics[$index]" hasValidationError "doesNotExist"
      } else {
        if (!characteristicService.modelScopeMatches(entity, room)) {
          "$.characteristics[$index]" hasValidationError "incorrectCharacteristicModelScope"
        }
        if (!characteristicService.serviceScopeMatches(entity, room)) {
          "$.characteristics[$index]" hasValidationError "incorrectCharacteristicServiceScope"
        }
      }

      entity
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    room.characteristics.addAll(characteristicEntities.map { it!! })

    room = roomRepository.save(room)

    val automaticallyCreateBed = when (premises) {
      is TemporaryAccommodationPremisesEntity -> true
      else -> false
    }

    if (automaticallyCreateBed) {
      val bed = createBedInternal(room, "default-bed")
      room.beds.add(bed)
    }

    return success(room)
  }

  fun updateRoom(
    premises: PremisesEntity,
    roomId: UUID,
    notes: String?,
    characteristicIds: List<UUID>,
  ): AuthorisableActionResult<ValidatableActionResult<RoomEntity>> {
    var room = roomRepository.findByIdOrNull(roomId) ?: return AuthorisableActionResult.NotFound()

    if (room.premises.id != premises.id) {
      return AuthorisableActionResult.NotFound()
    }

    val validationErrors = ValidationErrors()

    val characteristicEntities = characteristicIds.mapIndexed { index, uuid ->
      val entity = characteristicService.getCharacteristic(uuid)

      if (entity == null) {
        validationErrors["$.characteristics[$index]"] = "doesNotExist"
      } else {
        if (!characteristicService.modelScopeMatches(entity, room)) {
          validationErrors["$.characteristics[$index]"] = "incorrectCharacteristicModelScope"
        }
        if (!characteristicService.serviceScopeMatches(entity, room)) {
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

    room = RoomEntity(
      id = room.id,
      name = room.name,
      code = null,
      notes = notes,
      premises = room.premises,
      beds = room.beds,
      characteristics = characteristicEntities.map { it!! }.toMutableList(),
    )

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(roomRepository.save(room)),
    )
  }

  fun createBed(
    room: RoomEntity,
    bedName: String,
  ): ValidatableActionResult<BedEntity> = validated {
    if (bedName.isEmpty()) {
      "$.name" hasValidationError "empty"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    return success(createBedInternal(room, bedName))
  }

  private fun createBedInternal(
    room: RoomEntity,
    bedName: String,
  ): BedEntity = bedRepository.save(
    BedEntity(
      id = UUID.randomUUID(),
      name = bedName,
      code = null,
      room = room,
    ),
  )

  @Transactional
  fun deleteRoom(room: RoomEntity): ValidatableActionResult<Unit> = validated {
    val bedIds = room.beds.map { it.id }

    if (bookingRepository.findByBedIds(bedIds).any()) {
      return room.id hasConflictError "A room cannot be hard-deleted if it has any bookings associated with it"
    }

    lostBedsRepository.findByBedIds(bedIds).forEach { lostBed ->
      lostBedsRepository.delete(lostBed)
    }

    room.beds.forEach { bed ->
      bedRepository.delete(bed)
    }
    roomRepository.delete(room)

    success(Unit)
  }
}
