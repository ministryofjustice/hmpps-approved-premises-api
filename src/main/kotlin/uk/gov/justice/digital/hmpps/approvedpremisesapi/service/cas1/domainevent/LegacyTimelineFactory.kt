package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventContentPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventDescriber.EventDescriptionAndPayload
import java.util.UUID

/**
 * Used when migrating 'description' based timeline entries to 'content payload based' timeline entries
 *
 * Once the UI has started using the content payload, the factory should be converted into a regular
 * [TimelineFactory]
 */
interface LegacyTimelineFactory<T : Cas1TimelineEventContentPayload> {
  fun produce(domainEventId: UUID): EventDescriptionAndPayload<T>

  fun forType(): DomainEventType
}
