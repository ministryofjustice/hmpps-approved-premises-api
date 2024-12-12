package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param confirmedAt 
 * @param reason 
 * @param notes 
 */
data class Cas1SpaceBookingNonArrival(

    @Schema(example = "null", description = "")
    @get:JsonProperty("confirmedAt") val confirmedAt: java.time.Instant? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("reason") val reason: NamedId? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("notes") val notes: kotlin.String? = null
) {

}

