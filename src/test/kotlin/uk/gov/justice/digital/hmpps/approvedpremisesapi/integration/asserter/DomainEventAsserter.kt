package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.asserter

import org.assertj.core.api.Assertions.assertThat
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.SnsDomainEventListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import java.util.UUID

@Component
class DomainEventAsserter(
  val snsDomainEventListener: SnsDomainEventListener,
  val domainEventRepository: DomainEventRepository,
) {

  fun blockForEmittedDomainEvent(eventType: DomainEventType) = snsDomainEventListener.blockForMessage(eventType)

  fun assertDomainEventStoreCount(applicationId: UUID, expectedCount: Int) {
    assertThat(domainEventRepository.findAllTimelineEventsByApplicationId(applicationId)).hasSize(expectedCount)
  }

  fun assertDomainEventOfTypeStored(applicationId: UUID, eventType: DomainEventType): DomainEventEntity {
    assertThat(
      domainEventRepository
        .findAllTimelineEventsByApplicationId(applicationId)
        .map { it.type },
    ).contains(eventType)

    return domainEventRepository.findByApplicationId(applicationId).first { it.type == eventType }
  }

  fun assertDomainEventsOfTypeStored(applicationId: UUID, eventType: DomainEventType, expectedCount: Int) {
    assertThat(
      domainEventRepository
        .findAllTimelineEventsByApplicationId(applicationId)
        .count { it.type == eventType },
    ).isEqualTo(expectedCount)
  }
}
