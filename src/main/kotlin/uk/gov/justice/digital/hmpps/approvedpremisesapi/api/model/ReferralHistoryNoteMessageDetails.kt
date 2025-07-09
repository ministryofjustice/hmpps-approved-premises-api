package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param rejectionReason 
 * @param rejectionReasonDetails 
 * @param isWithdrawn 
 * @param domainEvent Any object
 */
data class ReferralHistoryNoteMessageDetails(

    @Schema(example = "null", description = "")
    @get:JsonProperty("rejectionReason") val rejectionReason: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("rejectionReasonDetails") val rejectionReasonDetails: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("isWithdrawn") val isWithdrawn: kotlin.Boolean? = null,

    @Schema(example = "null", description = "Any object")
    @get:JsonProperty("domainEvent") val domainEvent: kotlin.Any? = null
    ) {

}

