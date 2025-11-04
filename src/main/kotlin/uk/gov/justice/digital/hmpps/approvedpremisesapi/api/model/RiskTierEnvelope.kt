package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class RiskTierEnvelope(

  @get:JsonProperty("status", required = true) val status: RiskEnvelopeStatus,

  @get:JsonProperty("value") val `value`: RiskTier? = null,
)
