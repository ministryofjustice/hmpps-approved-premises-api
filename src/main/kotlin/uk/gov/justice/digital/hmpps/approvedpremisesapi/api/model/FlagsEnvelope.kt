package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class FlagsEnvelope(

  @get:JsonProperty("status", required = true) val status: RiskEnvelopeStatus,

  @get:JsonProperty("value") val `value`: kotlin.collections.List<kotlin.String>? = null,
)
