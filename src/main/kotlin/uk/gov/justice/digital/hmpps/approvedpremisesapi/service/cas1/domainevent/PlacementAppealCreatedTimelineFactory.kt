package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlannedTransferRequestCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementAppealCreatedPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import java.util.UUID

@Service
class PlacementAppealCreatedTimelineFactory(val domainEventService: Cas1DomainEventService) : TimelineFactory<Cas1PlacementAppealCreatedPayload> {

  override fun produce(domainEventId: UUID): Cas1PlacementAppealCreatedPayload {
    val event = domainEventService.get(domainEventId, PlacementAppealCreated::class)!!

    val details = event.data.eventDetails
    val reason = details.reason

    return Cas1PlacementAppealCreatedPayload(
      type = Cas1TimelineEventType.placementAppealCreated,
      booking = details.booking.toTimelinePayloadSummary(),
      reason = NamedId(reason.id, reason.code),
      schemaVersion = event.schemaVersion,
    )
  }

  override fun forType() = DomainEventType.APPROVED_PREMISES_PLACEMENT_APPEAL_CREATED
}
