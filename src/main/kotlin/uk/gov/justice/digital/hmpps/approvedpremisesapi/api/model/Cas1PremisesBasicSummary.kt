package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Cas1PremisesBasicSummary(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "Hope House", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: String,

  @get:JsonProperty("apArea", required = true) val apArea: NamedId,

  @Schema(example = "22", required = true, description = "")
  @get:JsonProperty("bedCount", required = true) val bedCount: Int,

  @get:JsonProperty("supportsSpaceBookings", required = true) val supportsSpaceBookings: Boolean,

  @Schema(example = "null", required = true, description = "Full address, excluding postcode")
  @get:JsonProperty("fullAddress", required = true) val fullAddress: String,

  @get:JsonProperty("postcode", required = true) val postcode: String,

  @Schema(example = "NEHOPE1", description = "")
  @get:JsonProperty("apCode") val apCode: String? = null,
)
