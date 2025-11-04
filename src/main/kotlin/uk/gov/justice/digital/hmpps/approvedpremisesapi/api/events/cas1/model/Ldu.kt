package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import io.swagger.v3.oas.annotations.media.Schema

data class Ldu(

  @Schema(example = "N54PPU", required = true, description = "")
  val code: kotlin.String,

  @Schema(example = "Public Protection NE", required = true, description = "")
  val name: kotlin.String,
)
