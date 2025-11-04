package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param status
 * @param &#x60;value&#x60;
 */
data class RoshRisksEnvelope(

  val status: RiskEnvelopeStatus,

  val `value`: RoshRisks? = null,
)
