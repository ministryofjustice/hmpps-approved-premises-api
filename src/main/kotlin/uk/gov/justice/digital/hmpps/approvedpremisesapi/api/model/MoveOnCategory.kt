package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class MoveOnCategory(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "Housing Association - Rented", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: String,

  @get:JsonProperty("serviceScope", required = true) val serviceScope: String,

  @get:JsonProperty("isActive", required = true) val isActive: Boolean,
)
