package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResult
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param resultsRoomCount How many distinct Rooms the Beds in the results belong to
 * @param resultsPremisesCount How many distinct Premises the Beds in the results belong to
 * @param resultsBedCount How many Beds are in the results
 * @param results 
 */
data class BedSearchResults(

    @Schema(example = "null", required = true, description = "How many distinct Rooms the Beds in the results belong to")
    @get:JsonProperty("resultsRoomCount", required = true) val resultsRoomCount: kotlin.Int,

    @Schema(example = "null", required = true, description = "How many distinct Premises the Beds in the results belong to")
    @get:JsonProperty("resultsPremisesCount", required = true) val resultsPremisesCount: kotlin.Int,

    @Schema(example = "null", required = true, description = "How many Beds are in the results")
    @get:JsonProperty("resultsBedCount", required = true) val resultsBedCount: kotlin.Int,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("results", required = true) val results: kotlin.collections.List<BedSearchResult>
) {

}

