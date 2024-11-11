package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning

import java.time.LocalDate
import java.util.UUID

data class Characteristic(
  val id: UUID,
  val label: String,
  val weighting: Int,
  val singleRoom: Boolean,
)

data class Bed(
  val id: UUID,
  val label: String,
  val room: Room,
)

data class BedDayState(
  val bed: Bed,
  val day: LocalDate,
  val inactiveReason: BedInactiveReason?,
) {
  fun isActive() = inactiveReason == null
}

sealed interface BedInactiveReason
data class BedEnded(val ended: LocalDate) : BedInactiveReason
data class BedOutOfService(val reason: String) : BedInactiveReason

data class Room(
  val id: UUID,
  val label: String,
  val characteristics: Set<Characteristic>,
)

data class SpaceBooking(
  val id: UUID,
  val label: String,
  val requiredRoomCharacteristics: Set<Characteristic>,
)
