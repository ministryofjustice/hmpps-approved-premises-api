package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import java.time.LocalDate

abstract class Cas3IntegrationTestBase : IntegrationTestBase() {

  protected fun createRoomsWithSingleBedInPremises(
    premises: List<TemporaryAccommodationPremisesEntity>,
    endDate: LocalDate? = null,
    numOfRoomsPerPremise: Int = 1,
  ): List<TemporaryAccommodationPremisesEntity> {
    premises.forEach { premise ->
      val rooms = roomEntityFactory.produceAndPersistMultiple(numOfRoomsPerPremise) {
        withPremises(premise)
        withBeds()
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
}
