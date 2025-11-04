package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1NewChangeRequest(

  @get:JsonProperty("spaceBookingId", required = true) val spaceBookingId: java.util.UUID,

  @get:JsonProperty("type", required = true) val type: Cas1ChangeRequestType,

  @get:JsonProperty("requestJson", required = true) val requestJson: kotlin.Any,

  @get:JsonProperty("reasonId", required = true) val reasonId: java.util.UUID,
)
