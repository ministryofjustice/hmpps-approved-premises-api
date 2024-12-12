package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CancellationReason
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param bookingId 
 * @param date 
 * @param reason 
 * @param createdAt 
 * @param premisesName 
 * @param id 
 * @param notes 
 * @param otherReason 
 */
data class Cancellation(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("date", required = true) val date: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("reason", required = true) val reason: CancellationReason,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("premisesName", required = true) val premisesName: kotlin.String,

    @Schema(example = "null", description = "")
    @get:JsonProperty("id") val id: java.util.UUID? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("notes") val notes: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("otherReason") val otherReason: kotlin.String? = null
) {

}

