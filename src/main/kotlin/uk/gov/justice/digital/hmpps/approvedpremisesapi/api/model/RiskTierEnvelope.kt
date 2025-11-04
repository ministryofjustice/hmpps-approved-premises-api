package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param status
 * @param &#x60;value&#x60;
 */
data class RiskTierEnvelope(

  @get:JsonProperty("status", required = true) val status: RiskEnvelopeStatus,

  @get:JsonProperty("value") val `value`: RiskTier? = null,
)
