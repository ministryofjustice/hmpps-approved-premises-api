package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param premisesName 
 * @param arrivalDate 
 * @param departureDate 
 */
data class NewPlacementRequestBookingConfirmation(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("premisesName", required = true) val premisesName: kotlin.String,

    @Schema(example = "Thu Jul 28 01:00:00 BST 2022", required = true, description = "")
    @get:JsonProperty("arrivalDate", required = true) val arrivalDate: java.time.LocalDate,

    @Schema(example = "Fri Sep 30 01:00:00 BST 2022", required = true, description = "")
    @get:JsonProperty("departureDate", required = true) val departureDate: java.time.LocalDate
) {

}

