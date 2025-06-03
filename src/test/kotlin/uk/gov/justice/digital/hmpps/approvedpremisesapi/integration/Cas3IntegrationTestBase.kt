package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import java.time.LocalDate

abstract class Cas3IntegrationTestBase : IntegrationTestBase() {

  protected fun createRoomsWithSingleBedInPremises(
    premises: List<TemporaryAccommodationPremisesEntity>,
    endDate: LocalDate? = null,
    numOfRoomsPerPremise: Int = 1,
  ): List<TemporaryAccommodationPremisesEntity> {
    val cas3RoomCharacteristics = characteristicRepository.findAllCharacteristicsReferenceData(
      modelScope = "room",
      serviceScope = ServiceName.temporaryAccommodation.value,
    )
    premises.forEach { premise ->
      val characteristicsCopy = cas3RoomCharacteristics.toMutableList()
      val rooms = roomEntityFactory.produceAndPersistMultiple(numOfRoomsPerPremise) {
        withPremises(premise)
        withBeds()
        withCharacteristics(
          mutableListOf(
            pickRandomCharacteristicAndRemoveFromList(characteristicsCopy),
            pickRandomCharacteristicAndRemoveFromList(characteristicsCopy),
            pickRandomCharacteristicAndRemoveFromList(characteristicsCopy),
          ),
        )
      }.apply { premise.rooms.addAll(this) }

      rooms.forEach { room ->
        bedEntityFactory.produceAndPersist {
          withRoom(room)
          withEndDate(endDate)
        }.apply {
          premise.rooms
            .first { it.id == room.id }
            .beds.add(this)
        }
      }
    }
    return premises
  }

  protected fun pickRandomCharacteristicAndRemoveFromList(characteristics: MutableList<CharacteristicEntity>): CharacteristicEntity {
    val randomCharacteristic = randomOf(characteristics)
    characteristics.remove(randomCharacteristic)
    return randomCharacteristic
  }
}
