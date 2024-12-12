package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param applicationId The UUID of an application for an AP place
 * @param applicationUrl The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings
 * @param personReference 
 * @param deliusEventNumber Used in Delius to identify the 'event' via the first active conviction's 'index'
 * @param requestForPlacementId The UUID of a request for placement. Currently a proxy for PlacementApplicationId
 * @param createdAt 
 * @param expectedArrival 
 * @param duration 
 * @param requestForPlacementType 
 * @param createdBy 
 */
data class RequestForPlacementCreated(

    @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
    @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

    @Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
    @get:JsonProperty("applicationUrl", required = true) val applicationUrl: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

    @Schema(example = "7", required = true, description = "Used in Delius to identify the 'event' via the first active conviction's 'index'")
    @get:JsonProperty("deliusEventNumber", required = true) val deliusEventNumber: kotlin.String,

    @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of a request for placement. Currently a proxy for PlacementApplicationId")
    @get:JsonProperty("requestForPlacementId", required = true) val requestForPlacementId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

    @Schema(example = "Mon Jan 30 00:00:00 GMT 2023", required = true, description = "")
    @get:JsonProperty("expectedArrival", required = true) val expectedArrival: java.time.LocalDate,

    @Schema(example = "7", required = true, description = "")
    @get:JsonProperty("duration", required = true) val duration: kotlin.Int,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("requestForPlacementType", required = true) val requestForPlacementType: RequestForPlacementType,

    @Schema(example = "null", description = "")
    @get:JsonProperty("createdBy") val createdBy: StaffMember? = null
) {

}

