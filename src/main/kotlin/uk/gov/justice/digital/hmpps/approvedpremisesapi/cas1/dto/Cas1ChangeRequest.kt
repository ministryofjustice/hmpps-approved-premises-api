package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import java.time.Instant
import java.util.UUID

data class Cas1ChangeRequest(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("type", required = true) val type: Cas1ChangeRequestType,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("requestReason", required = true) val requestReason: NamedId,

  @get:JsonProperty("requestJson", required = true) val requestJson: Any,

  @get:JsonProperty("spaceBookingId", required = true) val spaceBookingId: UUID,

  @get:JsonProperty("updatedAt", required = true) val updatedAt: Instant,

  @get:JsonProperty("decision") val decision: Cas1ChangeRequestDecision? = null,

  @get:JsonProperty("decisionJson") val decisionJson: Any? = null,

  @get:JsonProperty("rejectionReason") val rejectionReason: NamedId? = null,
)
