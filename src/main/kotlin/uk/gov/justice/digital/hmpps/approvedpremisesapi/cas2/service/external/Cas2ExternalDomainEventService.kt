package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.external

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2Event
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.external.Cas2ArrivalEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

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

  fun getArrivalEventFromApplicationId(applicationId: UUID) = get<Cas2ArrivalEvent>(applicationId)

  private inline fun <reified T : Cas2Event> get(applicationId: UUID): DomainEvent<T>? {
    val type = when (T::class) {
      Cas2ArrivalEvent::class -> DomainEventType.CAS2_PERSON_ARRIVED
      else -> error("Unsupported CAS2 event type: ${T::class.simpleName}")
    }

    return domainEventRepository.findByApplicationIdAndType(applicationId, type)
      .firstOrNull()
      ?.let { domainEvent ->
        DomainEvent(
          id = domainEvent.id,
          applicationId = domainEvent.applicationId,
          assessmentId = domainEvent.assessmentId,
          bookingId = domainEvent.bookingId,
          crn = domainEvent.crn,
          nomsNumber = domainEvent.nomsNumber,
          occurredAt = domainEvent.occurredAt.toInstant(),
          data = jsonMapper.readValue(domainEvent.data, T::class.java),
          metadata = domainEvent.metadata,
          schemaVersion = domainEvent.schemaVersion,
        )
      }
  }
}
