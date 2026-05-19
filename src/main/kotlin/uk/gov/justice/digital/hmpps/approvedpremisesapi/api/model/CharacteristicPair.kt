package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class CharacteristicPair(

  @get:JsonProperty("name", required = true) val name: String,

  @get:JsonProperty("propertyName") val propertyName: String? = null,
)
