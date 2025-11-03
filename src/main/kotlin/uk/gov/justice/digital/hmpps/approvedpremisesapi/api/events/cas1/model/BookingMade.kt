package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TransferReason
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class BookingMade(

  val applicationId: UUID,

  val applicationUrl: String,

  val bookingId: UUID,

  val personReference: PersonReference,

  val deliusEventNumber: String,

  val createdAt: Instant,

  val bookedBy: BookingMadeBookedBy,

  val premises: Premises,

  val arrivalOn: LocalDate,

  val departureOn: LocalDate,

  val applicationSubmittedOn: Instant? = null,

  val releaseType: String? = null,

  val sentenceType: String? = null,

  val situation: String? = null,

  val characteristics: List<SpaceCharacteristic>? = null,

  val transferReason: TransferReason? = null,

  val transferredFrom: EventTransferInfo? = null,

  val additionalInformation: String? = null,
) : Cas1DomainEventPayload

data class EventTransferInfo(
  val type: EventTransferType,
  val changeRequestId: UUID? = null,
  val booking: EventBookingSummary,
)

enum class EventTransferType { PLANNED, EMERGENCY }
