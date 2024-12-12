package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param translatedDocument Any object that conforms to the current JSON schema for an application
 * @param placementType 
 * @param placementDates 
 */
data class SubmitPlacementApplication(

    @Schema(example = "null", required = true, description = "Any object that conforms to the current JSON schema for an application")
    @get:JsonProperty("translatedDocument", required = true) val translatedDocument: kotlin.Any,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("placementType", required = true) val placementType: PlacementType,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("placementDates", required = true) val placementDates: kotlin.collections.List<PlacementDates>
) {

}

