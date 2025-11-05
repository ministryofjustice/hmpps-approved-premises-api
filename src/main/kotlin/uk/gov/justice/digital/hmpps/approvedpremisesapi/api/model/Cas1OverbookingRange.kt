package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Cas1OverbookingRange(

  @field:Schema(example = "Thu Jul 28 01:00:00 BST 2022", required = true, description = "")
  @get:JsonProperty("startInclusive", required = true) val startInclusive: java.time.LocalDate,

  @field:Schema(example = "Fri Jul 29 01:00:00 BST 2022", required = true, description = "This can be the same as the start date if overbooked for one night")
  @get:JsonProperty("endInclusive", required = true) val endInclusive: java.time.LocalDate,
)
