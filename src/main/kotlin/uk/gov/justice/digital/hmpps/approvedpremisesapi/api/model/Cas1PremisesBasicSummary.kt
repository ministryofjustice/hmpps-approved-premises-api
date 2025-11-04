package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class Cas1PremisesBasicSummary(

  val id: java.util.UUID,

  @Schema(example = "Hope House", required = true, description = "")
  val name: kotlin.String,

  val apArea: NamedId,

  @Schema(example = "22", required = true, description = "")
  val bedCount: kotlin.Int,

  val supportsSpaceBookings: kotlin.Boolean,

  @Schema(example = "null", required = true, description = "Full address, excluding postcode")
  val fullAddress: kotlin.String,

  val postcode: kotlin.String,

  @Schema(example = "NEHOPE1", description = "")
  val apCode: kotlin.String? = null,
)
