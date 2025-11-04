package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1BookingMadeContentPayload(

  val booking: Cas1TimelineEventPayloadBookingSummary,

  val eventNumber: kotlin.String,

  override val type: Cas1TimelineEventType,

  val transferredFrom: Cas1TimelineEventTransferInfo? = null,
) : Cas1TimelineEventContentPayload
