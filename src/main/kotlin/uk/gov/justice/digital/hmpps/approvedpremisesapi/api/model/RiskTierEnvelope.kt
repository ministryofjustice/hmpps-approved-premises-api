package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param status
 * @param &#x60;value&#x60;
 */
data class RiskTierEnvelope(

  val status: RiskEnvelopeStatus,

  val `value`: RiskTier? = null,
)
