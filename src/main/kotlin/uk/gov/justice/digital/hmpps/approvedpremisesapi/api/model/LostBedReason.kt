package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class LostBedReason(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "Double Room with Single Occupancy - Other (Non-FM)", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: String,

  @get:JsonProperty("isActive", required = true) val isActive: Boolean,

  @get:JsonProperty("serviceScope", required = true) val serviceScope: String,
)
