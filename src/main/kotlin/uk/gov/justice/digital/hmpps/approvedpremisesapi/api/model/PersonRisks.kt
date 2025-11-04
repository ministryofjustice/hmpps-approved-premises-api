package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param crn
 * @param roshRisks
 * @param tier
 * @param flags
 * @param mappa
 */
data class PersonRisks(

  val crn: kotlin.String,

  val roshRisks: RoshRisksEnvelope,

  val tier: RiskTierEnvelope,

  val flags: FlagsEnvelope,

  val mappa: MappaEnvelope? = null,
)
