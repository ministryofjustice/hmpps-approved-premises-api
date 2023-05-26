package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.OffsetDateTime
import java.util.UUID

class DomainEventEntityFactory : Factory<DomainEventEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var crn: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var type: Yielded<DomainEventType> = { DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED }
  private var occurredAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(7) }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(7) }
  private var data: Yielded<String> = { "{}" }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withApplicationId(applicationId: UUID) = apply {
    this.applicationId = { applicationId }
  }

  fun withCrn(crn: String) = apply {
    this.crn = { crn }
  }

  fun withType(type: DomainEventType) = apply {
    this.type = { type }
  }

  fun withOccurredAt(occurredAt: OffsetDateTime) = apply {
    this.occurredAt = { occurredAt }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withData(data: String) = apply {
    this.data = { data }
  }

  override fun produce(): DomainEventEntity = DomainEventEntity(
    id = this.id(),
    applicationId = this.applicationId(),
    crn = this.crn(),
    type = this.type(),
    occurredAt = this.occurredAt(),
    createdAt = this.createdAt(),
    data = this.data(),
  )
}
