package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param crn 
 * @param arrivalDate 
 * @param departureDate 
 * @param serviceName 
 * @param bedId 
 * @param enableTurnarounds 
 * @param assessmentId 
 * @param eventNumber 
 */
data class NewBooking(

    @Schema(example = "A123456", required = true, description = "")
    @get:JsonProperty("crn", required = true) val crn: kotlin.String,

    @Schema(example = "Thu Jul 28 01:00:00 BST 2022", required = true, description = "")
    @get:JsonProperty("arrivalDate", required = true) val arrivalDate: java.time.LocalDate,

    @Schema(example = "Fri Sep 30 01:00:00 BST 2022", required = true, description = "")
    @get:JsonProperty("departureDate", required = true) val departureDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("serviceName", required = true) val serviceName: ServiceName,

    @Schema(example = "null", description = "")
    @get:JsonProperty("bedId") val bedId: java.util.UUID? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("enableTurnarounds") val enableTurnarounds: kotlin.Boolean? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("assessmentId") val assessmentId: java.util.UUID? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("eventNumber") val eventNumber: kotlin.String? = null
) {

}

