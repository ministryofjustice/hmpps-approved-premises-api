package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param gender 
 * @param type 
 * @param location 
 * @param radius 
 * @param essentialCriteria 
 * @param desirableCriteria 
 */
data class PlacementRequirements(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("gender", required = true) val gender: Gender,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) val type: ApType,

    @Schema(example = "B74", required = true, description = "")
    @get:JsonProperty("location", required = true) val location: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("radius", required = true) val radius: kotlin.Int,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("essentialCriteria", required = true) val essentialCriteria: kotlin.collections.List<PlacementCriteria>,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("desirableCriteria", required = true) val desirableCriteria: kotlin.collections.List<PlacementCriteria>
) {

}

