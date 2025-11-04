package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class Cas1OverbookingRange(

  @Schema(example = "Thu Jul 28 01:00:00 BST 2022", required = true, description = "")
  val startInclusive: java.time.LocalDate,

  @Schema(example = "Fri Jul 29 01:00:00 BST 2022", required = true, description = "This can be the same as the start date if overbooked for one night")
  val endInclusive: java.time.LocalDate,
)
