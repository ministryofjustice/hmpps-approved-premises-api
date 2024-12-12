package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NonArrivalReason
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param bookingId 
 * @param date 
 * @param reason 
 * @param createdAt 
 * @param notes 
 */
data class Nonarrival(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("date", required = true) val date: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("reason", required = true) val reason: NonArrivalReason,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

    @Schema(example = "null", description = "")
    @get:JsonProperty("notes") val notes: kotlin.String? = null
) {

}

