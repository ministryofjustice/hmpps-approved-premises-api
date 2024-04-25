package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas2

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2Event
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.Instant
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class Cas2DomainEventFactory<T : Cas2Event, D : Any>(
  private val eventClass: KClass<T>,
  private val dataClass: KClass<D>,
) : Factory<DomainEvent<T>> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var crn: Yielded<String> = { randomStringUpperCase(6) }
  private var occurredAt: Yielded<Instant> = { Instant.now() }
  private var timestamp: Yielded<Instant> = { Instant.now() }
  private var data: Yielded<D?> = { null }
  private var nomsNumber: Yielded<String?> = { randomStringUpperCase(8) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withApplicationId(applicationId: UUID) = apply {
    this.applicationId = { applicationId }
  }

  fun withCrn(crn: String) = apply {
    this.crn = { crn }
  }

  fun withOccurredAt(occurredAt: Instant) = apply {
    this.occurredAt = { occurredAt }
  }

  fun withTimestamp(timestamp: Instant) = apply {
    this.timestamp = { timestamp }
  }

  fun withData(data: D) = apply {
    this.data = { data }
  }

  override fun produce(): DomainEvent<T> {
    val dataConstructor = getConstructor()
    val eventType = getEventType()

    return DomainEvent(
      id = this.id(),
      applicationId = this.applicationId(),
      crn = this.crn(),
      nomsNumber = this.nomsNumber(),
      occurredAt = this.occurredAt(),
      data = dataConstructor(
        this.data() ?: throw RuntimeException("Must provide event data"),
        this.id(),
        this.timestamp(),
        eventType,
      ),
    )
  }

  private fun getConstructor(): (D, UUID, Instant, EventType) -> T {
    val primaryConstructor = eventClass.primaryConstructor!!

    return { eventDetails: D, id: UUID, timestamp: Instant, eventType: EventType ->
      primaryConstructor.call(eventDetails, id, timestamp, eventType)
    }
  }

  private fun getEventType(): EventType = when (dataClass) {
    Cas2ApplicationSubmittedEvent::class -> EventType.applicationSubmitted
    else -> throw RuntimeException("Unknown event details type ${dataClass.qualifiedName}")
  }
}
