package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class Cas1UpdatedClarificationNote(

  val response: kotlin.String,

  @Schema(example = "Thu Jul 28 01:00:00 BST 2022", required = true, description = "")
  val responseReceivedOn: java.time.LocalDate,
)
