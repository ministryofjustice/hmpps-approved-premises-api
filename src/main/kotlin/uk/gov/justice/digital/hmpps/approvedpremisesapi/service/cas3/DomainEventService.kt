package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3

import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingCancelledEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingCancelledUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingConfirmedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingProvisionallyMadeEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3Event
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonArrivedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonArrivedUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonDepartedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonDepartureUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3ReferralSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
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
import javax.transaction.Transactional
import kotlin.reflect.KClass

@Service(
  "uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventService",
)
class DomainEventService(
  private val objectMapper: ObjectMapper,
  private val domainEventRepository: DomainEventRepository,
  private val domainEventBuilder: DomainEventBuilder,
  private val hmppsQueueService: HmppsQueueService,
  @Value("\${domain-events.cas3.emit-enabled}") private val emitDomainEventsEnabled: List<EventType>,
  @Value("\${url-templates.api.cas3.booking-cancelled-event-detail}") private val bookingCancelledDetailUrlTemplate: String,
  @Value("\${url-templates.api.cas3.booking-cancelled-updated-event-detail}") private val bookingCancelledUpdatedDetailUrlTemplate: String,
  @Value("\${url-templates.api.cas3.booking-confirmed-event-detail}") private val bookingConfirmedDetailUrlTemplate: String,
  @Value("\${url-templates.api.cas3.booking-provisionally-made-event-detail}") private val bookingProvisionallyMadeDetailUrlTemplate: String,
  @Value("\${url-templates.api.cas3.person-arrived-event-detail}") private val personArrivedDetailUrlTemplate: String,
  @Value("\${url-templates.api.cas3.person-departed-event-detail}") private val personDepartedDetailUrlTemplate: String,
  @Value("\${url-templates.api.cas3.referral-submitted-event-detail}") private val referralSubmittedDetailUrlTemplate: String,
  @Value("\${url-templates.api.cas3.person-departure-updated-event-detail}") private val personDepartureUpdatedDetailUrlTemplate: String,
  @Value("\${url-templates.api.cas3.person-arrived-updated-event-detail}") private val personArrivedUpdatedDetailUrlTemplate: String,
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

  @Transactional
  fun saveBookingCancelledEvent(booking: BookingEntity, user: UserEntity?) {
    val domainEvent = domainEventBuilder.getBookingCancelledDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      typeName = "accommodation.cas3.booking.cancelled",
      typeDescription = "A booking for a Transitional Accommodation premises has been cancelled",
      detailUrl = bookingCancelledDetailUrlTemplate.replace("#eventId", domainEvent.id.toString()),
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )
  }

  @Transactional
  fun saveBookingConfirmedEvent(booking: BookingEntity, user: UserEntity) {
    val domainEvent = domainEventBuilder.getBookingConfirmedDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      typeName = "accommodation.cas3.booking.confirmed",
      typeDescription = "A booking has been confirmed for a Transitional Accommodation premises",
      detailUrl = bookingConfirmedDetailUrlTemplate.replace("#eventId", domainEvent.id.toString()),
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )
  }

  @Transactional
  fun saveBookingProvisionallyMadeEvent(booking: BookingEntity, user: UserEntity) {
    val domainEvent = domainEventBuilder.getBookingProvisionallyMadeDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      typeName = "accommodation.cas3.booking.provisionally-made",
      typeDescription = "A booking has been provisionally made for a Transitional Accommodation premises",
      detailUrl = bookingProvisionallyMadeDetailUrlTemplate.replace("#eventId", domainEvent.id.toString()),
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )
  }

  @Transactional
  fun savePersonArrivedEvent(booking: BookingEntity, user: UserEntity?) {
    val domainEvent = domainEventBuilder.getPersonArrivedDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      typeName = "accommodation.cas3.person.arrived",
      typeDescription = "Someone has arrived at a Transitional Accommodation premises for their booking",
      detailUrl = personArrivedDetailUrlTemplate.replace("#eventId", domainEvent.id.toString()),
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )
  }

  @Transactional
  fun savePersonArrivedUpdatedEvent(booking: BookingEntity, user: UserEntity?) {
    val domainEvent = domainEventBuilder.buildPersonArrivedUpdatedDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      typeName = "accommodation.cas3.person.arrived.updated",
      typeDescription = "Someone has changed arrival date at a Transitional Accommodation premises for their booking",
      detailUrl = personArrivedUpdatedDetailUrlTemplate.replace("#eventId", domainEvent.id.toString()),
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )
  }

  @Transactional
  fun savePersonDepartedEvent(booking: BookingEntity, user: UserEntity?) {
    val domainEvent = domainEventBuilder.getPersonDepartedDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      typeName = EventType.personDeparted.value,
      typeDescription = "Someone has left a Transitional Accommodation premises",
      detailUrl = personDepartedDetailUrlTemplate.replace("#eventId", domainEvent.id.toString()),
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )
  }

  @Transactional
  fun saveReferralSubmittedEvent(application: TemporaryAccommodationApplicationEntity) {
    val domainEvent = domainEventBuilder.getReferralSubmittedDomainEvent(application)

    saveAndEmit(
      domainEvent = domainEvent,
      typeName = EventType.referralSubmitted.value,
      typeDescription = "A referral for Transitional Accommodation has been submitted",
      detailUrl = referralSubmittedDetailUrlTemplate.replace("#eventId", domainEvent.id.toString()),
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )
  }

  @Transactional
  fun savePersonDepartureUpdatedEvent(booking: BookingEntity, user: UserEntity?) {
    val domainEvent = domainEventBuilder.buildDepartureUpdatedDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      typeName = EventType.personDepartureUpdated.value,
      typeDescription = "Person has updated departure date of Transitional Accommodation premises",
      detailUrl = personDepartureUpdatedDetailUrlTemplate.replace("#eventId", domainEvent.id.toString()),
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
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
        assessmentId = domainEvent.assessmentId,
        bookingId = domainEvent.bookingId,
        crn = domainEvent.crn,
        type = enumTypeFromDataType(domainEvent.data::class),
        occurredAt = domainEvent.occurredAt.atOffset(ZoneOffset.UTC),
        createdAt = OffsetDateTime.now(),
        data = objectMapper.writeValueAsString(domainEvent.data),
        service = "CAS3",
        triggeredByUserId = null,
      ),
    )

    if (emitDomainEventsEnabled.contains(domainEvent.data.eventType)) {
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
    else -> throw RuntimeException("Unrecognised domain event type: ${type.qualifiedName}")
  }

  fun saveBookingCancelledUpdatedEvent(booking: BookingEntity, user: UserEntity?) {
    val domainEvent = domainEventBuilder.getBookingCancelledUpdatedDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      typeName = "accommodation.cas3.booking.cancelled.updated",
      typeDescription = "A cancelled booking for a Transitional Accommodation premises has been updated",
      detailUrl = bookingCancelledUpdatedDetailUrlTemplate.replace("#eventId", domainEvent.id.toString()),
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )
  }
}
