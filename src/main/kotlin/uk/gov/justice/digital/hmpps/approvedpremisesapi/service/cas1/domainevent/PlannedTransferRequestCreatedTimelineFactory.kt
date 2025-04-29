package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlannedTransferRequestCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlannedTransferRequestCreatedPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import java.util.UUID

@Service
class PlannedTransferRequestCreatedTimelineFactory(val domainEventService: Cas1DomainEventService) : TimelineFactory<Cas1PlannedTransferRequestCreatedPayload> {

  override fun produce(domainEventId: UUID): Cas1PlannedTransferRequestCreatedPayload {
    val event = domainEventService.get(domainEventId, PlannedTransferRequestCreated::class)!!

    val details = event.data.eventDetails
    val reason = details.reason

    return Cas1PlannedTransferRequestCreatedPayload(
      type = Cas1TimelineEventType.plannedTransferRequestCreated,
      booking = details.booking.toTimelinePayloadSummary(),
      reason = NamedId(reason.id, reason.code),
      schemaVersion = event.schemaVersion,
    )
  }

  override fun forType() = DomainEventType.APPROVED_PREMISES_PLANNED_TRANSFER_REQUEST_CREATED
}
