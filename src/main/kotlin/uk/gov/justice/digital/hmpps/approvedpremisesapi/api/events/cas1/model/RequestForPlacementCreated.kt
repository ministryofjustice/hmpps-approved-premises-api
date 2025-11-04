package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import io.swagger.v3.oas.annotations.media.Schema

data class RequestForPlacementCreated(

  @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
  val applicationId: java.util.UUID,

  @Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
  val applicationUrl: kotlin.String,

  val personReference: PersonReference,

  @Schema(example = "7", required = true, description = "Used in Delius to identify the 'event' via the first active conviction's 'index'")
  val deliusEventNumber: kotlin.String,

  @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of a request for placement. Currently a proxy for PlacementApplicationId")
  val requestForPlacementId: java.util.UUID,

  val createdAt: java.time.Instant,

  @Schema(example = "Mon Jan 30 00:00:00 GMT 2023", required = true, description = "")
  val expectedArrival: java.time.LocalDate,

  @Schema(example = "7", required = true, description = "")
  val duration: kotlin.Int,

  val requestForPlacementType: RequestForPlacementType,

  val createdBy: StaffMember? = null,
) : Cas1DomainEventPayload
