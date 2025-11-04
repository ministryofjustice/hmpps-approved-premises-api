package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param noms
 * @param crn
 */
data class PersonReference(

  val noms: kotlin.String,

  val crn: kotlin.String? = null,
)
