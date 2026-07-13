package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import io.swagger.v3.oas.annotations.media.Schema

data class Cas1NewApplication(
  val crn: String,

  @Schema(example = "1502724704")
  val convictionId: Long,

  @Schema(example = "7")
  val deliusEventNumber: String,

  @Schema(example = "M1502750438")
  val offenceId: String,
)
