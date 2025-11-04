package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param notes
 * @param withdrawables
 */
data class Withdrawables(

  val notes: kotlin.collections.List<kotlin.String>,

  val withdrawables: kotlin.collections.List<Withdrawable>,
)
