package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param overallRisk
 * @param riskToChildren
 * @param riskToPublic
 * @param riskToKnownAdult
 * @param riskToStaff
 * @param lastUpdated
 */
data class RoshRisks(

  val overallRisk: kotlin.String,

  val riskToChildren: kotlin.String,

  val riskToPublic: kotlin.String,

  val riskToKnownAdult: kotlin.String,

  val riskToStaff: kotlin.String,

  val lastUpdated: java.time.LocalDate? = null,
)
