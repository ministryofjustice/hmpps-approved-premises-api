package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.PersonReference
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param personReference 
 * @param applicationId 
 * @param applicationUrl 
 */
data class CAS3ReferralSubmittedEventDetails(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("applicationUrl", required = true) val applicationUrl: java.net.URI
) {

}

