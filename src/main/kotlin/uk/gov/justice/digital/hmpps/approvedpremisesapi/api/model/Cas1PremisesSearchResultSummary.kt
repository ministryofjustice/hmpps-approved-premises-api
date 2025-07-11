package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param apType
 * @param name
 * @param fullAddress Full address, excluding postcode
 * @param apArea
 * @param characteristics Room and premise characteristics
 * @param postcode
 */
data class Cas1PremisesSearchResultSummary(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("apType", required = true) val apType: ApType,

  @Schema(example = "Hope House", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @Schema(example = "null", required = true, description = "Full address, excluding postcode")
  @get:JsonProperty("fullAddress", required = true) val fullAddress: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("apArea", required = true) val apArea: NamedId,

  @Schema(example = "null", required = true, description = "Room and premise characteristics")
  @get:JsonProperty("characteristics", required = true) val characteristics: kotlin.collections.List<Cas1SpaceCharacteristic>,

  @Schema(example = "LS1 3AD", description = "")
  @get:JsonProperty("postcode") val postcode: kotlin.String? = null,
)
