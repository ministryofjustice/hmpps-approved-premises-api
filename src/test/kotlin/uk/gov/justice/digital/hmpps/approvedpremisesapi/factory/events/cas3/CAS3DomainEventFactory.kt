package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas3

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingCancelledEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingConfirmedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingProvisionallyMadeEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3Event
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonArrivedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonDepartedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3ReferralSubmittedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.Instant
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class CAS3DomainEventFactory<T : CAS3Event, D : Any>(
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
    CAS3BookingCancelledEventDetails::class -> EventType.bookingCancelled
    CAS3BookingConfirmedEventDetails::class -> EventType.bookingConfirmed
    CAS3BookingProvisionallyMadeEventDetails::class -> EventType.bookingProvisionallyMade
    CAS3PersonArrivedEventDetails::class -> EventType.personArrived
    CAS3PersonDepartedEventDetails::class -> EventType.personDeparted
    CAS3ReferralSubmittedEventDetails::class -> EventType.referralSubmitted
    else -> throw RuntimeException("Unknown event details type ${dataClass.qualifiedName}")
  }
}
