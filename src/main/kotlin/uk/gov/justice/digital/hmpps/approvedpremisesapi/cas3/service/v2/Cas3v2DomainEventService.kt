package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3AssessmentUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingCancelledEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingCancelledUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingConfirmedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingProvisionallyMadeEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3DraftReferralDeletedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3Event
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonArrivedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonArrivedUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonDepartedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonDepartureUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3ReferralSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3DomainEventServiceConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.DomainEventUrlConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
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
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.reflect.KClass

@SuppressWarnings("TooManyFunctions", "TooGenericExceptionThrown", "")
@Service
class Cas3v2DomainEventService(
  private val objectMapper: ObjectMapper,
  private val domainEventRepository: DomainEventRepository,
  private val cas3v2DomainEventBuilder: Cas3v2DomainEventBuilder,
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

  @Transactional
  fun saveBookingCancelledEvent(booking: Cas3BookingEntity, user: UserEntity) {
    val domainEvent = cas3v2DomainEventBuilder.getBookingCancelledDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      triggerSourceType = TriggerSourceType.USER,
    )
  }

  @Transactional
  fun saveBookingConfirmedEvent(booking: Cas3BookingEntity, user: UserEntity) {
    val domainEvent = cas3v2DomainEventBuilder.getBookingConfirmedDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      triggerSourceType = TriggerSourceType.USER,
    )
  }

  @Transactional
  fun saveCas3BookingProvisionallyMadeEvent(booking: Cas3BookingEntity, user: UserEntity) {
    val domainEvent = cas3v2DomainEventBuilder.getBookingProvisionallyMadeDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      triggerSourceType = TriggerSourceType.USER,
    )
  }

  @Transactional
  fun savePersonArrivedEvent(booking: Cas3BookingEntity, user: UserEntity) {
    val domainEvent = cas3v2DomainEventBuilder.getPersonArrivedDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      triggerSourceType = TriggerSourceType.USER,
    )
  }

  @Transactional
  fun savePersonArrivedUpdatedEvent(booking: Cas3BookingEntity, user: UserEntity) {
    val domainEvent = cas3v2DomainEventBuilder.buildPersonArrivedUpdatedDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      triggerSourceType = TriggerSourceType.USER,
    )
  }

  @Transactional
  fun savePersonDepartedEvent(booking: Cas3BookingEntity, user: UserEntity) {
    val domainEvent = cas3v2DomainEventBuilder.getPersonDepartedDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      triggerSourceType = TriggerSourceType.USER,
    )
  }

  @Transactional
  fun savePersonDepartureUpdatedEvent(booking: Cas3BookingEntity, user: UserEntity) {
    val domainEvent = cas3v2DomainEventBuilder.buildDepartureUpdatedDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      triggerSourceType = TriggerSourceType.USER,
    )
  }

  @Transactional
  fun saveBookingCancelledUpdatedEvent(booking: Cas3BookingEntity, user: UserEntity) {
    val domainEvent = cas3v2DomainEventBuilder.getBookingCancelledUpdatedDomainEvent(booking, user)

    saveAndEmit(
      domainEvent = domainEvent,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      triggerSourceType = TriggerSourceType.USER,
    )
  }

  private fun <T : CAS3Event> saveAndEmit(
    domainEvent: DomainEvent<T>,
    crn: String?,
    nomsNumber: String?,
    triggerSourceType: TriggerSourceType,
    emit: Boolean = cas3DomainEventServiceConfig.emitForEvent(domainEvent.data.eventType),
  ) {
    val enumType = enumTypeFromDataType(domainEvent.data::class)
    val typeName = enumType.typeName
    val typeDescription = enumType.typeDescription

    val user = userService.getUserForRequestOrNull()

    val cas3PremisesId = when (domainEvent.data) {
      is CAS3PremisesArchiveEvent -> domainEvent.data.eventDetails.premisesId
      is CAS3PremisesUnarchiveEvent -> domainEvent.data.eventDetails.premisesId
      else -> null
    }

    val cas3BedspaceId = when (domainEvent.data) {
      is CAS3BedspaceArchiveEvent -> domainEvent.data.eventDetails.bedspaceId
      is CAS3BedspaceUnarchiveEvent -> domainEvent.data.eventDetails.bedspaceId
      else -> null
    }

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
        cas3CancelledAt = null,
        data = objectMapper.writeValueAsString(domainEvent.data),
        service = "CAS3",
        triggeredByUserId = user?.id,
        triggerSource = triggerSourceType,
        schemaVersion = domainEvent.schemaVersion,
        cas1SpaceBookingId = null,
        cas3PremisesId = cas3PremisesId,
        cas3BedspaceId = cas3BedspaceId,
      ),
    )

    if (emit) {
      val personReferenceIdentifiers = mutableListOf<SnsEventPersonReference>()

      if (nomsNumber != null) {
        personReferenceIdentifiers.add(SnsEventPersonReference("NOMS", nomsNumber))
      }
      if (crn != null) {
        personReferenceIdentifiers.add(SnsEventPersonReference("CRN", crn))
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
          identifiers = listOfNotNull(
            crn?.let { SnsEventPersonReference("CRN", it) },
            nomsNumber?.let { SnsEventPersonReference("NOMS", it) },
          ),
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

  @Transactional
  fun savePremisesUnarchiveEvent(premises: Cas3PremisesEntity, currentStartDate: LocalDate, newStartDate: LocalDate, currentEndDate: LocalDate?) {
    val user = userService.getUserForRequest()
    val domainEvent = cas3v2DomainEventBuilder.getPremisesUnarchiveEvent(premises, currentStartDate, newStartDate, currentEndDate, user)

    saveAndEmit(
      domainEvent = domainEvent,
      crn = domainEvent.crn,
      nomsNumber = domainEvent.nomsNumber,
      triggerSourceType = TriggerSourceType.USER,
      false,
    )
  }

  @Suppress("CyclomaticComplexMethod")
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
    CAS3PremisesArchiveEvent::class -> DomainEventType.CAS3_PREMISES_ARCHIVED
    CAS3PremisesUnarchiveEvent::class -> DomainEventType.CAS3_PREMISES_UNARCHIVED
    CAS3BedspaceArchiveEvent::class -> DomainEventType.CAS3_BEDSPACE_ARCHIVED
    CAS3BedspaceUnarchiveEvent::class -> DomainEventType.CAS3_BEDSPACE_UNARCHIVED
    else -> throw RuntimeException("Unrecognised domain event type: ${type.qualifiedName}")
  }

  fun getBedspacesActiveDomainEvents(ids: List<UUID>, bedspaceDomainEventTypes: List<DomainEventType>): List<DomainEventEntity> = domainEventRepository.findBedspacesActiveDomainEventsByType(
    cas3BedspaceIds = ids,
    bedspaceDomainEventTypes = bedspaceDomainEventTypes.map { it.toString() },
  )

  fun getBedspaceActiveDomainEvents(id: UUID, bedspaceDomainEventTypes: List<DomainEventType>): List<DomainEventEntity> = domainEventRepository.findBedspacesActiveDomainEventsByType(
    listOf(id),
    bedspaceDomainEventTypes.map { it.toString() },
  )
}
