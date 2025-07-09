package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param applicationId 
 * @param id If type is 'Additional', provides the PlacementApplication ID. If type is 'Initial' this field provides a PlacementRequest ID.
 * @param createdByUserId 
 * @param createdAt 
 * @param assessmentId If type is 'Additional', provides the PlacementApplication ID. If type is 'Initial' this field shouldn't be used.
 * @param assessmentCompletedAt 
 * @param applicationCompletedAt 
 * @param canBeWithdrawn 
 * @param isWithdrawn 
 * @param type 
 * @param placementDates Deprecated, use dates. Only populated with values after the placement application has been submitted
 * @param submittedAt 
 * @param &#x60;data&#x60; Any object
 * @param document Any object
 * @param withdrawalReason 
 * @param dates 
 */
data class PlacementApplication(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

    @Schema(example = "null", required = true, description = "If type is 'Additional', provides the PlacementApplication ID. If type is 'Initial' this field provides a PlacementRequest ID.")
    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdByUserId", required = true) val createdByUserId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

    @Schema(example = "null", required = true, description = "If type is 'Additional', provides the PlacementApplication ID. If type is 'Initial' this field shouldn't be used.")
    @get:JsonProperty("assessmentId", required = true) val assessmentId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("assessmentCompletedAt", required = true) val assessmentCompletedAt: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("applicationCompletedAt", required = true) val applicationCompletedAt: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("canBeWithdrawn", required = true) val canBeWithdrawn: kotlin.Boolean,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("isWithdrawn", required = true) val isWithdrawn: kotlin.Boolean,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) val type: PlacementApplicationType,

    @Schema(example = "null", required = true, description = "Deprecated, use dates. Only populated with values after the placement application has been submitted")
    @get:JsonProperty("placementDates", required = true) val placementDates: kotlin.collections.List<PlacementDates>,

    @Schema(example = "null", description = "")
    @get:JsonProperty("submittedAt") val submittedAt: java.time.Instant? = null,

    @Schema(example = "null", description = "Any object")
    @get:JsonProperty("data") val `data`: kotlin.Any? = null,

    @Schema(example = "null", description = "Any object")
    @get:JsonProperty("document") val document: kotlin.Any? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("withdrawalReason") val withdrawalReason: WithdrawPlacementRequestReason? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("dates") val dates: PlacementDates? = null
    ) {

}

