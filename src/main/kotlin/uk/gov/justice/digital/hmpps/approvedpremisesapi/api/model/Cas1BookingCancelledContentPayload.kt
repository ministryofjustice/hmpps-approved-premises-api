package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param booking
 * @param cancellationReason
 * @param appealChangeRequestId
 */
data class Cas1BookingCancelledContentPayload(

  val booking: Cas1TimelineEventPayloadBookingSummary,

  val cancellationReason: kotlin.String,

  override val type: Cas1TimelineEventType,

  val appealChangeRequestId: java.util.UUID? = null,
) : Cas1TimelineEventContentPayload
