package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingRequirements
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param arrivalDate 
 * @param departureDate 
 * @param premisesId 
 * @param requirements 
 */
data class Cas1NewSpaceBooking(

    @Schema(example = "Thu Jul 28 01:00:00 BST 2022", required = true, description = "")
    @get:JsonProperty("arrivalDate", required = true) val arrivalDate: java.time.LocalDate,

    @Schema(example = "Fri Sep 30 01:00:00 BST 2022", required = true, description = "")
    @get:JsonProperty("departureDate", required = true) val departureDate: java.time.LocalDate,

    @Schema(example = "290fa58c-77b2-47e2-b729-4cd6b2ed1a78", required = true, description = "")
    @get:JsonProperty("premisesId", required = true) val premisesId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("requirements", required = true) val requirements: Cas1SpaceBookingRequirements
) {

}

