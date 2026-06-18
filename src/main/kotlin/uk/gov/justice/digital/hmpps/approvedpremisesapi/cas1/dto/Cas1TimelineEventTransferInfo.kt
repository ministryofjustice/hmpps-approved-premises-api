package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1TimelineEventTransferInfo(

  @get:JsonProperty("type", required = true) val type: Cas1TimelineEventTransferType,

  @get:JsonProperty("booking", required = true) val booking: Cas1TimelineEventPayloadBookingSummary,

  @get:JsonProperty("changeRequestId") val changeRequestId: java.util.UUID? = null,
)
