package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

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

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @get:JsonProperty("addressLine1", required = true) val addressLine1: kotlin.String,

  @get:JsonProperty("postcode", required = true) val postcode: kotlin.String,

  @get:JsonProperty("addressLine2") val addressLine2: kotlin.String? = null,

  @get:JsonProperty("town") val town: kotlin.String? = null,
)
