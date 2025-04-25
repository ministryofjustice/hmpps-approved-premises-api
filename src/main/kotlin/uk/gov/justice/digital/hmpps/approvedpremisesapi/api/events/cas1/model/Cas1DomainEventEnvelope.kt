package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.time.Instant
import java.util.UUID

data class Cas1DomainEventEnvelope<T : Cas1DomainEventPayload>(
  val id: UUID,
  val timestamp: Instant,
  val eventType: EventType,
  val eventDetails: T,
)