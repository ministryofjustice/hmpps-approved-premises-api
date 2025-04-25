package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.time.Instant
import java.util.UUID

interface Cas1DomainEventEnvelopeInterface<D> {
  val id: UUID
  val timestamp: Instant
  val eventType: EventType
  val eventDetails: D
}
