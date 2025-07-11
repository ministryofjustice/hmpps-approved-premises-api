package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param overallRisk
 * @param riskToChildren
 * @param riskToPublic
 * @param riskToKnownAdult
 * @param riskToStaff
 * @param lastUpdated
 */
data class RoshRisks(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("overallRisk", required = true) val overallRisk: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("riskToChildren", required = true) val riskToChildren: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("riskToPublic", required = true) val riskToPublic: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("riskToKnownAdult", required = true) val riskToKnownAdult: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("riskToStaff", required = true) val riskToStaff: kotlin.String,

  @Schema(example = "null", description = "")
  @get:JsonProperty("lastUpdated") val lastUpdated: java.time.LocalDate? = null,
)
