package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param name
 * @param nomisUsername
 * @param isActive
 * @param email
 */
data class NomisUser(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "Roger Smith", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @Schema(example = "SMITHR_GEN", required = true, description = "")
  @get:JsonProperty("nomisUsername", required = true) val nomisUsername: kotlin.String,

  @Schema(example = "true", required = true, description = "")
  @get:JsonProperty("isActive", required = true) val isActive: kotlin.Boolean,

  @Schema(example = "Roger.Smith@justice.gov.uk", description = "")
  @get:JsonProperty("email") val email: kotlin.String? = null,
)
