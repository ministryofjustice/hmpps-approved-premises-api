package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import java.util.UUID

@Service
class RoomService(
  private val roomRepository: RoomRepository,
  private val bedRepository: BedRepository,
  private val characteristicRepository: CharacteristicRepository,
) {

  fun createRoom(
    premises: PremisesEntity,
    roomName: String,
    notes: String?,
    characteristics: List<UUID>,
  ): ValidatableActionResult<RoomEntity> = validated {
    if (roomName.isEmpty()) {
      "$.name" hasValidationError "empty"
    }

    val characteristicEntities = characteristics.mapIndexed { index, uuid ->
      val entity = characteristicRepository.findByIdOrNull(uuid)

      if (entity == null) {
        "$.characteristics[$index]" hasValidationError "doesNotExist"
      } else {
        if (entity.modelScope != "room") {
          "$.characteristics[$index]" hasValidationError "incorrectCharacteristicModelScope"
        }
        if (premises is TemporaryAccommodationPremisesEntity && entity.serviceScope != "temporary-accommodation") {
          "$.characteristics[$index]" hasValidationError "incorrectCharacteristicServiceScope"
        }
        if (premises is ApprovedPremisesEntity && entity.serviceScope != "approved-premises") {
          "$.characteristics[$index]" hasValidationError "incorrectCharacteristicServiceScope"
        }
      }

      entity
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    val automaticallyCreateBed = when (premises) {
      is TemporaryAccommodationPremisesEntity -> true
      else -> false
    }

    val room = roomRepository.save(
      RoomEntity(
        id = UUID.randomUUID(),
        name = roomName,
        notes = notes,
        premises = premises,
        beds = mutableListOf(),
        characteristics = characteristicEntities.map { it!! }.toMutableList(),
      )
    )

    if (automaticallyCreateBed) {
      val bed = createBedInternal(room, "default-bed")
      room.beds.add(bed)
    }

    return success(room)
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
      room = room,
    )
  )
}
