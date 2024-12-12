package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeBookedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param personReference 
 * @param deliusEventNumber Used in Delius to identify the 'event' via the first active conviction's 'index'
 * @param applicationId The UUID of an application for an AP place
 * @param applicationUrl The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings
 * @param attemptedAt 
 * @param attemptedBy 
 * @param failureDescription 
 */
data class BookingNotMade(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

    @Schema(example = "7", required = true, description = "Used in Delius to identify the 'event' via the first active conviction's 'index'")
    @get:JsonProperty("deliusEventNumber", required = true) val deliusEventNumber: kotlin.String,

    @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
    @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

    @Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
    @get:JsonProperty("applicationUrl", required = true) val applicationUrl: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("attemptedAt", required = true) val attemptedAt: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("attemptedBy", required = true) val attemptedBy: BookingMadeBookedBy,

    @Schema(example = "No availability", description = "")
    @get:JsonProperty("failureDescription") val failureDescription: kotlin.String? = null
) {

}

