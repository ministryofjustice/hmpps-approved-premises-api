package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class MatchRequestWithdrawn(

  @field:Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

  @field:Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
  @get:JsonProperty("applicationUrl", required = true) val applicationUrl: kotlin.String,

  @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

  @field:Schema(example = "7", required = true, description = "Used in Delius to identify the 'event' via the first active conviction's 'index'")
  @get:JsonProperty("deliusEventNumber", required = true) val deliusEventNumber: kotlin.String,

  @get:JsonProperty("withdrawnAt", required = true) val withdrawnAt: java.time.Instant,

  @get:JsonProperty("withdrawnBy", required = true) val withdrawnBy: WithdrawnBy,

  @field:Schema(example = "RELATED_APPLICATION_WITHDRAWN", required = true, description = "")
  @get:JsonProperty("withdrawalReason", required = true) val withdrawalReason: kotlin.String,

  @get:JsonProperty("datePeriod", required = true) val datePeriod: DatePeriod,

  @field:Schema(example = "14c80733-4b6d-4f35-b724-66955aac320c", description = "The UUID of a placement application")
  @get:JsonProperty("matchRequestId") val matchRequestId: java.util.UUID? = null,

  @field:Schema(example = "null", description = "Indicate if this match request was created for the arrival date specified when the application was initially submitted")
  @get:JsonProperty("requestIsForApplicationsArrivalDate") val requestIsForApplicationsArrivalDate: kotlin.Boolean? = null,
) : Cas1DomainEventPayload
