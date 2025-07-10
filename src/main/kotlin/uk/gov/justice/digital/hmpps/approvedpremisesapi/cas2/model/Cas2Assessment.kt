package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 * 
 * @param id 
 * @param nacroReferralId 
 * @param assessorName 
 * @param statusUpdates 
 */
data class Cas2Assessment(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: UUID,

    @Schema(example = "null", description = "")
    @get:JsonProperty("nacroReferralId") val nacroReferralId: String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("assessorName") val assessorName: String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("statusUpdates") val statusUpdates: List<Cas2StatusUpdate>? = null
    ) {

}

