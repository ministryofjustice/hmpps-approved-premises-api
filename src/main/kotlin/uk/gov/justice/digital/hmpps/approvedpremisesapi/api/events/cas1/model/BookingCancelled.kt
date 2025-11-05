package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class BookingCancelled(

  @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

  @field:Schema(example = "7", required = true, description = "Used in Delius to identify the 'event' via the first active conviction's 'index'")
  @get:JsonProperty("deliusEventNumber", required = true) val deliusEventNumber: String,

  @field:Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
  @get:JsonProperty("applicationId", required = true) val applicationId: UUID,

  @field:Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
  @get:JsonProperty("applicationUrl", required = true) val applicationUrl: String,

  @field:Schema(example = "14c80733-4b6d-4f35-b724-66955aac320c", required = true, description = "The UUID of booking for an AP place")
  @get:JsonProperty("bookingId", required = true) val bookingId: UUID,

  @get:JsonProperty("premises", required = true) val premises: Premises,

  @field:Schema(example = "null", required = true, description = "cancelledAtDate should be used instead")
  @get:JsonProperty("cancelledAt", required = true) val cancelledAt: java.time.Instant,

  @get:JsonProperty("cancelledAtDate", required = true) val cancelledAtDate: java.time.LocalDate,

  @get:JsonProperty("cancelledBy", required = true) val cancelledBy: StaffMember,

  @get:JsonProperty("cancellationReason", required = true) val cancellationReason: String,

  @get:JsonProperty("cancellationRecordedAt", required = true) val cancellationRecordedAt: java.time.Instant,

  val appealChangeRequestId: UUID? = null,
) : Cas1DomainEventPayload
