package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param name
 * @param propertyName
 */
data class CharacteristicPair(

  val name: kotlin.String,

  val propertyName: kotlin.String? = null,
)
