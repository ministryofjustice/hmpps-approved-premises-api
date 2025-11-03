package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param crn
 * @param noms
 */
data class PersonReference(

  @get:JsonProperty("crn", required = true) val crn: kotlin.String,

  @get:JsonProperty("noms") val noms: kotlin.String? = null,
)
