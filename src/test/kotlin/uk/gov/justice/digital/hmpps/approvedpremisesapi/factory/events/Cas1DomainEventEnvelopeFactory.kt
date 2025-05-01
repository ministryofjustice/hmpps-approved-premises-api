package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import java.time.Instant
import java.util.UUID

class Cas1DomainEventEnvelopeFactory<T : Cas1DomainEventPayload> : Factory<Cas1DomainEventEnvelope<T>> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var timestamp: Yielded<Instant> = { Instant.now() }
  private var type: Yielded<EventType> = { EventType.plannedTransferRequestCreated }
  private var details: Yielded<T> = { PlacementChangeRequestCreatedFactory().produce() as T }

  fun withDetails(details: T) = apply { this.details = { details } }

  override fun produce() = Cas1DomainEventEnvelope<T>(
    id = id(),
    timestamp = timestamp(),
    eventType = type(),
    eventDetails = details(),
  )
}
