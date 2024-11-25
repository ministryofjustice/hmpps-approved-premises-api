package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param code
 * @param name
 */
data class Team(

  @Schema(example = "N54NGH", required = true, description = "")
  @get:JsonProperty("code", required = true) val code: kotlin.String,

  @Schema(example = "Gateshead 1", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,
)
