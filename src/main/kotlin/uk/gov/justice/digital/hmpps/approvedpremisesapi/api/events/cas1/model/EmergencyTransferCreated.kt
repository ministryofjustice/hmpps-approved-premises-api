package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.time.Instant
import java.util.UUID

data class EmergencyTransferCreated(
  val applicationId: UUID,
  val createdAt: Instant,
  val createdBy: StaffMember,
  val from: TransferBooking,
  val to: TransferBooking,
) : Cas1DomainEventPayload

data class TransferBooking(
  val bookingId: UUID,
  val premises: Premises,
  val arrivalOn: java.time.LocalDate,
  val departureOn: java.time.LocalDate,
)