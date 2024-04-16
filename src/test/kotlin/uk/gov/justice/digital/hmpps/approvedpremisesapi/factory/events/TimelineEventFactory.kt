package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventAssociatedUrl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.util.UUID

class TimelineEventFactory : Factory<TimelineEvent> {
  private var type: Yielded<TimelineEventType?> = { randomOf(TimelineEventType.entries) }
  private var id: Yielded<UUID?> = { UUID.randomUUID() }
  private var occurredAt: Yielded<Instant?> = { Instant.now().randomDateTimeBefore(7) }
  private var content: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var createdBy: Yielded<User?> = { null }
  private var associatedUrls: Yielded<List<TimelineEventAssociatedUrl>?> = { null }

  fun withType(type: TimelineEventType?) = apply {
    this.type = { type }
  }

  fun withId(id: UUID?) = apply {
    this.id = { id }
  }

  fun withOccurredAt(occurredAt: Instant?) = apply {
    this.occurredAt = { occurredAt }
  }

  fun withContent(content: String?) = apply {
    this.content = { content }
  }

  fun withCreatedBy(createdBy: User?) = apply {
    this.createdBy = { createdBy }
  }

  fun withCreatedBy(configuration: ApprovedPremisesUserFactory.() -> Unit) = apply {
    this.createdBy = { ApprovedPremisesUserFactory().apply(configuration).produce() }
  }

  fun withAssociatedUrls(associatedUrls: List<TimelineEventAssociatedUrl>?) = apply {
    this.associatedUrls = { associatedUrls }
  }

  override fun produce() = TimelineEvent(
    type = this.type(),
    id = this.id()?.toString(),
    occurredAt = this.occurredAt(),
    content = this.content(),
    createdBy = this.createdBy(),
    associatedUrls = this.associatedUrls(),
  )
}
