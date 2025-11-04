package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param id
 * @param name
 * @param addressLine1
 * @param postcode
 * @param addressLine2
 * @param town
 */
data class BookingSearchResultPremisesSummary(

  val id: java.util.UUID,

  val name: kotlin.String,

  val addressLine1: kotlin.String,

  val postcode: kotlin.String,

  val addressLine2: kotlin.String? = null,

  val town: kotlin.String? = null,
)
