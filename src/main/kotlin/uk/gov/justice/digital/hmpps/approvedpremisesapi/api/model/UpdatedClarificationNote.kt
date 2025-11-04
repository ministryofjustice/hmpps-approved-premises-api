package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param response
 * @param responseReceivedOn
 */
data class UpdatedClarificationNote(

  val response: kotlin.String,

  @Schema(example = "Thu Jul 28 01:00:00 BST 2022", required = true, description = "")
  val responseReceivedOn: java.time.LocalDate,
)
