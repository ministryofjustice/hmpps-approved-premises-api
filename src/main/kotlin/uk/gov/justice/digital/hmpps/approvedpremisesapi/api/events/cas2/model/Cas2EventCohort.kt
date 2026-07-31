package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import io.swagger.v3.oas.annotations.media.Schema

data class Cas2EventCohort(

  @Schema(example = "HDC", required = true, description = "The machine readable code identifying the cohort")
  val code: String,

  @Schema(example = "Home Detention Curfew", required = true, description = "The human readable name of the cohort")
  val longDisplayName: String,
)
