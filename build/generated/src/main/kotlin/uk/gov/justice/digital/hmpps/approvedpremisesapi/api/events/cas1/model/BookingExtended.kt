package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param personReference 
 * @param deliusEventNumber Used in Delius to identify the 'event' via the first active conviction's 'index'
 * @param applicationId The UUID of an application for an AP place
 * @param applicationUrl The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings
 * @param bookingId The UUID of booking for an AP place
 * @param premises 
 * @param extendedAt 
 * @param extendedBy 
 * @param extensionReason 
 * @param previousDepartureOn 
 * @param newDepartureOn 
 */
data class BookingExtended(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

    @Schema(example = "7", required = true, description = "Used in Delius to identify the 'event' via the first active conviction's 'index'")
    @get:JsonProperty("deliusEventNumber", required = true) val deliusEventNumber: kotlin.String,

    @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
    @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

    @Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
    @get:JsonProperty("applicationUrl", required = true) val applicationUrl: kotlin.String,

    @Schema(example = "14c80733-4b6d-4f35-b724-66955aac320c", required = true, description = "The UUID of booking for an AP place")
    @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("premises", required = true) val premises: Premises,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("extendedAt", required = true) val extendedAt: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("extendedBy", required = true) val extendedBy: StaffMember,

    @Schema(example = "There has been a flood at the housing association to which Mr Smith is moving", required = true, description = "")
    @get:JsonProperty("extensionReason", required = true) val extensionReason: kotlin.String,

    @Schema(example = "Mon Jan 30 00:00:00 GMT 2023", required = true, description = "")
    @get:JsonProperty("previousDepartureOn", required = true) val previousDepartureOn: java.time.LocalDate,

    @Schema(example = "Sun Feb 12 00:00:00 GMT 2023", required = true, description = "")
    @get:JsonProperty("newDepartureOn", required = true) val newDepartureOn: java.time.LocalDate
) {

}

