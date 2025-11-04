package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class Cas2v2ApplicationStatusDetail(

  val id: UUID,

  @Schema(example = "changeOfCircumstances", required = true, description = "")
  val name: String,

  @Schema(example = "Change of Circumstances", required = true, description = "")
  val label: String,
)
