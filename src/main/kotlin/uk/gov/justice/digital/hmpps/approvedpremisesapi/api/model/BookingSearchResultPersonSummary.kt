package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param crn
 * @param name
 */
data class BookingSearchResultPersonSummary(

  val crn: kotlin.String,

  val name: kotlin.String? = null,
)
