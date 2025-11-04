package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param level
 * @param lastUpdated
 */
data class RiskTier(

  val level: kotlin.String,

  val lastUpdated: java.time.LocalDate,
)
