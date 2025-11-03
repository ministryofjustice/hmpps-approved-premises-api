package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param notes
 */
data class NewLostBedCancellation(

  @get:JsonProperty("notes") val notes: kotlin.String? = null,
)
