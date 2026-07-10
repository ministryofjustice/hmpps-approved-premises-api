package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.InboxEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.ProcessedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

class InboxEventEntityFactory : Factory<InboxEventEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var eventType: Yielded<String> = { randomStringUpperCase(12) }
  private var eventDetailUrl: Yielded<String> = { randomStringUpperCase(12) }
  private var eventOccurredAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(5) }
  private var createdAt: Yielded<Instant> = { Instant.now().randomDateTimeBefore(14) }
  private var processedStatus: Yielded<ProcessedStatus> = { ProcessedStatus.entries.random() }
  private var processedAt: Yielded<Instant?> = { Instant.now().randomDateTimeBefore(14) }
  private var payload: Yielded<String> = { "{}" }

  fun withEventType(eventType: String) = apply {
    this.eventType = { eventType }
  }

  fun withEventOccurredAt(eventOccurredAt: OffsetDateTime) = apply {
    this.eventOccurredAt = { eventOccurredAt }
  }

  fun withPayload(payload: String) = apply {
    this.payload = { payload }
  }

  fun withProcessedStatus(status: ProcessedStatus) = apply {
    this.processedStatus = { status }
  }

  fun withProcessedAt(processedAt: Instant?) = apply {
    this.processedAt = { processedAt }
  }

  override fun produce(): InboxEventEntity = InboxEventEntity(
    id = this.id(),
    eventType = this.eventType(),
    eventDetailUrl = this.eventDetailUrl(),
    eventOccurredAt = this.eventOccurredAt(),
    createdAt = this.createdAt(),
    processedStatus = this.processedStatus(),
    processedAt = this.processedAt(),
    payload = this.payload(),
  )
}
