package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealAccepted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementAppealAcceptedPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import java.util.UUID

@Service
class PlacementAppealAcceptedTimelineFactory(val domainEventService: Cas1DomainEventService) : TimelineFactory<Cas1PlacementAppealAcceptedPayload> {

  override fun produce(domainEventId: UUID): Cas1PlacementAppealAcceptedPayload {
    val event = domainEventService.get(domainEventId, PlacementAppealAccepted::class)!!

    val details = event.data.eventDetails

    return Cas1PlacementAppealAcceptedPayload(
      type = Cas1TimelineEventType.placementAppealAccepted,
      booking = details.booking.toTimelinePayloadSummary(),
    )
  }

  override fun forType() = DomainEventType.APPROVED_PREMISES_PLACEMENT_APPEAL_ACCEPTED
}
