package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2StatusUpdateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ExternalUser
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param name 
 * @param label 
 * @param description 
 * @param updatedBy 
 * @param updatedAt 
 * @param statusUpdateDetails 
 */
data class Cas2StatusUpdate(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @Schema(example = "moreInfoRequested", required = true, description = "")
    @get:JsonProperty("name", required = true) val name: kotlin.String,

    @Schema(example = "More information requested", required = true, description = "")
    @get:JsonProperty("label", required = true) val label: kotlin.String,

    @Schema(example = "More information about the application has been requested from the POM (Prison Offender Manager).", required = true, description = "")
    @get:JsonProperty("description", required = true) val description: kotlin.String,

    @Schema(example = "null", description = "")
    @get:JsonProperty("updatedBy") val updatedBy: ExternalUser? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("updatedAt") val updatedAt: java.time.Instant? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("statusUpdateDetails") val statusUpdateDetails: kotlin.collections.List<Cas2StatusUpdateDetail>? = null
) {

}

