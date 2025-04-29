package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventContentPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import java.util.UUID

interface TimelineFactory<T : Cas1TimelineEventContentPayload> {
  fun produce(domainEventId: UUID): T

  fun forType(): DomainEventType
}
