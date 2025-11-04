package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import io.swagger.v3.oas.annotations.media.Schema

data class AssessmentAppealed(

  @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
  val applicationId: java.util.UUID,

  @Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
  val applicationUrl: kotlin.String,

  @Schema(example = "dd450bbc-162d-4380-a103-9f261943b98f", required = true, description = "The UUID of an appeal for an application")
  val appealId: java.util.UUID,

  @Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713/appeals/dd450bbc-162d-4380-a103-9f261943b98f", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an appeal and related resources")
  val appealUrl: kotlin.String,

  val personReference: PersonReference,

  @Schema(example = "7", required = true, description = "Used in Delius to identify the 'event' via the first active conviction's 'index'")
  val deliusEventNumber: kotlin.String,

  val createdAt: java.time.Instant,

  val createdBy: StaffMember,

  val appealDetail: kotlin.String,

  val decision: AppealDecision,

  val decisionDetail: kotlin.String,
) : Cas1DomainEventPayload
