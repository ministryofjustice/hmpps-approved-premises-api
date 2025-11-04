package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1ChangeRequest(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("type", required = true) val type: Cas1ChangeRequestType,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("requestReason", required = true) val requestReason: NamedId,

  @get:JsonProperty("requestJson", required = true) val requestJson: kotlin.Any,

  @get:JsonProperty("spaceBookingId", required = true) val spaceBookingId: java.util.UUID,

  @get:JsonProperty("updatedAt", required = true) val updatedAt: java.time.Instant,

  @get:JsonProperty("decision") val decision: Cas1ChangeRequestDecision? = null,

  @get:JsonProperty("decisionJson") val decisionJson: kotlin.Any? = null,

  @get:JsonProperty("rejectionReason") val rejectionReason: NamedId? = null,
)
