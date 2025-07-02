package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * 
 * @param id 
 * @param person 
 * @param arrivalDate 
 * @param originalArrivalDate 
 * @param departureDate 
 * @param originalDepartureDate 
 * @param createdAt 
 * @param bedspace 
 */
data class Cas3BookingBody(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("person", required = true) val person: Person,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("arrivalDate", required = true) val arrivalDate: LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("originalArrivalDate", required = true) val originalArrivalDate: LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("departureDate", required = true) val departureDate: LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("originalDepartureDate", required = true) val originalDepartureDate: LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("bedspace", required = true) val bedspace: Cas3Bedspace
    ) {

}

