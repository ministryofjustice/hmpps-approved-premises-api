package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param startDate
 * @param endDate
 */
data class DatePeriod(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("startDate", required = true) val startDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("endDate", required = true) val endDate: java.time.LocalDate,
)
