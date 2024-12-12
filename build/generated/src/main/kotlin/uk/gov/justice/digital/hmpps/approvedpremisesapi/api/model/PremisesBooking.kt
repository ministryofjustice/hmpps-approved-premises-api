package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Bed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param arrivalDate 
 * @param departureDate 
 * @param person 
 * @param bed 
 * @param status 
 */
data class PremisesBooking(

    @Schema(example = "null", description = "")
    @get:JsonProperty("id") val id: java.util.UUID? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("arrivalDate") val arrivalDate: java.time.LocalDate? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("departureDate") val departureDate: java.time.LocalDate? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("person") val person: Person? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("bed") val bed: Bed? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("status") val status: BookingStatus? = null
) {

}

