package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.time.Instant
import java.util.UUID

data class EmergencyTransferCreated(
  val applicationId: UUID,
  val createdAt: Instant,
  val createdBy: StaffMember,
  val from: EventBookingSummary,
  val to: EventBookingSummary,
) : Cas1DomainEventPayload

