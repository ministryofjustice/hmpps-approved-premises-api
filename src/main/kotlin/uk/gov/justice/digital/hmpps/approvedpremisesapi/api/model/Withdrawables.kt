package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param notes
 * @param withdrawables
 */
data class Withdrawables(

  val notes: kotlin.collections.List<kotlin.String>,

  val withdrawables: kotlin.collections.List<Withdrawable>,
)
