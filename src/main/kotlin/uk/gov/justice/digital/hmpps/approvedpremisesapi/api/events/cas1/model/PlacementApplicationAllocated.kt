package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class PlacementApplicationAllocated(

  @field:Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

  @field:Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
  @get:JsonProperty("applicationUrl", required = true) val applicationUrl: kotlin.String,

  @field:Schema(example = "14c80733-4b6d-4f35-b724-66955aac320c", required = true, description = "The UUID of a placement application")
  @get:JsonProperty("placementApplicationId", required = true) val placementApplicationId: java.util.UUID,

  @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

  @get:JsonProperty("allocatedAt", required = true) val allocatedAt: java.time.Instant,

  @get:JsonProperty("placementDates", required = true) val placementDates: kotlin.collections.List<DatePeriod>,

  @get:JsonProperty("allocatedTo") val allocatedTo: StaffMember? = null,

  @get:JsonProperty("allocatedBy") val allocatedBy: StaffMember? = null,
) : Cas1DomainEventPayload
