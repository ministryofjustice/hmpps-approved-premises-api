package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param code
 * @param name
 */
data class ProbationArea(

  @Schema(example = "N02", required = true, description = "")
  @get:JsonProperty("code", required = true) val code: kotlin.String,

  @Schema(example = "NPS North East", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,
)
