package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3

import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3Event
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventAdditionalInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReferenceCollection
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.reflect.KClass

@Service(
  "uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventService",
)
class DomainEventService(
  private val objectMapper: ObjectMapper,
  private val domainEventRepository: DomainEventRepository,
  private val domainEventBuilder: DomainEventBuilder,
  private val hmppsQueueService: HmppsQueueService,
  @Value("\${domain-events.cas3.emit-enabled}") private val emitDomainEventsEnabled: Boolean,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  private val domainTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
      ?: throw MissingTopicException("domainevents not found")
  }

  private inline fun <reified T : CAS3Event> get(id: UUID): DomainEvent<T>? {
    val domainEventEntity = domainEventRepository.findByIdOrNull(id) ?: return null

    val data = when {
      enumTypeFromDataType(T::class) == domainEventEntity.type ->
        objectMapper.readValue(domainEventEntity.data, T::class.java)
      else -> throw RuntimeException("Unsupported DomainEventData type ${T::class.qualifiedName}/${domainEventEntity.type.name}")
    }
    return DomainEvent(
      id = domainEventEntity.id,
      applicationId = domainEventEntity.applicationId,
      bookingId = domainEventEntity.bookingId,
      crn = domainEventEntity.crn,
      occurredAt = domainEventEntity.occurredAt.toInstant(),
      data = data,
    )
  }

  private fun <T : CAS3Event> saveAndEmit(
    domainEvent: DomainEvent<T>,
    typeName: String,
    typeDescription: String,
    detailUrl: String,
    crn: String,
    nomsNumber: String?,
  ) {
    domainEventRepository.save(
      DomainEventEntity(
        id = domainEvent.id,
        applicationId = domainEvent.applicationId,
        bookingId = domainEvent.bookingId,
        crn = domainEvent.crn,
        type = enumTypeFromDataType(domainEvent.data::class),
        occurredAt = domainEvent.occurredAt.atOffset(ZoneOffset.UTC),
        createdAt = OffsetDateTime.now(),
        data = objectMapper.writeValueAsString(domainEvent.data),
        service = "CAS3",
      ),
    )

    if (emitDomainEventsEnabled) {
      val personReferenceIdentifiers = when (nomsNumber) {
        null -> listOf(
          SnsEventPersonReference("CRN", crn),
        )
        else -> listOf(
          SnsEventPersonReference("CRN", crn),
          SnsEventPersonReference("NOMS", nomsNumber),
        )
      }

      val snsEvent = SnsEvent(
        eventType = typeName,
        version = 1,
        description = typeDescription,
        detailUrl = detailUrl,
        occurredAt = domainEvent.occurredAt.atOffset(ZoneOffset.UTC),
        additionalInformation = SnsEventAdditionalInformation(
          applicationId = domainEvent.applicationId,
          bookingId = domainEvent.bookingId,
        ),
        personReference = SnsEventPersonReferenceCollection(
          identifiers = personReferenceIdentifiers,
        ),
      )

      val publishResult = domainTopic.snsClient.publish(
        PublishRequest(domainTopic.arn, objectMapper.writeValueAsString(snsEvent))
          .withMessageAttributes(mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue(snsEvent.eventType))),
      )

      log.info("Emitted SNS event (Message Id: ${publishResult.messageId}, Sequence Id: ${publishResult.sequenceNumber}) for Domain Event: ${domainEvent.id} of type: ${snsEvent.eventType}")
    } else {
      log.info("Not emitting SNS event for domain event because domain-events.cas3.emit-enabled is not enabled")
    }
  }

  private fun <T : CAS3Event> enumTypeFromDataType(type: KClass<T>): DomainEventType = when (type) {
    else -> throw RuntimeException("Unrecognised domain event type: ${type.qualifiedName}")
  }
}
