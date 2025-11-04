package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

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

  @get:JsonProperty("overallRisk", required = true) val overallRisk: kotlin.String,

  @get:JsonProperty("riskToChildren", required = true) val riskToChildren: kotlin.String,

  @get:JsonProperty("riskToPublic", required = true) val riskToPublic: kotlin.String,

  @get:JsonProperty("riskToKnownAdult", required = true) val riskToKnownAdult: kotlin.String,

  @get:JsonProperty("riskToStaff", required = true) val riskToStaff: kotlin.String,

  @get:JsonProperty("lastUpdated") val lastUpdated: java.time.LocalDate? = null,
)
