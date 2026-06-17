package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class Cas1PremiseCapacity(

  @get:JsonProperty("startDate", required = true) val startDate: LocalDate,

  @get:JsonProperty("endDate", required = true) val endDate: LocalDate,

  @Schema(example = "null", required = true, description = "Capacity for each day, returning chronologically (oldest first)")
  @get:JsonProperty("capacity", required = true) val capacity: List<Cas1PremiseCapacityForDay>,
)
