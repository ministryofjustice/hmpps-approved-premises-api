package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementChangeRequestCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementChangeRequestCreatedPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import java.util.UUID

@Service
class PlacementChangeRequestCreatedTimelineFactory(val domainEventService: Cas1DomainEventService) : TimelineFactory<Cas1PlacementChangeRequestCreatedPayload> {

  override fun produce(domainEventId: UUID): Cas1PlacementChangeRequestCreatedPayload {
    val event = domainEventService.get(domainEventId, PlacementChangeRequestCreated::class)!!

    val details = event.data.eventDetails
    val reason = details.reason

    return Cas1PlacementChangeRequestCreatedPayload(
      type = Cas1TimelineEventType.placementChangeRequestCreated,
      booking = details.booking.toTimelinePayloadSummary(),
      reason = NamedId(reason.id, reason.code),
    )
  }

  override fun forType() = DomainEventType.APPROVED_PREMISES_PLACEMENT_CHANGE_REQUEST_CREATED
}
