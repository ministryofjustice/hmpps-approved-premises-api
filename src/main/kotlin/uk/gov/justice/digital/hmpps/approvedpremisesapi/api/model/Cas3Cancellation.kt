package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

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
data class Cas3Cancellation(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("bookingId", required = true) val bookingId: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("date", required = true) val date: LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("reason", required = true) val reason: CancellationReason,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("premisesName", required = true) val premisesName: String,

    @Schema(example = "null", description = "")
    @get:JsonProperty("id") val id: UUID? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("notes") val notes: String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("otherReason") val otherReason: String? = null
    ) {

}

