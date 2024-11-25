package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param date
 * @param availableBeds
 */
data class DateCapacity(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("date", required = true) val date: java.time.LocalDate,

  @Schema(example = "10", required = true, description = "")
  @get:JsonProperty("availableBeds", required = true) val availableBeds: kotlin.Int,
)
