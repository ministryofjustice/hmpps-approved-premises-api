package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param code
 * @param name
 */
data class Region(

  @Schema(example = "NE", required = true, description = "")
  val code: kotlin.String,

  @Schema(example = "North East", required = true, description = "")
  val name: kotlin.String,
)
