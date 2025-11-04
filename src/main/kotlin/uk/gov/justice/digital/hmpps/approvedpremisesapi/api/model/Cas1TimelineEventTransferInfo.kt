package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param type
 * @param booking
 * @param changeRequestId
 */
data class Cas1TimelineEventTransferInfo(

  val type: Cas1TimelineEventTransferType,

  val booking: Cas1TimelineEventPayloadBookingSummary,

  val changeRequestId: java.util.UUID? = null,
)
