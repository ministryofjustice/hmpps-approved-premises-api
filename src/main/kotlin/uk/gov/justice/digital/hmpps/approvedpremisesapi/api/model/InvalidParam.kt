package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class InvalidParam(

  @Schema(example = "arrivalDate", description = "")
  val propertyName: kotlin.String? = null,

  val errorType: kotlin.String? = null,
)
