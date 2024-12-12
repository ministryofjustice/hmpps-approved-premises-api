package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param applicationId The UUID of an application for an AP place
 * @param applicationUrl The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings
 * @param placementApplicationId The UUID of a placement application
 * @param personReference 
 * @param allocatedAt 
 * @param placementDates 
 * @param allocatedTo 
 * @param allocatedBy 
 */
data class PlacementApplicationAllocated(

    @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
    @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

    @Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
    @get:JsonProperty("applicationUrl", required = true) val applicationUrl: kotlin.String,

    @Schema(example = "14c80733-4b6d-4f35-b724-66955aac320c", required = true, description = "The UUID of a placement application")
    @get:JsonProperty("placementApplicationId", required = true) val placementApplicationId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("allocatedAt", required = true) val allocatedAt: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("placementDates", required = true) val placementDates: kotlin.collections.List<DatePeriod>,

    @Schema(example = "null", description = "")
    @get:JsonProperty("allocatedTo") val allocatedTo: StaffMember? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("allocatedBy") val allocatedBy: StaffMember? = null
) {

}

