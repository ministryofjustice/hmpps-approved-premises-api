package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param level
 * @param lastUpdated
 */
data class RiskTier(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("level", required = true) val level: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("lastUpdated", required = true) val lastUpdated: java.time.LocalDate,
)
