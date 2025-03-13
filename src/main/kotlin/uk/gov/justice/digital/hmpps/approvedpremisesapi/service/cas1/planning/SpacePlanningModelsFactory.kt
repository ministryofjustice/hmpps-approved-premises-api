package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1PlanningBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_SINGLE_ROOM
import java.time.LocalDate

@Service
class SpacePlanningModelsFactory {
  companion object {
    const val DEFAULT_CHARACTERISTIC_WEIGHT = 100
  }

  fun allBeds(
    beds: List<Cas1PlanningBedSummary>,
  ) = beds.map { it.toBed() }

  fun allBedsDayState(
    day: LocalDate,
    beds: List<Cas1PlanningBedSummary>,
    outOfServiceBedRecordsToConsider: List<Cas1OutOfServiceBedEntity>,
  ): List<BedDayState> = beds
    .map { bedSummary ->
      BedDayState(
        bed = bedSummary.toBed(),
        day = day,
        inactiveReason = bedSummary.getInactiveReason(day, outOfServiceBedRecordsToConsider),
      )
    }

  fun spaceBookingsForDay(
    day: LocalDate,
    spaceBookingsToConsider: List<Cas1SpaceBookingEntity>,
  ): List<SpaceBooking> = spaceBookingsToConsider
    .filter { it.isExpectedOrResident(day) }
    .map { booking ->
      SpaceBooking(
        id = booking.id,
        label = booking.crn,
        requiredRoomCharacteristics = toRoomCharacteristics(booking.criteria.mapNotNull { it.propertyName }),
      )
    }

  private fun Cas1PlanningBedSummary.getInactiveReason(day: LocalDate, outOfServiceBedRecords: List<Cas1OutOfServiceBedEntity>): BedInactiveReason? {
    val outOfServiceRecord = this.findOutOfServiceRecord(day, outOfServiceBedRecords)
    return if (outOfServiceRecord != null) {
      BedOutOfService(outOfServiceRecord.reason.name)
    } else if (!BedEntity.isActive(day, this.bedEndDate)) {
      BedEnded(this.bedEndDate!!)
    } else {
      null
    }
  }

  private fun Cas1PlanningBedSummary.findOutOfServiceRecord(
    day: LocalDate,
    outOfServiceBedRecords: List<Cas1OutOfServiceBedEntity>,
  ) = outOfServiceBedRecords.firstOrNull { it.isApplicable(day, bedId = this.bedId) }

  private fun Cas1PlanningBedSummary.toBed() = Bed(
    id = this.bedId,
    label = this.bedName,
    room = Room(
      id = this.roomId,
      label = this.roomName,
      characteristics = toRoomCharacteristics(this.characteristicsPropertyNames),
    ),
  )

  private fun toRoomCharacteristics(characteristicPropertyNames: List<String>) = characteristicPropertyNames
    .asSequence()
    .filter { Cas1SpaceBookingEntity.ROOM_CHARACTERISTICS_OF_INTEREST.contains(it) }
    .map { toCharacteristic(it) }
    .toSet()

  private fun toCharacteristic(propertyName: String) = Characteristic(
    label = propertyName,
    propertyName = propertyName,
    weighting = DEFAULT_CHARACTERISTIC_WEIGHT,
    singleRoom = propertyName == CAS1_PROPERTY_NAME_SINGLE_ROOM,
  )
}
