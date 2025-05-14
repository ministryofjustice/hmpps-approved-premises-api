package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TransferType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class Cas1BookingCreatedEvent(
  val booking: Cas1SpaceBookingEntity,
  val createdBy: UserEntity,
  val transferredFrom: TransferInfo? = null,
)

data class Cas1BookingChangedEvent(
  val booking: Cas1SpaceBookingEntity,
  val changedBy: UserEntity,
  val bookingChangedAt: OffsetDateTime,
  val previousArrivalDateIfChanged: LocalDate?,
  val previousDepartureDateIfChanged: LocalDate?,
  val previousCharacteristicsIfChanged: List<CharacteristicEntity>?,
  val transferredTo: TransferInfo? = null,
)

data class Cas1BookingCancelledEvent(
  val booking: Cas1SpaceBookingEntity,
  val user: UserEntity,
  val reason: CancellationReasonEntity,
  val appealChangeRequestId: UUID? = null,
)

data class TransferInfo(
  val type: TransferType,
  val changeRequestId: UUID? = null,
  val booking: Cas1SpaceBookingEntity,
)
