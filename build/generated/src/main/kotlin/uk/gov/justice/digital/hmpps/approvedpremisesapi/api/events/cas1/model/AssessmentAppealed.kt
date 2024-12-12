package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param applicationId The UUID of an application for an AP place
 * @param applicationUrl The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings
 * @param appealId The UUID of an appeal for an application
 * @param appealUrl The URL on the Approved Premises service at which a user can view a representation of an appeal and related resources
 * @param personReference 
 * @param deliusEventNumber Used in Delius to identify the 'event' via the first active conviction's 'index'
 * @param createdAt 
 * @param createdBy 
 * @param appealDetail 
 * @param decision 
 * @param decisionDetail 
 */
data class AssessmentAppealed(

    @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
    @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

    @Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
    @get:JsonProperty("applicationUrl", required = true) val applicationUrl: kotlin.String,

    @Schema(example = "dd450bbc-162d-4380-a103-9f261943b98f", required = true, description = "The UUID of an appeal for an application")
    @get:JsonProperty("appealId", required = true) val appealId: java.util.UUID,

    @Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713/appeals/dd450bbc-162d-4380-a103-9f261943b98f", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an appeal and related resources")
    @get:JsonProperty("appealUrl", required = true) val appealUrl: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

    @Schema(example = "7", required = true, description = "Used in Delius to identify the 'event' via the first active conviction's 'index'")
    @get:JsonProperty("deliusEventNumber", required = true) val deliusEventNumber: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdBy", required = true) val createdBy: StaffMember,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("appealDetail", required = true) val appealDetail: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("decision", required = true) val decision: AppealDecision,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("decisionDetail", required = true) val decisionDetail: kotlin.String
) {

}

