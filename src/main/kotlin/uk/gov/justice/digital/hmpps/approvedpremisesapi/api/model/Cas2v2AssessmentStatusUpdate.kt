package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class Cas2v2AssessmentStatusUpdate(

  @Schema(example = "moreInfoRequired", required = true, description = "The \"name\" of the new status to be applied")
  val newStatus: String,

  val newStatusDetails: List<String>? = null,
)
