package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlannedTransferRequestRejected
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlannedTransferRequestRejectedPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import java.util.UUID

@Service
class PlannedTransferRequestRejectedTimelineFactory(val domainEventService: Cas1DomainEventService) : TimelineFactory<Cas1PlannedTransferRequestRejectedPayload> {

  override fun produce(domainEventId: UUID): Cas1PlannedTransferRequestRejectedPayload {
    val event = domainEventService.get(domainEventId, PlannedTransferRequestRejected::class)!!

    val details = event.data.eventDetails
    val reason = details.reason

    return Cas1PlannedTransferRequestRejectedPayload(
      type = Cas1TimelineEventType.plannedTransferRequestRejected,
      booking = details.booking.toTimelinePayloadSummary(),
      reason = NamedId(reason.id, reason.code),
    )
  }

  override fun forType() = DomainEventType.APPROVED_PREMISES_PLANNED_TRANSFER_REQUEST_REJECTED
}
