package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementChangeRequestAccepted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementChangeRequestAcceptedPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import java.util.UUID

@Service
class PlacementChangeRequestAcceptedTimelineFactory(val domainEventService: Cas1DomainEventService) : TimelineFactory<Cas1PlacementChangeRequestAcceptedPayload> {

  override fun produce(domainEventId: UUID): Cas1PlacementChangeRequestAcceptedPayload {
    val event = domainEventService.get(domainEventId, PlacementChangeRequestAccepted::class)!!

    val details = event.data.eventDetails

    return Cas1PlacementChangeRequestAcceptedPayload(
      type = Cas1TimelineEventType.placementChangeRequestAccepted,
      booking = details.booking.toTimelinePayloadSummary(),
    )
  }

  override fun forType() = DomainEventType.APPROVED_PREMISES_PLACEMENT_CHANGE_REQUEST_ACCEPTED
}
