package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class RoshRisksEnvelope(

  val status: RiskEnvelopeStatus,

  val `value`: RoshRisks? = null,
)
