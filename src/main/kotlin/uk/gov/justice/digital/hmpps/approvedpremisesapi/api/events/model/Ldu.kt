package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param code
 * @param name
 */
data class Ldu(

  @Schema(example = "N54PPU", required = true, description = "")
  @get:JsonProperty("code", required = true) val code: kotlin.String,

  @Schema(example = "Public Protection NE", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,
)
