package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class PersonRisks(

  @get:JsonProperty("crn", required = true) val crn: kotlin.String,

  @get:JsonProperty("roshRisks", required = true) val roshRisks: RoshRisksEnvelope,

  @get:JsonProperty("tier", required = true) val tier: RiskTierEnvelope,

  @get:JsonProperty("flags", required = true) val flags: FlagsEnvelope,

  @get:JsonProperty("mappa") val mappa: MappaEnvelope? = null,
)
