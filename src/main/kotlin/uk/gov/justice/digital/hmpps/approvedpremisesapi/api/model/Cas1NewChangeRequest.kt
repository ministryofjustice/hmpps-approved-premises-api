package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param spaceBookingId
 * @param type
 * @param requestJson Any object
 * @param reasonId
 */
data class Cas1NewChangeRequest(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("spaceBookingId", required = true) val spaceBookingId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) val type: Cas1ChangeRequestType,

  @Schema(example = "null", required = true, description = "Any object")
  @get:JsonProperty("requestJson", required = true) val requestJson: kotlin.Any,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("reasonId", required = true) val reasonId: java.util.UUID,
)
