package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_SINGLE_ROOM_CHARACTERISTIC_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningModelsFactory.Constants.DEFAULT_CHARACTERISTIC_WEIGHT
import java.time.LocalDate

@Service
class SpacePlanningModelsFactory(
  val spaceBookingRepository: Cas1SpaceBookingRepository,
) {
  object Constants {
    const val DEFAULT_CHARACTERISTIC_WEIGHT = 100
  }

  fun allBeds(
    premises: ApprovedPremisesEntity,
  ) = premises.rooms.flatMap { it.beds }.map { it.toBed() }

  fun allBedsDayState(
    day: LocalDate,
    premises: ApprovedPremisesEntity,
    outOfServiceBedRecords: List<Cas1OutOfServiceBedEntity>,
  ) = premises.rooms.flatMap { it.beds }
    .map { bedEntity ->
      BedDayState(
        bed = bedEntity.toBed(),
        day = day,
        inactiveReason = bedEntity.getInactiveReason(day, outOfServiceBedRecords),
      )
    }

  fun spaceBookingsForDay(day: LocalDate, premises: ApprovedPremisesEntity): List<SpaceBooking> =
    spaceBookingRepository.findAllBookingsOnGivenDayForPremises(premises.id, day)
      .map { booking ->
        SpaceBooking(
          id = booking.id,
          label = booking.crn,
          requiredRoomCharacteristics = toRoomCharacteristics(booking.criteria),
        )
      }

  private fun BedEntity.getInactiveReason(day: LocalDate, outOfServiceBedRecords: List<Cas1OutOfServiceBedEntity>) =
    if (this.isOutOfService(day, outOfServiceBedRecords)) {
      BedInactiveReason.OUT_OF_SERVICE
    } else if (!this.isActive(day)) {
      BedInactiveReason.ENDED
    } else {
      null
    }

  private fun BedEntity.isOutOfService(
    day: LocalDate,
    outOfServiceBedRecords: List<Cas1OutOfServiceBedEntity>,
  ) = outOfServiceBedRecords.any { it.isApplicable(day, candidate = this) }

  private fun BedEntity.toBed() = Bed(
    id = this.id,
    label = this.name,
    room = Room(
      id = this.room.id,
      label = this.room.name,
      characteristics = toRoomCharacteristics(this.room.characteristics),
    ),
  )

  private fun toRoomCharacteristics(characteristicEntities: List<CharacteristicEntity>) = characteristicEntities
    .filter { it.isActive }
    .filter { it.isModelScopeRoom() }
    .map { toCharacteristic(it) }
    .toSet()

  private fun toCharacteristic(characteristicEntity: CharacteristicEntity) = Characteristic(
    id = characteristicEntity.id,
    label = characteristicEntity.propertyName ?: "",
    weighting = DEFAULT_CHARACTERISTIC_WEIGHT,
    singleRoom = characteristicEntity.id == CAS1_SINGLE_ROOM_CHARACTERISTIC_ID,
  )
}
