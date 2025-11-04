package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param booking
 * @param eventNumber
 * @param transferredFrom
 */
data class Cas1BookingMadeContentPayload(

  val booking: Cas1TimelineEventPayloadBookingSummary,

  val eventNumber: kotlin.String,

  override val type: Cas1TimelineEventType,

  val transferredFrom: Cas1TimelineEventTransferInfo? = null,
) : Cas1TimelineEventContentPayload
