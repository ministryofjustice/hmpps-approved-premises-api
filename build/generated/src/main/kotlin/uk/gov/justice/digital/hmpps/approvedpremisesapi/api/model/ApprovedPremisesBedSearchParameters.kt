package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param postcodeDistrict The postcode district to search outwards from
 * @param maxDistanceMiles Maximum number of miles from the postcode district to search, only required if more than 50 miles which is the default
 * @param requiredCharacteristics 
 */
data class ApprovedPremisesBedSearchParameters(

    @Schema(example = "null", required = true, description = "The postcode district to search outwards from")
    @get:JsonProperty("postcodeDistrict", required = true) val postcodeDistrict: kotlin.String,

    @Schema(example = "null", required = true, description = "Maximum number of miles from the postcode district to search, only required if more than 50 miles which is the default")
    @get:JsonProperty("maxDistanceMiles", required = true) val maxDistanceMiles: kotlin.Int,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("requiredCharacteristics", required = true) val requiredCharacteristics: kotlin.collections.List<PlacementCriteria>,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("serviceName", required = true) override val serviceName: kotlin.String,

    @Schema(example = "null", required = true, description = "The date the Bed will need to be free from")
    @get:JsonProperty("startDate", required = true) override val startDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "The number of days the Bed will need to be free from the start_date until")
    @get:JsonProperty("durationDays", required = true) override val durationDays: kotlin.Int
) : BedSearchParameters{

}

