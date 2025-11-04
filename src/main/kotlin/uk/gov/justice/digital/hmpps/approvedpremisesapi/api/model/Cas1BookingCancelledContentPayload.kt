package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

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
