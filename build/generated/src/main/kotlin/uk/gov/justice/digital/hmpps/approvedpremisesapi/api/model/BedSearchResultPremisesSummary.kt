package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CharacteristicPair
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param name 
 * @param addressLine1 
 * @param postcode 
 * @param characteristics 
 * @param bedCount the total number of Beds in the Premises
 * @param addressLine2 
 * @param town 
 * @param probationDeliveryUnitName 
 * @param notes 
 * @param bookedBedCount the total number of booked Beds in the Premises
 */
data class BedSearchResultPremisesSummary(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("name", required = true) val name: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("addressLine1", required = true) val addressLine1: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("postcode", required = true) val postcode: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("characteristics", required = true) val characteristics: kotlin.collections.List<CharacteristicPair>,

    @Schema(example = "null", required = true, description = "the total number of Beds in the Premises")
    @get:JsonProperty("bedCount", required = true) val bedCount: kotlin.Int,

    @Schema(example = "null", description = "")
    @get:JsonProperty("addressLine2") val addressLine2: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("town") val town: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("probationDeliveryUnitName") val probationDeliveryUnitName: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("notes") val notes: kotlin.String? = null,

    @Schema(example = "null", description = "the total number of booked Beds in the Premises")
    @get:JsonProperty("bookedBedCount") val bookedBedCount: kotlin.Int? = null
) {

}

