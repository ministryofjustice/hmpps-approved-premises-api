package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealRejected
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementAppealRejectedPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import java.util.UUID

@Service
class PlacementAppealRejectedTimelineFactory(val domainEventService: Cas1DomainEventService) : TimelineFactory<Cas1PlacementAppealRejectedPayload> {

  override fun produce(domainEventId: UUID): Cas1PlacementAppealRejectedPayload {
    val event = domainEventService.get(domainEventId, PlacementAppealRejected::class)!!

    val details = event.data.eventDetails
    val reason = details.reason

    return Cas1PlacementAppealRejectedPayload(
      type = Cas1TimelineEventType.placementAppealRejected,
      booking = details.booking.toTimelinePayloadSummary(),
      reason = NamedId(reason.id, reason.code),
    )
  }

  override fun forType() = DomainEventType.APPROVED_PREMISES_PLACEMENT_APPEAL_REJECTED
}
