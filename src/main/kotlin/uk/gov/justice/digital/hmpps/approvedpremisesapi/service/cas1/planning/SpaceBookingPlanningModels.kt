package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning

import java.util.UUID

data class Characteristic(
  val id: UUID,
  val name: String,
  val weighting: Int,
  val singleRoom: Boolean = false,
)

data class Bed(
  val id: UUID,
  val label: String,
  val room: Room,
)

data class Room(
  val id: UUID,
  val label: String,
  val characteristics: Set<Characteristic>,
)

data class SpaceBooking(
  val spaceBookingId: UUID,
  val label: String,
  val requiredCharacteristics: Set<Characteristic>,
)
