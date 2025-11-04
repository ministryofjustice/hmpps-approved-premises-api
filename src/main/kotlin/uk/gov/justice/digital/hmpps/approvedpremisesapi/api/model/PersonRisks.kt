package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class PersonRisks(

  val crn: kotlin.String,

  val roshRisks: RoshRisksEnvelope,

  val tier: RiskTierEnvelope,

  val flags: FlagsEnvelope,

  val mappa: MappaEnvelope? = null,
)
