package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.time.Instant
import java.util.UUID

@Deprecated("The generic [Cas1DomainEventEnvelope] should be used instead of type-specific envelopes")
data class EmergencyTransferCreatedEnvelope(
  override val id: UUID,
  override val timestamp: Instant,
  override val eventType: EventType,
  override val eventDetails: EmergencyTransferCreated,
): Cas1DomainEventEnvelopeInterface<EmergencyTransferCreated>