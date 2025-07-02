package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

/**
 * 
 * @param id 
 * @param bookingId 
 * @param dateTime 
 * @param createdAt 
 * @param notes 
 */
data class Cas3Confirmation(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("bookingId", required = true) val bookingId: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("dateTime", required = true) val dateTime: Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

    @Schema(example = "null", description = "")
    @get:JsonProperty("notes") val notes: String? = null
    ) {

}

