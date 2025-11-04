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

  val addressLine1: kotlin.String,

  val postcode: kotlin.String,

  val region: kotlin.String,

  val addressLine2: kotlin.String? = null,

  val town: kotlin.String? = null,
)
