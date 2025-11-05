package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Cas1PremiseCapacity(

  @get:JsonProperty("startDate", required = true) val startDate: java.time.LocalDate,

  @get:JsonProperty("endDate", required = true) val endDate: java.time.LocalDate,

  @field:Schema(example = "null", required = true, description = "Capacity for each day, returning chronologically (oldest first)")
  @get:JsonProperty("capacity", required = true) val capacity: kotlin.collections.List<Cas1PremiseCapacityForDay>,
)
