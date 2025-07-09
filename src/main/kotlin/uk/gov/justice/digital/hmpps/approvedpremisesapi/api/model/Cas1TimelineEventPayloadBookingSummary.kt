package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param bookingId 
 * @param premises 
 * @param arrivalDate 
 * @param departureDate 
 */
data class Cas1TimelineEventPayloadBookingSummary(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("premises", required = true) val premises: NamedId,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("arrivalDate", required = true) val arrivalDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("departureDate", required = true) val departureDate: java.time.LocalDate
    ) {

}

