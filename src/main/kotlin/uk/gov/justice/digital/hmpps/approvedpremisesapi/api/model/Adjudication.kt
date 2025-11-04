package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Adjudication(

  @get:JsonProperty("id", required = true) val id: kotlin.Long,

  @get:JsonProperty("reportedAt", required = true) val reportedAt: java.time.Instant,

  @get:JsonProperty("establishment", required = true) val establishment: kotlin.String,

  @Schema(example = "Wounding or inflicting grievous bodily harm (inflicting bodily injury with or without weapon) (S20) - 00801", required = true, description = "")
  @get:JsonProperty("offenceDescription", required = true) val offenceDescription: kotlin.String,

  @get:JsonProperty("hearingHeld", required = true) val hearingHeld: kotlin.Boolean,

  @get:JsonProperty("finding") val finding: kotlin.String? = null,
)
