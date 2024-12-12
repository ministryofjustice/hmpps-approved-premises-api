package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param arrivalDate 
 * @param departureDate 
 * @param bedId 
 * @param premisesId 
 */
data class NewPlacementRequestBooking(

    @Schema(example = "Thu Jul 28 01:00:00 BST 2022", required = true, description = "")
    @get:JsonProperty("arrivalDate", required = true) val arrivalDate: java.time.LocalDate,

    @Schema(example = "Fri Sep 30 01:00:00 BST 2022", required = true, description = "")
    @get:JsonProperty("departureDate", required = true) val departureDate: java.time.LocalDate,

    @Schema(example = "null", description = "")
    @get:JsonProperty("bedId") val bedId: java.util.UUID? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("premisesId") val premisesId: java.util.UUID? = null
) {

}

