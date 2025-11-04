package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param crn
 * @param name
 */
data class BookingSearchResultPersonSummary(

  @get:JsonProperty("crn", required = true) val crn: kotlin.String,

  @get:JsonProperty("name") val name: kotlin.String? = null,
)
