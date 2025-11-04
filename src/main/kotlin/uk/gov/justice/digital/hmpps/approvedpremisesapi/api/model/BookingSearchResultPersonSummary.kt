package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param crn
 * @param name
 */
data class BookingSearchResultPersonSummary(

  val crn: kotlin.String,

  val name: kotlin.String? = null,
)
