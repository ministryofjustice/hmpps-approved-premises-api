package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.time.Instant
import java.util.UUID

data class EmergencyTransferCreatedEnvelope(
  override val id: UUID,
  override val timestamp: Instant,
  override val eventType: EventType,
  override val eventDetails: EmergencyTransferCreated,
): Cas1DomainEventEnvelope<EmergencyTransferCreated>