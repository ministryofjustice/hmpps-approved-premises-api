package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

fun IntegrationTestBase.givenATemporaryAccommodationRoom(
  premises: TemporaryAccommodationPremisesEntity? = null,
  name: String = randomStringMultiCaseWithNumbers(8),
  characteristics: List<CharacteristicEntity> = emptyList(),
  block: ((room: RoomEntity) -> Unit)? = null,
): RoomEntity {
  val resolvedPremises = premises ?: givenATemporaryAccommodationPremises()

  val room = roomEntityFactory.produceAndPersist {
    withPremises(resolvedPremises)
    withName(name)
    withCharacteristics(characteristics.toMutableList())
  }

  block?.invoke(room)
  return room
}

fun IntegrationTestBase.givenATemporaryAccommodationRooms(
  premises: TemporaryAccommodationPremisesEntity? = null,
  count: Int = 1,
  roomNames: List<String> = emptyList(),
  characteristics: List<CharacteristicEntity> = emptyList(),
  block: ((rooms: List<RoomEntity>) -> Unit)? = null,
): List<RoomEntity> {
  val resolvedPremises = premises ?: givenATemporaryAccommodationPremises()
  val rooms = mutableListOf<RoomEntity>()

  repeat(count) { index ->
    val roomName = if (roomNames.size > index) roomNames[index] else randomStringMultiCaseWithNumbers(8)
    val room = givenATemporaryAccommodationRoom(
      premises = resolvedPremises,
      name = roomName,
      characteristics = characteristics,
    )
    rooms.add(room)
  }

  block?.invoke(rooms)
  return rooms
} 
