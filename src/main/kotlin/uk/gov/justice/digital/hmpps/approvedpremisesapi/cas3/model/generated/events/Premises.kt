package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param addressLine1
 * @param postcode
 * @param region
 * @param addressLine2
 * @param town
 */
data class Premises(

  @get:JsonProperty("addressLine1", required = true) val addressLine1: kotlin.String,

  @get:JsonProperty("postcode", required = true) val postcode: kotlin.String,

  @get:JsonProperty("region", required = true) val region: kotlin.String,

  @get:JsonProperty("addressLine2") val addressLine2: kotlin.String? = null,

  @get:JsonProperty("town") val town: kotlin.String? = null,
)
