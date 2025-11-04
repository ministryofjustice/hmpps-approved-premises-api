package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param assessmentId The UUID of an assessment of an application for an AP place
 * @param assessmentUrl The URL on the Approved Premises service at which a user can view a representation of an AP assessment and related resources, including bookings
 * @param applicationId The UUID of an application for an AP place
 * @param applicationUrl The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings
 * @param personReference
 * @param requestedAt
 * @param requester
 * @param recipient
 * @param requestId The UUID of an application for an AP place
 */
data class FurtherInformationRequested(

  @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an assessment of an application for an AP place")
  val assessmentId: java.util.UUID,

  @Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/assessments/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP assessment and related resources, including bookings")
  val assessmentUrl: kotlin.String,

  @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
  val applicationId: java.util.UUID,

  @Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
  val applicationUrl: kotlin.String,

  val personReference: PersonReference,

  val requestedAt: java.time.Instant,

  val requester: StaffMember,

  val recipient: StaffMember,

  @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
  val requestId: java.util.UUID,
) : Cas1DomainEventPayload
