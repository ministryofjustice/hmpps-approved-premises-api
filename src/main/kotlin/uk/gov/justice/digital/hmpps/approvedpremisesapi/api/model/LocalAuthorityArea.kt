package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class LocalAuthorityArea(

  @Schema(example = "6abb5fa3-e93f-4445-887b-30d081688f44", required = true, description = "")
  val id: java.util.UUID,

  @Schema(example = "LEEDS", required = true, description = "")
  val identifier: kotlin.String,

  @Schema(example = "Leeds City Council", required = true, description = "")
  val name: kotlin.String,
)
