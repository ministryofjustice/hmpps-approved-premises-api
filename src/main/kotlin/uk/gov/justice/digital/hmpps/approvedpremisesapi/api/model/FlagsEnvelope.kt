package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class FlagsEnvelope(

  val status: RiskEnvelopeStatus,

  val `value`: kotlin.collections.List<kotlin.String>? = null,
)
