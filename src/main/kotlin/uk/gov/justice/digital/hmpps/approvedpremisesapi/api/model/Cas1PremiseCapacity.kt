package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class Cas1PremiseCapacity(

  val startDate: java.time.LocalDate,

  val endDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "Capacity for each day, returning chronologically (oldest first)")
  val capacity: kotlin.collections.List<Cas1PremiseCapacityForDay>,
)
