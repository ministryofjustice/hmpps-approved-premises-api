package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.util.UUID

/**
 *
 * @param applicationId The UUID of an application for an AP place
 */
data class ApplicationExpiredManually(

  val applicationId: UUID,

  val expiredBy: StaffMember,

  val expiredAt: java.time.Instant,

  val expiredReason: String,
) : Cas1DomainEventPayload
