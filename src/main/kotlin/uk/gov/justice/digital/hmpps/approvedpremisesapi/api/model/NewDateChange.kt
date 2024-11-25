package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param newArrivalDate
 * @param newDepartureDate
 */
data class NewDateChange(

  @Schema(example = "null", description = "")
  @get:JsonProperty("newArrivalDate") val newArrivalDate: java.time.LocalDate? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("newDepartureDate") val newDepartureDate: java.time.LocalDate? = null,
)
