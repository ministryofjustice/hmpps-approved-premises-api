package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param departureDate Updated departure date
 */
data class Cas1ShortenSpaceBooking(

    @Schema(example = "Fri Sep 30 01:00:00 BST 2022", required = true, description = "Updated departure date")
    @get:JsonProperty("departureDate", required = true) val departureDate: java.time.LocalDate
    ) {

}

