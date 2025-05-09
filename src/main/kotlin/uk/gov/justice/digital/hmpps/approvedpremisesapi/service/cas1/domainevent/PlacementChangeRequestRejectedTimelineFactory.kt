package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementChangeRequestRejected
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementChangeRequestRejectedPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import java.util.UUID

@Service
class PlacementChangeRequestRejectedTimelineFactory(val domainEventService: Cas1DomainEventService) : TimelineFactory<Cas1PlacementChangeRequestRejectedPayload> {

  override fun produce(domainEventId: UUID): Cas1PlacementChangeRequestRejectedPayload {
    val event = domainEventService.get(domainEventId, PlacementChangeRequestRejected::class)!!

    val details = event.data.eventDetails
    val reason = details.reason

    return Cas1PlacementChangeRequestRejectedPayload(
      type = Cas1TimelineEventType.placementChangeRequestRejected,
      booking = details.booking.toTimelinePayloadSummary(),
      reason = NamedId(reason.id, reason.code),
      changeRequestType = when (details.changeRequestType) {
        EventChangeRequestType.PLACEMENT_APPEAL -> Cas1ChangeRequestType.PLACEMENT_APPEAL
        EventChangeRequestType.PLACEMENT_EXTENSION -> Cas1ChangeRequestType.PLACEMENT_EXTENSION
        EventChangeRequestType.PLANNED_TRANSFER -> Cas1ChangeRequestType.PLANNED_TRANSFER
      },
    )
  }

  override fun forType() = DomainEventType.APPROVED_PREMISES_PLACEMENT_CHANGE_REQUEST_REJECTED
}
