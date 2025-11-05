package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class RequestForPlacementCreated(

  @field:Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

  @field:Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
  @get:JsonProperty("applicationUrl", required = true) val applicationUrl: kotlin.String,

  @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

  @field:Schema(example = "7", required = true, description = "Used in Delius to identify the 'event' via the first active conviction's 'index'")
  @get:JsonProperty("deliusEventNumber", required = true) val deliusEventNumber: kotlin.String,

  @field:Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of a request for placement. Currently a proxy for PlacementApplicationId")
  @get:JsonProperty("requestForPlacementId", required = true) val requestForPlacementId: java.util.UUID,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @field:Schema(example = "Mon Jan 30 00:00:00 GMT 2023", required = true, description = "")
  @get:JsonProperty("expectedArrival", required = true) val expectedArrival: java.time.LocalDate,

  @field:Schema(example = "7", required = true, description = "")
  @get:JsonProperty("duration", required = true) val duration: kotlin.Int,

  @get:JsonProperty("requestForPlacementType", required = true) val requestForPlacementType: RequestForPlacementType,

  @get:JsonProperty("createdBy") val createdBy: StaffMember? = null,
) : Cas1DomainEventPayload
