package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import io.swagger.v3.oas.annotations.media.Schema

data class ExternalUser(

  @Schema(example = "CAS2_ASSESSOR_USER", required = true, description = "")
  val username: kotlin.String,

  @Schema(example = "Roger Smith", required = true, description = "")
  val name: kotlin.String,

  @Schema(example = "roger@external.example.com", required = true, description = "")
  val email: kotlin.String,

  @Schema(example = "NACRO", description = "")
  val origin: kotlin.String? = null,
)
