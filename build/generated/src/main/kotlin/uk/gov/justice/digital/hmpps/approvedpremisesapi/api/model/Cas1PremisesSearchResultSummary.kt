package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CharacteristicPair
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param apCode 
 * @param deliusQCode 
 * @param apType 
 * @param name 
 * @param addressLine1 
 * @param addressLine2 
 * @param town 
 * @param postcode 
 * @param apArea 
 * @param totalSpaceCount The total number of spaces in this premises
 * @param premisesCharacteristics 
 */
data class Cas1PremisesSearchResultSummary(

    @Schema(example = "null", description = "")
    @get:JsonProperty("id") val id: java.util.UUID? = null,

    @Schema(example = "NEHOPE1", description = "")
    @get:JsonProperty("apCode") val apCode: kotlin.String? = null,

    @Schema(example = "Q005", description = "")
    @get:JsonProperty("deliusQCode") val deliusQCode: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("apType") val apType: ApType? = null,

    @Schema(example = "Hope House", description = "")
    @get:JsonProperty("name") val name: kotlin.String? = null,

    @Schema(example = "1 The Street", description = "")
    @get:JsonProperty("addressLine1") val addressLine1: kotlin.String? = null,

    @Schema(example = "Blackmore End", description = "")
    @get:JsonProperty("addressLine2") val addressLine2: kotlin.String? = null,

    @Schema(example = "Braintree", description = "")
    @get:JsonProperty("town") val town: kotlin.String? = null,

    @Schema(example = "LS1 3AD", description = "")
    @get:JsonProperty("postcode") val postcode: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("apArea") val apArea: NamedId? = null,

    @Schema(example = "22", description = "The total number of spaces in this premises")
    @get:JsonProperty("totalSpaceCount") val totalSpaceCount: kotlin.Int? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("premisesCharacteristics") val premisesCharacteristics: kotlin.collections.List<CharacteristicPair>? = null
) {

}

