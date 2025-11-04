package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import io.swagger.v3.oas.annotations.media.Schema

data class Cas2StatusDetail(

  @Schema(example = "changeOfCircumstances", required = true, description = "")
  val name: kotlin.String,

  @Schema(example = "Change of Circumstances", required = true, description = "")
  val label: kotlin.String,
)
