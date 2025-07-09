package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param type 
 * @param createdAt 
 * @param requestReason 
 * @param requestJson Any object
 * @param spaceBookingId 
 * @param updatedAt 
 * @param decision 
 * @param decisionJson Any object
 * @param rejectionReason 
 */
data class Cas1ChangeRequest(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) val type: Cas1ChangeRequestType,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("requestReason", required = true) val requestReason: NamedId,

    @Schema(example = "null", required = true, description = "Any object")
    @get:JsonProperty("requestJson", required = true) val requestJson: kotlin.Any,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("spaceBookingId", required = true) val spaceBookingId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("updatedAt", required = true) val updatedAt: java.time.Instant,

    @Schema(example = "null", description = "")
    @get:JsonProperty("decision") val decision: Cas1ChangeRequestDecision? = null,

    @Schema(example = "null", description = "Any object")
    @get:JsonProperty("decisionJson") val decisionJson: kotlin.Any? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("rejectionReason") val rejectionReason: NamedId? = null
    ) {

}

