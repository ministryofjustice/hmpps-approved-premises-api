package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto.RiskEnvelopeStatusDto

data class RoshRisksEnvelope(

  @get:JsonProperty("status", required = true) val status: RiskEnvelopeStatusDto,

  @get:JsonProperty("value") val `value`: RoshRisks? = null,
)
