package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.external

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.external.Cas2ArrivalEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class Cas2ExternalDomainEventService(
  private val jsonMapper: JsonMapper,
  val domainEventRepository: DomainEventRepository,
) {

  @Transactional
  fun saveArrivalDomainEvent(event: DomainEvent<Cas2ArrivalEvent>) = domainEventRepository.save(
    DomainEventEntity(
      id = event.id,
      applicationId = event.applicationId,
      assessmentId = event.assessmentId,
      bookingId = event.bookingId,
      cas1SpaceBookingId = null,
      cas3PremisesId = null,
      cas3BedspaceId = null,
      crn = event.crn,
      nomsNumber = event.nomsNumber,
      type = DomainEventType.CAS2_PERSON_ARRIVED,
      occurredAt = event.occurredAt.atOffset(ZoneOffset.UTC),
      createdAt = OffsetDateTime.now(),
      cas3CancelledAt = null,
      cas3TransactionId = null,
      data = jsonMapper.writeValueAsString(event.data),
      service = "CAS2",
      triggerSource = null,
      triggeredByUserId = null,
      schemaVersion = event.schemaVersion,
    ),
  )
}
