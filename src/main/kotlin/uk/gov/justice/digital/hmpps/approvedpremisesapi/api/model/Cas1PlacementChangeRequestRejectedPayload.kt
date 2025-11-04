package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1PlacementChangeRequestRejectedPayload(

  @get:JsonProperty("changeRequestId", required = true) val changeRequestId: java.util.UUID,

  @get:JsonProperty("booking", required = true) val booking: Cas1TimelineEventPayloadBookingSummary,

  @get:JsonProperty("reason", required = true) val reason: NamedId,

  @get:JsonProperty("changeRequestType", required = true) val changeRequestType: Cas1ChangeRequestType,

  @get:JsonProperty("type", required = true) override val type: Cas1TimelineEventType,
) : Cas1TimelineEventContentPayload
