package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import io.swagger.v3.oas.annotations.media.Schema

data class Problem(

  @Schema(example = "https://example.net/validation-error", description = "")
  val type: kotlin.String? = null,

  @Schema(example = "Invalid request parameters", description = "")
  val title: kotlin.String? = null,

  @Schema(example = "400", description = "")
  val status: kotlin.Int? = null,

  @Schema(example = "You provided invalid request parameters", description = "")
  val detail: kotlin.String? = null,

  @Schema(example = "f7493e12-546d-42c3-b838-06c12671ab5b", description = "")
  val instance: kotlin.String? = null,
)
