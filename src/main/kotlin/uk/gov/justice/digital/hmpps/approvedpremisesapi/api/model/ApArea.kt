package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class ApArea(

  @field:Schema(example = "cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @field:Schema(example = "LON", required = true, description = "")
  @get:JsonProperty("identifier", required = true) val identifier: kotlin.String,

  @field:Schema(example = "Yorkshire & The Humber", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,
)
