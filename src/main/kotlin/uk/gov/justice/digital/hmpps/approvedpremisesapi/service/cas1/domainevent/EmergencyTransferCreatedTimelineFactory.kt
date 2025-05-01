package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EmergencyTransferCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1EmergencyTransferCreatedContentPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import java.util.UUID

@Service
class EmergencyTransferCreatedTimelineFactory(val domainEventService: Cas1DomainEventService) : TimelineFactory<Cas1EmergencyTransferCreatedContentPayload> {

  override fun produce(domainEventId: UUID): Cas1EmergencyTransferCreatedContentPayload {
    val event = domainEventService.get(domainEventId, EmergencyTransferCreated::class)!!

    val details = event.data.eventDetails

    return Cas1EmergencyTransferCreatedContentPayload(
      type = Cas1TimelineEventType.emergencyTransferCreated,
      from = details.from.toTimelinePayloadSummary(),
      to = details.to.toTimelinePayloadSummary(),
      schemaVersion = event.schemaVersion,
    )
  }

  override fun forType() = DomainEventType.APPROVED_PREMISES_EMERGENCY_TRANSFER_CREATED
}
