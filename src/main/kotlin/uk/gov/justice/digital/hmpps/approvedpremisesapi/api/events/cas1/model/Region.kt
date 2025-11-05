package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Region(

  @field:Schema(example = "NE", required = true, description = "")
  @get:JsonProperty("code", required = true) val code: kotlin.String,

  @field:Schema(example = "North East", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,
)
