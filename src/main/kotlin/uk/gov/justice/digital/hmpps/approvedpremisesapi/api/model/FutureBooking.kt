package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

/**
 * 
 * @param id 
 * @param person 
 * @param arrivalDate 
 * @param departureDate 
 * @param bed 
 */
data class FutureBooking(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("person", required = true) val person: Person,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("arrivalDate", required = true) val arrivalDate: LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("departureDate", required = true) val departureDate: LocalDate,

    @Schema(example = "null", description = "")
    @get:JsonProperty("bed") val bed: Bed? = null
    ) {

}

