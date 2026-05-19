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

  @get:JsonProperty("addressLine1", required = true) val addressLine1: String,

  @get:JsonProperty("postcode", required = true) val postcode: String,

  @get:JsonProperty("region", required = true) val region: String,

  @get:JsonProperty("addressLine2") val addressLine2: String? = null,

  @get:JsonProperty("town") val town: String? = null,
)
