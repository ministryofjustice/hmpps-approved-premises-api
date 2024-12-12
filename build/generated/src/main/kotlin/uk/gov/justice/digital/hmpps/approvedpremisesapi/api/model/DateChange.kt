package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param bookingId 
 * @param previousArrivalDate 
 * @param newArrivalDate 
 * @param previousDepartureDate 
 * @param newDepartureDate 
 * @param createdAt 
 */
data class DateChange(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("previousArrivalDate", required = true) val previousArrivalDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("newArrivalDate", required = true) val newArrivalDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("previousDepartureDate", required = true) val previousDepartureDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("newDepartureDate", required = true) val newDepartureDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant
) {

}

