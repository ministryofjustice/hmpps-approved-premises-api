package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.time.Instant
import java.util.UUID

data class Cas1DomainEventEnvelope<T : Cas1DomainEventPayload>(
  override val id: UUID,
  override val timestamp: Instant,
  override val eventType: EventType,
  override val eventDetails: T,
) : Cas1DomainEventEnvelopeInterface<T>