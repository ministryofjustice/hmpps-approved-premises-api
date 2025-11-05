package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class PersonDeparted(

  @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

  @field:Schema(example = "7", required = true, description = "Used in Delius to identify the 'event' via the first active conviction's 'index'")
  @get:JsonProperty("deliusEventNumber", required = true) val deliusEventNumber: kotlin.String,

  @field:Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

  @field:Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
  @get:JsonProperty("applicationUrl", required = true) val applicationUrl: kotlin.String,

  @field:Schema(example = "14c80733-4b6d-4f35-b724-66955aac320c", required = true, description = "The UUID of booking for an AP place")
  @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

  @get:JsonProperty("recordedBy", required = true) val recordedBy: StaffMember,

  @get:JsonProperty("premises", required = true) val premises: Premises,

  @get:JsonProperty("departedAt", required = true) val departedAt: java.time.Instant,

  @field:Schema(example = "Arrested, remanded in custody, or sentenced", required = true, description = "")
  @get:JsonProperty("reason", required = true) val reason: kotlin.String,

  @field:Schema(example = "Q", required = true, description = "")
  @get:JsonProperty("legacyReasonCode", required = true) val legacyReasonCode: kotlin.String,

  @get:JsonProperty("destination", required = true) val destination: PersonDepartedDestination,
) : Cas1DomainEventPayload
