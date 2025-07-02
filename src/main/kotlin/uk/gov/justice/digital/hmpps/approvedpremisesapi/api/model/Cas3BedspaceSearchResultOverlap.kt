package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 * 
 * @param name 
 * @param crn 
 * @param personType 
 * @param days 
 * @param bookingId 
 * @param roomId 
 * @param isSexualRisk 
 * @param sex 
 * @param assessmentId 
 */
data class Cas3BedspaceSearchResultOverlap(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("name", required = true) val name: String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("crn", required = true) val crn: String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("personType", required = true) val personType: PersonType,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("days", required = true) val days: Int,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("bookingId", required = true) val bookingId: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("roomId", required = true) val roomId: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("isSexualRisk", required = true) val isSexualRisk: Boolean,

    @Schema(example = "null", description = "")
    @get:JsonProperty("sex") val sex: String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("assessmentId") val assessmentId: UUID? = null
    ) {

}

