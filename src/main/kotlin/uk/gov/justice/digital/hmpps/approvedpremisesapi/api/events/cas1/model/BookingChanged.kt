package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class BookingChanged(

  @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
  val applicationId: UUID,

  @Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
  val applicationUrl: String,

  @Schema(example = "14c80733-4b6d-4f35-b724-66955aac320c", required = true, description = "The UUID of booking for an AP place")
  val bookingId: UUID,

  val personReference: PersonReference,

  @Schema(example = "7", required = true, description = "Used in Delius to identify the 'event' via the first active conviction's 'index'")
  val deliusEventNumber: String,

  val changedAt: java.time.Instant,

  val changedBy: StaffMember,

  val premises: Premises,

  @Schema(example = "Mon Jan 30 00:00:00 GMT 2023", required = true, description = "")
  val arrivalOn: java.time.LocalDate,

  @Schema(example = "Sun Apr 30 01:00:00 BST 2023", required = true, description = "")
  val departureOn: java.time.LocalDate,

  @Schema(example = "Mon Jan 30 00:00:00 GMT 2023", description = "Only set if the expected arrival on has changed")
  val previousArrivalOn: java.time.LocalDate? = null,

  @Schema(example = "Mon Jan 30 00:00:00 GMT 2023", description = "Only set if the expected departure on has changed")
  val previousDepartureOn: java.time.LocalDate? = null,

  val characteristics: List<SpaceCharacteristic>? = null,

  val previousCharacteristics: List<SpaceCharacteristic>? = null,

  val transferredTo: EventTransferInfo? = null,
) : Cas1DomainEventPayload
