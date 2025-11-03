package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param name
 * @param propertyName
 */
data class CharacteristicPair(

  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @get:JsonProperty("propertyName") val propertyName: kotlin.String? = null,
)
