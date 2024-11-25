package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param reportedAt
 * @param establishment
 * @param offenceDescription
 * @param hearingHeld
 * @param finding
 */
data class Adjudication(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: kotlin.Long,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("reportedAt", required = true) val reportedAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("establishment", required = true) val establishment: kotlin.String,

  @Schema(example = "Wounding or inflicting grievous bodily harm (inflicting bodily injury with or without weapon) (S20) - 00801", required = true, description = "")
  @get:JsonProperty("offenceDescription", required = true) val offenceDescription: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("hearingHeld", required = true) val hearingHeld: kotlin.Boolean,

  @Schema(example = "null", description = "")
  @get:JsonProperty("finding") val finding: kotlin.String? = null,
)
