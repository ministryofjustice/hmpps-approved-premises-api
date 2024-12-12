package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param reason 
 * @param parentReason 
 * @param moveOnCategory 
 * @param notes 
 */
data class Cas1SpaceBookingDeparture(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("reason", required = true) val reason: NamedId,

    @Schema(example = "null", description = "")
    @get:JsonProperty("parentReason") val parentReason: NamedId? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("moveOnCategory") val moveOnCategory: NamedId? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("notes") val notes: kotlin.String? = null
) {

}

