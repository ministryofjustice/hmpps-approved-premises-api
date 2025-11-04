package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import io.swagger.v3.oas.annotations.media.Schema

data class Team(

  @Schema(example = "N54NGH", required = true, description = "")
  val code: kotlin.String,

  @Schema(example = "Gateshead 1", required = true, description = "")
  val name: kotlin.String,
)
