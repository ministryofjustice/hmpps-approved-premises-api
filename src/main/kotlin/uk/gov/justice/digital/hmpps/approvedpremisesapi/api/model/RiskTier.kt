package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param level
 * @param lastUpdated
 */
data class RiskTier(

  val level: kotlin.String,

  val lastUpdated: java.time.LocalDate,
)
