package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param code
 * @param keyWorker
 * @param name
 */
data class StaffMember(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("code", required = true) val code: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("keyWorker", required = true) val keyWorker: kotlin.Boolean,

  @Schema(example = "Brown, James (PS - PSO)", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,
)
