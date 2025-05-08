package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import java.time.LocalDate
import java.time.OffsetDateTime

data class Cas1BookingCreatedEvent(
  val booking: Cas1SpaceBookingEntity,
  val createdBy: UserEntity,
)

data class Cas1BookingChangedEvent(
  val booking: Cas1SpaceBookingEntity,
  val changedBy: UserEntity,
  val bookingChangedAt: OffsetDateTime,
  val previousArrivalDateIfChanged: LocalDate?,
  val previousDepartureDateIfChanged: LocalDate?,
  val previousCharacteristicsIfChanged: List<CharacteristicEntity>?,
)
