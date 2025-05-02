package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlannedTransferRequestAccepted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlannedTransferRequestAcceptedPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import java.util.UUID

@Service
class PlannedTransferRequestAcceptedTimelineFactory(val domainEventService: Cas1DomainEventService) : TimelineFactory<Cas1PlannedTransferRequestAcceptedPayload> {

  override fun produce(domainEventId: UUID): Cas1PlannedTransferRequestAcceptedPayload {
    val event = domainEventService.get(domainEventId, PlannedTransferRequestAccepted::class)!!

    val details = event.data.eventDetails

    return Cas1PlannedTransferRequestAcceptedPayload(
      type = Cas1TimelineEventType.plannedTransferRequestAccepted,
      from = details.from.toTimelinePayloadSummary(),
      to = details.to.toTimelinePayloadSummary(),
    )
  }

  override fun forType() = DomainEventType.APPROVED_PREMISES_PLANNED_TRANSFER_REQUEST_ACCEPTED
}
