package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param notes
 * @param withdrawables
 */
data class Withdrawables(

  @get:JsonProperty("notes", required = true) val notes: kotlin.collections.List<kotlin.String>,

  @get:JsonProperty("withdrawables", required = true) val withdrawables: kotlin.collections.List<Withdrawable>,
)
