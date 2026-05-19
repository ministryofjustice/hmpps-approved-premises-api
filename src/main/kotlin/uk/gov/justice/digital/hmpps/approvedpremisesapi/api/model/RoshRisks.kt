package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class RoshRisks(

  @get:JsonProperty("overallRisk", required = true) val overallRisk: String,

  @get:JsonProperty("riskToChildren", required = true) val riskToChildren: String,

  @get:JsonProperty("riskToPublic", required = true) val riskToPublic: String,

  @get:JsonProperty("riskToKnownAdult", required = true) val riskToKnownAdult: String,

  @get:JsonProperty("riskToStaff", required = true) val riskToStaff: String,

  @get:JsonProperty("lastUpdated") val lastUpdated: java.time.LocalDate? = null,
)
