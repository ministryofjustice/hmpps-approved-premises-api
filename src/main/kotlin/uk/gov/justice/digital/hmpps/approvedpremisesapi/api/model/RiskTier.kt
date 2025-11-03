package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param level
 * @param lastUpdated
 */
data class RiskTier(

  @get:JsonProperty("level", required = true) val level: kotlin.String,

  @get:JsonProperty("lastUpdated", required = true) val lastUpdated: java.time.LocalDate,
)
