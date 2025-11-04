package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class Cas1ExpireApplicationReason(

  @Schema(example = "Application no longer required and superseded.", required = true, description = "Reason for expiring the application")
  val reason: String,
)
