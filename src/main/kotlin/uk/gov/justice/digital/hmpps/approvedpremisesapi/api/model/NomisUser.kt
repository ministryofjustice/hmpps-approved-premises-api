package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class NomisUser(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "Roger Smith", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: String,

  @Schema(example = "SMITHR_GEN", required = true, description = "")
  @get:JsonProperty("nomisUsername", required = true) val nomisUsername: String,

  @Schema(example = "true", required = true, description = "")
  @get:JsonProperty("isActive", required = true) val isActive: Boolean,

  @Schema(example = "Roger.Smith@justice.gov.uk", description = "")
  @get:JsonProperty("email") val email: String? = null,
)
