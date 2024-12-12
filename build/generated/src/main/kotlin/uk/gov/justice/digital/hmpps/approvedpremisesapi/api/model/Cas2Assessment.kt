package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2StatusUpdate
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param nacroReferralId 
 * @param assessorName 
 * @param statusUpdates 
 */
data class Cas2Assessment(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @Schema(example = "null", description = "")
    @get:JsonProperty("nacroReferralId") val nacroReferralId: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("assessorName") val assessorName: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("statusUpdates") val statusUpdates: kotlin.collections.List<Cas2StatusUpdate>? = null
) {

}

