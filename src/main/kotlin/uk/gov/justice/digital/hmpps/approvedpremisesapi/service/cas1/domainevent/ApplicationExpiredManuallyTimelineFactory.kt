package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationExpiredManually
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationExpiredManuallyPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import java.util.UUID

@Service
class ApplicationExpiredManuallyTimelineFactory(val domainEventService: Cas1DomainEventService) : TimelineFactory<Cas1ApplicationExpiredManuallyPayload> {
  override fun produce(domainEventId: UUID): Cas1ApplicationExpiredManuallyPayload {
    val event = domainEventService.get(domainEventId, ApplicationExpiredManually::class)!!

    return Cas1ApplicationExpiredManuallyPayload(
      type = Cas1TimelineEventType.applicationManuallyExpired,
      expiredReason = event.data.eventDetails.expiredReason,
    )
  }

  override fun forType() = DomainEventType.APPROVED_PREMISES_APPLICATION_EXPIRED_MANUALLY
}
