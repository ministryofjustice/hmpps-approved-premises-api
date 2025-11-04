package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class RiskTierEnvelope(

  val status: RiskEnvelopeStatus,

  val `value`: RiskTier? = null,
)
