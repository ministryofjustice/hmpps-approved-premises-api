package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedStatus
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param startDate 
 * @param endDate 
 * @param bedId 
 * @param bedName 
 * @param roomName 
 * @param reason 
 * @param status 
 * @param referenceNumber 
 * @param notes 
 * @param cancellation 
 */
data class LostBed(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("startDate", required = true) val startDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("endDate", required = true) val endDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("bedId", required = true) val bedId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("bedName", required = true) val bedName: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("roomName", required = true) val roomName: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("reason", required = true) val reason: LostBedReason,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("status", required = true) val status: LostBedStatus,

    @Schema(example = "null", description = "")
    @get:JsonProperty("referenceNumber") val referenceNumber: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("notes") val notes: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("cancellation") val cancellation: LostBedCancellation? = null
) {

}

