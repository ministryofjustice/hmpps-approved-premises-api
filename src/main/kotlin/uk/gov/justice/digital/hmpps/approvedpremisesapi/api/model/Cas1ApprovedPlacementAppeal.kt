package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param occurredAt 
 * @param placementAppealChangeRequestId 
 * @param reasonNotes 
 */
data class Cas1ApprovedPlacementAppeal(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("occurredAt", required = true) val occurredAt: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("placementAppealChangeRequestId", required = true) val placementAppealChangeRequestId: java.util.UUID,

    @Schema(example = "null", description = "")
    @get:JsonProperty("reasonNotes") val reasonNotes: kotlin.String? = null
    ) {

}

