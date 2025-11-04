package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class MappaEnvelope(

  @get:JsonProperty("status", required = true) val status: RiskEnvelopeStatus,

  @get:JsonProperty("value") val `value`: Mappa? = null,
)
