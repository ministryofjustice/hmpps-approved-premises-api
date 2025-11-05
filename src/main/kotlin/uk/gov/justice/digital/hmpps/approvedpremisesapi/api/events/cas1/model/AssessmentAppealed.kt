package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class AssessmentAppealed(

  @field:Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

  @field:Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
  @get:JsonProperty("applicationUrl", required = true) val applicationUrl: kotlin.String,

  @field:Schema(example = "dd450bbc-162d-4380-a103-9f261943b98f", required = true, description = "The UUID of an appeal for an application")
  @get:JsonProperty("appealId", required = true) val appealId: java.util.UUID,

  @field:Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713/appeals/dd450bbc-162d-4380-a103-9f261943b98f", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an appeal and related resources")
  @get:JsonProperty("appealUrl", required = true) val appealUrl: kotlin.String,

  @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

  @field:Schema(example = "7", required = true, description = "Used in Delius to identify the 'event' via the first active conviction's 'index'")
  @get:JsonProperty("deliusEventNumber", required = true) val deliusEventNumber: kotlin.String,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("createdBy", required = true) val createdBy: StaffMember,

  @get:JsonProperty("appealDetail", required = true) val appealDetail: kotlin.String,

  @get:JsonProperty("decision", required = true) val decision: AppealDecision,

  @get:JsonProperty("decisionDetail", required = true) val decisionDetail: kotlin.String,
) : Cas1DomainEventPayload
