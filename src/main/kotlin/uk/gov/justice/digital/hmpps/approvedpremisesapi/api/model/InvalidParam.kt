package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class InvalidParam(

  @Schema(example = "arrivalDate", description = "")
  @get:JsonProperty("propertyName") val propertyName: kotlin.String? = null,

  @get:JsonProperty("errorType") val errorType: kotlin.String? = null,
)
