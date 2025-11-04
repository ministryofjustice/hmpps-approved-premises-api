package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class MappaEnvelope(

  val status: RiskEnvelopeStatus,

  val `value`: Mappa? = null,
)
