package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class ApplicationAssessed(

  @field:Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

  @field:Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
  @get:JsonProperty("applicationUrl", required = true) val applicationUrl: kotlin.String,

  @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

  @field:Schema(example = "7", required = true, description = "Used in Delius to identify the 'event' via the first active conviction's 'index'")
  @get:JsonProperty("deliusEventNumber", required = true) val deliusEventNumber: kotlin.String,

  @get:JsonProperty("assessedAt", required = true) val assessedAt: java.time.Instant,

  @get:JsonProperty("assessedBy", required = true) val assessedBy: ApplicationAssessedAssessedBy,

  @field:Schema(example = "Rejected", required = true, description = "")
  @get:JsonProperty("decision", required = true) val decision: kotlin.String,

  @field:Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", description = "The UUID of an assessment of an application for an AP place")
  @get:JsonProperty("assessmentId") val assessmentId: java.util.UUID? = null,

  @field:Schema(example = "Risk too low", description = "")
  @get:JsonProperty("decisionRationale") val decisionRationale: kotlin.String? = null,

  @get:JsonProperty("arrivalDate") val arrivalDate: java.time.Instant? = null,
) : Cas1DomainEventPayload
