package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.util.UUID

data class ApplicationExpiredManually(

  val applicationId: UUID,

  val expiredBy: StaffMember,

  val expiredAt: java.time.Instant,

  val expiredReason: String,
) : Cas1DomainEventPayload
