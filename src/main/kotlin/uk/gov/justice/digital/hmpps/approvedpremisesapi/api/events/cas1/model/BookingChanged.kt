package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class BookingChanged(

  @field:Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
  @get:JsonProperty("applicationId", required = true) val applicationId: UUID,

  @field:Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
  @get:JsonProperty("applicationUrl", required = true) val applicationUrl: String,

  @field:Schema(example = "14c80733-4b6d-4f35-b724-66955aac320c", required = true, description = "The UUID of booking for an AP place")
  @get:JsonProperty("bookingId", required = true) val bookingId: UUID,

  @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

  @field:Schema(example = "7", required = true, description = "Used in Delius to identify the 'event' via the first active conviction's 'index'")
  @get:JsonProperty("deliusEventNumber", required = true) val deliusEventNumber: String,

  @get:JsonProperty("changedAt", required = true) val changedAt: java.time.Instant,

  @get:JsonProperty("changedBy", required = true) val changedBy: StaffMember,

  @get:JsonProperty("premises", required = true) val premises: Premises,

  @field:Schema(example = "Mon Jan 30 00:00:00 GMT 2023", required = true, description = "")
  @get:JsonProperty("arrivalOn", required = true) val arrivalOn: java.time.LocalDate,

  @field:Schema(example = "Sun Apr 30 01:00:00 BST 2023", required = true, description = "")
  @get:JsonProperty("departureOn", required = true) val departureOn: java.time.LocalDate,

  @field:Schema(example = "Mon Jan 30 00:00:00 GMT 2023", description = "Only set if the expected arrival on has changed")
  @get:JsonProperty("previousArrivalOn") val previousArrivalOn: java.time.LocalDate? = null,

  @field:Schema(example = "Mon Jan 30 00:00:00 GMT 2023", description = "Only set if the expected departure on has changed")
  @get:JsonProperty("previousDepartureOn") val previousDepartureOn: java.time.LocalDate? = null,

  @get:JsonProperty("characteristics") val characteristics: List<SpaceCharacteristic>? = null,

  @get:JsonProperty("previousCharacteristics") val previousCharacteristics: List<SpaceCharacteristic>? = null,

  val transferredTo: EventTransferInfo? = null,
) : Cas1DomainEventPayload
