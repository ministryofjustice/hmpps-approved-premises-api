package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param propertyName
 * @param errorType
 */
data class InvalidParam(

  @Schema(example = "arrivalDate", description = "")
  val propertyName: kotlin.String? = null,

  val errorType: kotlin.String? = null,
)
