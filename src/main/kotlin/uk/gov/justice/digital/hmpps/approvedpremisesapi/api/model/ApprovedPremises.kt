package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param apCode
 */
data class ApprovedPremises(

  @Schema(example = "NEHOPE1", required = true, description = "")
  @get:JsonProperty("apCode", required = true) val apCode: kotlin.String,

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

  @Schema(example = "20", required = true, description = "")
  @get:JsonProperty("availableBedsForToday", required = true) override val availableBedsForToday: kotlin.Int,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("probationRegion", required = true) override val probationRegion: ProbationRegion,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("apArea", required = true) override val apArea: ApArea,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("localAuthorityArea", required = true) override val localAuthorityArea: LocalAuthorityArea,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("status", required = true) override val status: PropertyStatus,

  @Schema(example = "Blackmore End", description = "")
  @get:JsonProperty("addressLine2") override val addressLine2: kotlin.String? = null,

  @Schema(example = "Braintree", description = "")
  @get:JsonProperty("town") override val town: kotlin.String? = null,

  @Schema(example = "some notes about this property", description = "")
  @get:JsonProperty("notes") override val notes: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("characteristics") override val characteristics: kotlin.collections.List<Characteristic>? = null,
) : Premises {
}
