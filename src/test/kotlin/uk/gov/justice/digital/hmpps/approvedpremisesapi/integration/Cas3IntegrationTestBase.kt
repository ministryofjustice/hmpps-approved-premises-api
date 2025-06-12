package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
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
    premises.forEach { premise ->
      val roomCharacteristics = getRoomCharacteristics().toMutableList()
      val rooms = roomEntityFactory.produceAndPersistMultiple(numOfRoomsPerPremise) {
        withPremises(premise)
        withBeds()
        withCharacteristics(
          mutableListOf(
            pickRandomCharacteristicAndRemoveFromList(roomCharacteristics),
            pickRandomCharacteristicAndRemoveFromList(roomCharacteristics),
            pickRandomCharacteristicAndRemoveFromList(roomCharacteristics),
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

  protected fun createBedspaceInPremises(
    premises: TemporaryAccommodationPremisesEntity,
    startDate: LocalDate,
    endDate: LocalDate? = null,
  ): BedEntity {
    val roomCharacteristics = getRoomCharacteristics().toMutableList()
    val room = roomEntityFactory.produceAndPersist {
      withPremises(premises)
      withBeds()
      withCharacteristics(
        mutableListOf(
          pickRandomCharacteristicAndRemoveFromList(roomCharacteristics),
          pickRandomCharacteristicAndRemoveFromList(roomCharacteristics),
          pickRandomCharacteristicAndRemoveFromList(roomCharacteristics),
        ),
      )
    }.apply { premises.rooms.add(this) }

    val bedspace = bedEntityFactory.produceAndPersist {
      withRoom(room)
      withStartDate(startDate)
      withEndDate(endDate)
    }.apply {
      premises.rooms
        .first { it.id == room.id }
        .beds.add(this)
    }

    return bedspace
  }

  protected fun pickRandomCharacteristicAndRemoveFromList(characteristics: MutableList<CharacteristicEntity>): CharacteristicEntity {
    val randomCharacteristic = randomOf(characteristics)
    characteristics.remove(randomCharacteristic)
    return randomCharacteristic
  }

  private fun getRoomCharacteristics() = characteristicRepository.findAllByServiceAndModelScope(
    modelScope = "room",
    serviceScope = ServiceName.temporaryAccommodation.value,
  )
}
