package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import java.time.OffsetDateTime
import java.util.UUID
import javax.transaction.Transactional

@Service
class DomainEventService(
  private val objectMapper: ObjectMapper,
  private val domainEventRepository: DomainEventRepository
) {
  fun getApplicationSubmittedDomainEvent(id: UUID) = get<ApplicationSubmittedEnvelope>(id)

  private inline fun <reified T> get(id: UUID): DomainEvent<T>? {
    val domainEventEntity = domainEventRepository.findByIdOrNull(id) ?: return null

    val data = when {
      T::class == ApplicationSubmittedEnvelope::class && domainEventEntity.type == DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED ->
        objectMapper.readValue(domainEventEntity.data, T::class.java)
      else -> throw RuntimeException("Unsupported DomainEventData type ${T::class.qualifiedName}/${domainEventEntity.type.name}")
    }

    return DomainEvent(
      id = domainEventEntity.id,
      applicationId = domainEventEntity.applicationId,
      crn = domainEventEntity.crn,
      occurredAt = domainEventEntity.occurredAt,
      data = data
    )
  }

  @Transactional
  fun save(domainEvent: DomainEvent<*>) {
    domainEventRepository.save(
      DomainEventEntity(
        id = domainEvent.id,
        applicationId = domainEvent.applicationId,
        crn = domainEvent.crn,
        type = enumTypeFromDataType(domainEvent.data!!::class.java),
        occurredAt = domainEvent.occurredAt,
        createdAt = OffsetDateTime.now(),
        data = objectMapper.writeValueAsString(domainEvent.data)
      )
    )

    // TODO: Emit certain types of event to SNS for downstream consumption
  }

  private fun <T> enumTypeFromDataType(type: Class<T>) = when (type) {
    ApplicationSubmittedEnvelope::class.java -> DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED
    else -> throw RuntimeException("Unrecognised domain event type: ${type.name}")
  }
}
