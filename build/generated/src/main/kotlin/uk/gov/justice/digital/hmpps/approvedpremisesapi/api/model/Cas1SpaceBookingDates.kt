package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param canonicalArrivalDate actual arrival date or, if not known, the expected arrival date
 * @param canonicalDepartureDate actual departure date or, if not known, the expected departure date
 */
data class Cas1SpaceBookingDates(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @Schema(example = "null", required = true, description = "actual arrival date or, if not known, the expected arrival date")
    @get:JsonProperty("canonicalArrivalDate", required = true) val canonicalArrivalDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "actual departure date or, if not known, the expected departure date")
    @get:JsonProperty("canonicalDepartureDate", required = true) val canonicalDepartureDate: java.time.LocalDate
) {

}

