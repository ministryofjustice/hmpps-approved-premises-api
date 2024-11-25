package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param apCode
 * @param probationRegion
 * @param apArea
 */
data class ApprovedPremisesSummary(

  @Schema(example = "NEHOPE1", required = true, description = "")
  @get:JsonProperty("apCode", required = true) val apCode: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("probationRegion", required = true) val probationRegion: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("apArea", required = true) val apArea: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("service", required = true) override val service: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @Schema(example = "Hope House", required = true, description = "")
  @get:JsonProperty("name", required = true) override val name: kotlin.String,

  @Schema(example = "one something street", required = true, description = "")
  @get:JsonProperty("addressLine1", required = true) override val addressLine1: kotlin.String,

  @Schema(example = "LS1 3AD", required = true, description = "")
  @get:JsonProperty("postcode", required = true) override val postcode: kotlin.String,

  @Schema(example = "22", required = true, description = "")
  @get:JsonProperty("bedCount", required = true) override val bedCount: kotlin.Int,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("status", required = true) override val status: PropertyStatus,

  @Schema(example = "Blackmore End", description = "")
  @get:JsonProperty("addressLine2") override val addressLine2: kotlin.String? = null,
) : PremisesSummary
