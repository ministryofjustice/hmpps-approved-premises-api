package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class BookingCancelled(

  val personReference: PersonReference,

  @Schema(example = "7", required = true, description = "Used in Delius to identify the 'event' via the first active conviction's 'index'")
  val deliusEventNumber: String,

  @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
  val applicationId: UUID,

  @Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
  val applicationUrl: String,

  @Schema(example = "14c80733-4b6d-4f35-b724-66955aac320c", required = true, description = "The UUID of booking for an AP place")
  val bookingId: UUID,

  val premises: Premises,

  @Schema(example = "null", required = true, description = "cancelledAtDate should be used instead")
  val cancelledAt: java.time.Instant,

  val cancelledAtDate: java.time.LocalDate,

  val cancelledBy: StaffMember,

  val cancellationReason: String,

  val cancellationRecordedAt: java.time.Instant,

  val appealChangeRequestId: UUID? = null,
) : Cas1DomainEventPayload
