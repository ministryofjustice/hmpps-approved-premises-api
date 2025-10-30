package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.util.UUID

data class BookingMade(

  val applicationId: UUID,

  val applicationUrl: String,

  val bookingId: UUID,

  val personReference: PersonReference,

  val deliusEventNumber: String,

  val createdAt: java.time.Instant,

  val bookedBy: BookingMadeBookedBy,

  val premises: Premises,

  val arrivalOn: java.time.LocalDate,

  val departureOn: java.time.LocalDate,

  val applicationSubmittedOn: java.time.Instant? = null,

  val releaseType: String? = null,

  val sentenceType: String? = null,

  val situation: String? = null,

  val characteristics: List<SpaceCharacteristic>? = null,

  val transferReason: String? = null,

  val transferredFrom: EventTransferInfo? = null,
) : Cas1DomainEventPayload

data class EventTransferInfo(
  val type: EventTransferType,
  val changeRequestId: UUID? = null,
  val booking: EventBookingSummary,
)

enum class EventTransferType { PLANNED, EMERGENCY }
