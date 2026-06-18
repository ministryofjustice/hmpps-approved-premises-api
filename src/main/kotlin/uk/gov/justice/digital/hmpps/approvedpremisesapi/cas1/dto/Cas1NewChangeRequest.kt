package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class Cas1NewChangeRequest(

  @get:JsonProperty("spaceBookingId", required = true) val spaceBookingId: UUID,

  @get:JsonProperty("type", required = true) val type: Cas1ChangeRequestType,

  @get:JsonProperty("requestJson", required = true) val requestJson: Any,

  @get:JsonProperty("reasonId", required = true) val reasonId: UUID,
)
