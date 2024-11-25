package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param personReference
 * @param deliusEventNumber Used in Delius to identify the 'event' via the first active conviction's 'index'
 * @param applicationId The UUID of an application for an AP place
 * @param applicationUrl The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings
 * @param bookingId The UUID of booking for an AP place
 * @param premises
 * @param cancelledAt cancelledAtDate should be used instead
 * @param cancelledAtDate
 * @param cancelledBy
 * @param cancellationReason
 * @param cancellationRecordedAt
 */
data class BookingCancelled(

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

  @Schema(example = "null", required = true, description = "cancelledAtDate should be used instead")
  @get:JsonProperty("cancelledAt", required = true) val cancelledAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("cancelledAtDate", required = true) val cancelledAtDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("cancelledBy", required = true) val cancelledBy: StaffMember,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("cancellationReason", required = true) val cancellationReason: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("cancellationRecordedAt", required = true) val cancellationRecordedAt: java.time.Instant,
)
