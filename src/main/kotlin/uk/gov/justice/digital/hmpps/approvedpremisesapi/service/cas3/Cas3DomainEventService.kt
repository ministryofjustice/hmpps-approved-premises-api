package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3AssessmentUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingCancelledEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingCancelledUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingConfirmedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingProvisionallyMadeEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3DraftReferralDeletedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3Event
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonArrivedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonArrivedUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonDepartedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonDepartureUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3ReferralSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.DomainEventUrlConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TriggerSourceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventAdditionalInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReferenceCollection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.reflect.KClass

@Service("uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventServiceConfig")
class Cas3DomainEventServiceConfig(
  @Value("\${domain-events.cas3.emit-enabled}") val domainEventsWithEmitEnabled: List<EventType>,
) {
  fun emitForEvent(eventType: EventType) = domainEventsWithEmitEnabled.contains(eventType)
}

@SuppressWarnings("TooManyFunctions", "TooGenericExceptionThrown", "")
@Service("uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventService")
class Cas3DomainEventService(
  private val objectMapper: ObjectMapper,
  private val domainEventRepository: DomainEventRepository,
  private val cas3DomainEventBuilder: Cas3DomainEventBuilder,
  private val hmppsQueueService: HmppsQueueService,
  private val cas3DomainEventServiceConfig: Cas3DomainEventServiceConfig,
  private val domainEventUrlConfig: DomainEventUrlConfig,
  private val userService: UserService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  private val domainTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
      ?: throw MissingTopicException("domainevents not found")
  }

  fun getBookingCancelledEvent(id: UUID) = get<CAS3BookingCancelledEvent>(id)

  fun getBookingConfirmedEvent(id: UUID) = get<CAS3BookingConfirmedEvent>(id)

  fun getBookingProvisionallyMadeEvent(id: UUID) = get<CAS3BookingProvisionallyMadeEvent>(id)

  fun getPersonArrivedEvent(id: UUID) = get<CAS3PersonArrivedEvent>(id)

  fun getPersonArrivedUpdatedEvent(id: UUID) = get<CAS3PersonArrivedUpdatedEvent>(id)

  fun getPersonDepartedEvent(id: UUID) = get<CAS3PersonDepartedEvent>(id)

  fun getReferralSubmittedEvent(id: UUID) = get<CAS3ReferralSubmittedEvent>(id)

  fun getPersonDepartureUpdatedEvent(id: UUID) = get<CAS3PersonDepartureUpdatedEvent>(id)

  fun getBookingCancelledUpdatedEvent(id: UUID) = get<CAS3BookingCancelledUpdatedEvent>(id)

  fun getAssessmentUpdatedEvents(assessmentId: UUID) = domainEventRepository.findByAssessmentIdAndType(assessmentId, DomainEventType.CAS3_ASSESSMENT_UPDATED)

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
      nomsNumber = domainEventEntity.nomsNumber,
      occurredAt = domainEventEntity.occurredAt.toInstant(),
      data = data,
    )
  }

  @Transactional
  fun saveBookingCancelledEvent(booking: BookingEntity, user: UserEntity) {
    val domainEvent = cas3DomainEventBuilder.getBookingCancelledDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      triggerSourceType = TriggerSourceType.USER,
    )
  }

  @Transactional
  fun saveBookingConfirmedEvent(booking: BookingEntity, user: UserEntity) {
    val domainEvent = cas3DomainEventBuilder.getBookingConfirmedDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      triggerSourceType = TriggerSourceType.USER,
    )
  }

  @Transactional
  fun saveBookingProvisionallyMadeEvent(booking: BookingEntity, user: UserEntity) {
    val domainEvent = cas3DomainEventBuilder.getBookingProvisionallyMadeDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      triggerSourceType = TriggerSourceType.USER,
    )
  }

  @Transactional
  fun savePersonArrivedEvent(booking: BookingEntity, user: UserEntity) {
    val domainEvent = cas3DomainEventBuilder.getPersonArrivedDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      triggerSourceType = TriggerSourceType.USER,
    )
  }

  @Transactional
  fun savePersonArrivedUpdatedEvent(booking: BookingEntity, user: UserEntity) {
    val domainEvent = cas3DomainEventBuilder.buildPersonArrivedUpdatedDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      triggerSourceType = TriggerSourceType.USER,
    )
  }

  @Transactional
  fun savePersonDepartedEvent(booking: BookingEntity, user: UserEntity) {
    val domainEvent = cas3DomainEventBuilder.getPersonDepartedDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      triggerSourceType = TriggerSourceType.USER,
    )
  }

  @Transactional
  fun saveReferralSubmittedEvent(application: TemporaryAccommodationApplicationEntity) {
    val domainEvent = cas3DomainEventBuilder.getReferralSubmittedDomainEvent(application)

    saveAndEmit(
      domainEvent = domainEvent,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      triggerSourceType = TriggerSourceType.USER,
    )
  }

  @Transactional
  fun savePersonDepartureUpdatedEvent(booking: BookingEntity, user: UserEntity) {
    val domainEvent = cas3DomainEventBuilder.buildDepartureUpdatedDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      triggerSourceType = TriggerSourceType.USER,
    )
  }

  private fun <T : CAS3Event> saveAndEmit(
    domainEvent: DomainEvent<T>,
    crn: String,
    nomsNumber: String?,
    triggerSourceType: TriggerSourceType,
    emit: Boolean = cas3DomainEventServiceConfig.emitForEvent(domainEvent.data.eventType),
  ) {
    val enumType = enumTypeFromDataType(domainEvent.data::class)
    val typeName = enumType.typeName
    val typeDescription = enumType.typeDescription

    val user = userService.getUserForRequestOrNull()

    domainEventRepository.save(
      DomainEventEntity(
        id = domainEvent.id,
        applicationId = domainEvent.applicationId,
        assessmentId = domainEvent.assessmentId,
        bookingId = domainEvent.bookingId,
        crn = domainEvent.crn,
        nomsNumber = domainEvent.nomsNumber,
        type = enumTypeFromDataType(domainEvent.data::class),
        occurredAt = domainEvent.occurredAt.atOffset(ZoneOffset.UTC),
        createdAt = OffsetDateTime.now(),
        data = objectMapper.writeValueAsString(domainEvent.data),
        service = "CAS3",
        triggeredByUserId = user?.id,
        triggerSource = triggerSourceType,
        schemaVersion = domainEvent.schemaVersion,
        cas1SpaceBookingId = domainEvent.cas1SpaceBookingId,
      ),
    )

    if (emit) {
      val personReferenceIdentifiers = when (nomsNumber) {
        null -> listOf(
          SnsEventPersonReference("CRN", crn),
        )
        else -> listOf(
          SnsEventPersonReference("CRN", crn),
          SnsEventPersonReference("NOMS", nomsNumber),
        )
      }

      val detailUrl = domainEventUrlConfig.getUrlForDomainEventId(enumType, domainEvent.id)

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
        PublishRequest.builder()
          .topicArn(domainTopic.arn)
          .message(objectMapper.writeValueAsString(snsEvent))
          .messageAttributes(
            mapOf(
              "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(snsEvent.eventType).build(),
            ),
          ).build(),
      ).get()

      log.info("Emitted SNS event (Message Id: ${publishResult.messageId()}, Sequence Id: ${publishResult.sequenceNumber()}) for Domain Event: ${domainEvent.id} of type: ${snsEvent.eventType}")
    } else {
      log.info("Not emitting SNS event for domain event because domain-events.cas3.emit-enabled does not contain '${domainEvent.data.eventType}'")
    }
  }

  private fun <T : CAS3Event> enumTypeFromDataType(type: KClass<T>): DomainEventType = when (type) {
    CAS3BookingCancelledEvent::class -> DomainEventType.CAS3_BOOKING_CANCELLED
    CAS3BookingCancelledUpdatedEvent::class -> DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED
    CAS3BookingConfirmedEvent::class -> DomainEventType.CAS3_BOOKING_CONFIRMED
    CAS3BookingProvisionallyMadeEvent::class -> DomainEventType.CAS3_BOOKING_PROVISIONALLY_MADE
    CAS3PersonArrivedEvent::class -> DomainEventType.CAS3_PERSON_ARRIVED
    CAS3PersonArrivedUpdatedEvent::class -> DomainEventType.CAS3_PERSON_ARRIVED_UPDATED
    CAS3PersonDepartedEvent::class -> DomainEventType.CAS3_PERSON_DEPARTED
    CAS3ReferralSubmittedEvent::class -> DomainEventType.CAS3_REFERRAL_SUBMITTED
    CAS3PersonDepartureUpdatedEvent::class -> DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED
    CAS3AssessmentUpdatedEvent::class -> DomainEventType.CAS3_ASSESSMENT_UPDATED
    CAS3DraftReferralDeletedEvent::class -> DomainEventType.CAS3_DRAFT_REFERRAL_DELETED
    else -> throw RuntimeException("Unrecognised domain event type: ${type.qualifiedName}")
  }

  fun saveBookingCancelledUpdatedEvent(booking: BookingEntity, user: UserEntity) {
    val domainEvent = cas3DomainEventBuilder.getBookingCancelledUpdatedDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      triggerSourceType = TriggerSourceType.USER,
    )
  }

  fun saveAssessmentUpdatedEvent(event: DomainEvent<CAS3AssessmentUpdatedEvent>) {
    saveAndEmit(
      domainEvent = event,
      crn = event.crn,
      nomsNumber = null,
      triggerSourceType = TriggerSourceType.USER,
      emit = false,
    )
  }

  @Transactional
  fun saveDraftReferralDeletedEvent(application: ApplicationEntity, user: UserEntity) {
    val domainEvent = cas3DomainEventBuilder.getDraftReferralDeletedEvent(application, user)

    saveAndEmit(
      domainEvent = domainEvent,
      crn = domainEvent.data.eventDetails.crn,
      nomsNumber = null,
      triggerSourceType = TriggerSourceType.USER,
      emit = false,
    )
  }
}
