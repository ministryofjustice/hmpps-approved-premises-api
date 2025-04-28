package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedAssessedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference

/**
 *
 * @param applicationId The UUID of an application for an AP place
 * @param applicationUrl The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings
 * @param personReference
 * @param deliusEventNumber Used in Delius to identify the 'event' via the first active conviction's 'index'
 * @param assessedAt
 * @param assessedBy
 * @param decision
 * @param assessmentId The UUID of an assessment of an application for an AP place
 * @param decisionRationale
 * @param arrivalDate
 */
data class ApplicationAssessed(

  @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

  @Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
  @get:JsonProperty("applicationUrl", required = true) val applicationUrl: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

  @Schema(example = "7", required = true, description = "Used in Delius to identify the 'event' via the first active conviction's 'index'")
  @get:JsonProperty("deliusEventNumber", required = true) val deliusEventNumber: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("assessedAt", required = true) val assessedAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("assessedBy", required = true) val assessedBy: ApplicationAssessedAssessedBy,

  @Schema(example = "Rejected", required = true, description = "")
  @get:JsonProperty("decision", required = true) val decision: kotlin.String,

  @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", description = "The UUID of an assessment of an application for an AP place")
  @get:JsonProperty("assessmentId") val assessmentId: java.util.UUID? = null,

  @Schema(example = "Risk too low", description = "")
  @get:JsonProperty("decisionRationale") val decisionRationale: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("arrivalDate") val arrivalDate: java.time.Instant? = null,
) : Cas1DomainEventPayload
