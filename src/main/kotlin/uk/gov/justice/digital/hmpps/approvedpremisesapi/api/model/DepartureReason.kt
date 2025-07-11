package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param name
 * @param serviceScope
 * @param isActive
 * @param parentReasonId
 */
data class DepartureReason(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "Admitted to Hospital", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("serviceScope", required = true) val serviceScope: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("isActive", required = true) val isActive: kotlin.Boolean,

  @Schema(example = "null", description = "")
  @get:JsonProperty("parentReasonId") val parentReasonId: java.util.UUID? = null,
)
