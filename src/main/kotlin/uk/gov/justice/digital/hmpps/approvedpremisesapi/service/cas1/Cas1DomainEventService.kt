package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationExpiredEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingKeyWorkerAssignedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventEnvelopeInterface
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EmergencyTransferCreatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.FurtherInformationRequestedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealAcceptedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealCreatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementAppealRejectedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementCreatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.DomainEventUrlConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MetaDataName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TriggerSourceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventAdditionalInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReferenceCollection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ConfiguredDomainEventWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.reflect.KClass

@SuppressWarnings("TooManyFunctions")
@Service
class Cas1DomainEventService(
  private val objectMapper: ObjectMapper,
  private val domainEventRepository: DomainEventRepository,
  val domainEventWorker: ConfiguredDomainEventWorker,
  private val userService: UserService,
  @Value("\${domain-events.cas1.emit-enabled}") private val emitDomainEventsEnabled: Boolean,
  private val domainEventUrlConfig: DomainEventUrlConfig,
  private val cas1DomainEventMigrationService: Cas1DomainEventMigrationService,
  private val sentryService: SentryService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getApplicationSubmittedDomainEvent(id: UUID) = get(id, ApplicationSubmittedEnvelope::class)
  fun getApplicationAssessedDomainEvent(id: UUID) = get(id, ApplicationAssessedEnvelope::class)
  fun getBookingMadeEvent(id: UUID) = get(id, BookingMadeEnvelope::class)
  fun getEmergencyTransferCreatedEvent(id: UUID) = get(id, EmergencyTransferCreatedEnvelope::class)
  fun getPersonArrivedEvent(id: UUID) = get(id, PersonArrivedEnvelope::class)
  fun getPersonNotArrivedEvent(id: UUID) = get(id, PersonNotArrivedEnvelope::class)
  fun getPersonDepartedEvent(id: UUID) = get(id, PersonDepartedEnvelope::class)
  fun getBookingNotMadeEvent(id: UUID) = get(id, BookingNotMadeEnvelope::class)
  fun getBookingCancelledEvent(id: UUID) = get(id, BookingCancelledEnvelope::class)
  fun getBookingChangedEvent(id: UUID) = get(id, BookingChangedEnvelope::class)
  fun getBookingKeyWorkerAssignedEvent(id: UUID) = get(id, BookingKeyWorkerAssignedEnvelope::class)
  fun getApplicationWithdrawnEvent(id: UUID) = get(id, ApplicationWithdrawnEnvelope::class)
  fun getApplicationExpiredEvent(id: UUID) = get(id, ApplicationExpiredEnvelope::class)
  fun getPlacementAppealAcceptedEvent(id: UUID) = get(id, PlacementAppealAcceptedEnvelope::class)
  fun getPlacementAppealCreatedEvent(id: UUID) = get(id, PlacementAppealCreatedEnvelope::class)
  fun getPlacementAppealRejectedEvent(id: UUID) = get(id, PlacementAppealRejectedEnvelope::class)
  fun getPlacementApplicationWithdrawnEvent(id: UUID) = get(id, PlacementApplicationWithdrawnEnvelope::class)
  fun getPlacementApplicationAllocatedEvent(id: UUID) = get(id, PlacementApplicationAllocatedEnvelope::class)
  fun getMatchRequestWithdrawnEvent(id: UUID) = get(id, MatchRequestWithdrawnEnvelope::class)
  fun getAssessmentAppealedEvent(id: UUID) = get(id, AssessmentAppealedEnvelope::class)
  fun getAssessmentAllocatedEvent(id: UUID) = get(id, AssessmentAllocatedEnvelope::class)
  fun getRequestForPlacementCreatedEvent(id: UUID) = get(id, RequestForPlacementCreatedEnvelope::class)
  fun getRequestForPlacementAssessedEvent(id: UUID) = get(id, RequestForPlacementAssessedEnvelope::class)
  fun getFurtherInformationRequestMadeEvent(id: UUID) = get(id, FurtherInformationRequestedEnvelope::class)

  private fun <T : Cas1DomainEventEnvelopeInterface<*>> get(id: UUID, envelopeType: KClass<T>): GetCas1DomainEvent<T>? {
    val entity = domainEventRepository.findByIdOrNull(id) ?: return null
    return toDomainEvent(entity, envelopeType)
  }

  @SuppressWarnings("CyclomaticComplexMethod", "TooGenericExceptionThrown")
  fun <T : Cas1DomainEventEnvelopeInterface<*>> toDomainEvent(entity: DomainEventEntity, envelopeType: KClass<T>): GetCas1DomainEvent<T> {
    checkNotNull(entity.applicationId) { "application id should not be null" }

    if (entity.type.cas1Info!!.envelopeType != envelopeType) {
      error(
        "Entity with id ${entity.id} has type ${entity.type}, which contains data of type ${entity.type.cas1Info!!.envelopeType}. " +
          "This is incompatible with the requested data type $envelopeType.",
      )
    }

    val dataJson = when (entity.type) {
      DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED -> cas1DomainEventMigrationService.bookingCancelledJson(entity)
      DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED -> cas1DomainEventMigrationService.personArrivedJson(entity)
      DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED -> cas1DomainEventMigrationService.personDepartedJson(entity)
      else -> entity.data
    }

    val data = objectMapper.readValue(dataJson, envelopeType.java)

    return GetCas1DomainEvent(
      id = entity.id,
      data = data,
      schemaVersion = entity.schemaVersion,
    )
  }

  @Transactional
  fun saveApplicationSubmittedDomainEvent(domainEvent: Cas1DomainEvent<ApplicationSubmittedEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED,
  )

  @Transactional
  fun saveApplicationAssessedDomainEvent(domainEvent: Cas1DomainEvent<ApplicationAssessedEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED,
  )

  @Transactional
  fun saveBookingMadeDomainEvent(domainEvent: Cas1DomainEvent<BookingMadeEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_BOOKING_MADE,
  )

  @Transactional
  fun saveEmergencyTransferCreatedEvent(domainEvent: Cas1DomainEvent<EmergencyTransferCreatedEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_EMERGENCY_TRANSFER_CREATED,
  )

  @Transactional
  fun savePersonArrivedEvent(domainEvent: Cas1DomainEvent<PersonArrivedEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED,
  )

  @Transactional
  fun savePersonNotArrivedEvent(domainEvent: Cas1DomainEvent<PersonNotArrivedEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED,
  )

  @Transactional
  fun savePersonDepartedEvent(domainEvent: Cas1DomainEvent<PersonDepartedEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED,
  )

  @Transactional
  fun saveBookingNotMadeEvent(domainEvent: Cas1DomainEvent<BookingNotMadeEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE,
  )

  @Transactional
  fun saveBookingCancelledEvent(domainEvent: Cas1DomainEvent<BookingCancelledEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED,
  )

  @Transactional
  fun saveBookingChangedEvent(domainEvent: Cas1DomainEvent<BookingChangedEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED,
  )

  @Transactional
  fun saveApplicationWithdrawnEvent(domainEvent: Cas1DomainEvent<ApplicationWithdrawnEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN,
  )

  @Transactional
  fun saveAssessmentAppealedEvent(domainEvent: Cas1DomainEvent<AssessmentAppealedEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED,
  )

  @Transactional
  fun savePlacementAppealAccepted(domainEvent: Cas1DomainEvent<PlacementAppealAcceptedEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_PLACEMENT_APPEAL_ACCEPTED,
  )

  @Transactional
  fun savePlacementAppealCreated(domainEvent: Cas1DomainEvent<PlacementAppealCreatedEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_PLACEMENT_APPEAL_CREATED,
  )

  @Transactional
  fun savePlacementAppealRejected(domainEvent: Cas1DomainEvent<PlacementAppealRejectedEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_PLACEMENT_APPEAL_REJECTED,
  )

  @Transactional
  fun savePlacementApplicationWithdrawnEvent(domainEvent: Cas1DomainEvent<PlacementApplicationWithdrawnEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN,
  )

  @Transactional
  fun savePlacementApplicationAllocatedEvent(domainEvent: Cas1DomainEvent<PlacementApplicationAllocatedEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED,
  )

  @Transactional
  fun saveMatchRequestWithdrawnEvent(domainEvent: Cas1DomainEvent<MatchRequestWithdrawnEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN,
  )

  @Transactional
  fun saveRequestForPlacementCreatedEvent(domainEvent: Cas1DomainEvent<RequestForPlacementCreatedEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED,
  )

  @Transactional
  fun saveRequestForPlacementAssessedEvent(domainEvent: Cas1DomainEvent<RequestForPlacementAssessedEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED,
  )

  @Transactional
  fun saveApplicationExpiredEvent(domainEvent: Cas1DomainEvent<ApplicationExpiredEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_APPLICATION_EXPIRED,
  )

  @Transactional
  fun saveAssessmentAllocatedEvent(domainEvent: Cas1DomainEvent<AssessmentAllocatedEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED,
  )

  @Transactional
  fun saveFurtherInformationRequestedEvent(domainEvent: Cas1DomainEvent<FurtherInformationRequestedEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED,
  )

  @Transactional
  fun saveKeyWorkerAssignedEvent(domainEvent: Cas1DomainEvent<BookingKeyWorkerAssignedEnvelope>) = saveAndEmit(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_BOOKING_KEYWORKER_ASSIGNED,
  )

  fun getAllDomainEventsForApplication(applicationId: UUID) = getAllDomainEventsById(applicationId = applicationId)

  fun getAllDomainEventsForSpaceBooking(spaceBookingId: UUID) = getAllDomainEventsById(spaceBookingId = spaceBookingId)

  private fun getAllDomainEventsById(applicationId: UUID? = null, spaceBookingId: UUID? = null) = domainEventRepository.findAllTimelineEventsByIds(applicationId, spaceBookingId).distinctBy { it.id }

  @Transactional
  fun saveAndEmit(
    domainEvent: Cas1DomainEvent<*>,
    eventType: DomainEventType,
  ) {
    val domainEventEntity = domainEventRepository.save(
      DomainEventEntity(
        id = domainEvent.id,
        applicationId = domainEvent.applicationId,
        assessmentId = domainEvent.assessmentId,
        bookingId = domainEvent.bookingId,
        cas1SpaceBookingId = domainEvent.cas1SpaceBookingId,
        cas1PlacementRequestId = domainEvent.cas1PlacementRequestId,
        crn = domainEvent.crn,
        type = eventType,
        occurredAt = domainEvent.occurredAt.atOffset(ZoneOffset.UTC),
        createdAt = OffsetDateTime.now(),
        data = objectMapper.writeValueAsString(domainEvent.data),
        service = "CAS1",
        triggerSource = domainEvent.triggerSource,
        triggeredByUserId = userService.getUserForRequestOrNull()?.id,
        nomsNumber = domainEvent.nomsNumber,
        metadata = domainEvent.metadata,
        schemaVersion = domainEvent.schemaVersion,
      ),
    )

    val emittable = eventType.cas1Info!!.emittable

    if (emittable && domainEvent.emit) {
      emit(domainEventEntity)
    } else {
      log.debug("Not emitting domain event of type $eventType. Type emittable? $emittable. Emit? ${domainEvent.emit}")
    }
  }

  fun replay(domainEventId: UUID) {
    val domainEventEntity = domainEventRepository.findByIdOrNull(domainEventId)
      ?: throw NotFoundProblem(domainEventId, "DomainEvent")

    emit(domainEventEntity)
  }

  private fun emit(
    domainEvent: DomainEventEntity,
  ) {
    val eventType = domainEvent.type
    val typeName = eventType.typeName

    if (!eventType.cas1Info!!.emittable) {
      sentryService.captureErrorMessage("An attempt was made to emit domain event ${domainEvent.id} of type $typeName which is not emittable")
      return
    }

    if (!emitDomainEventsEnabled) {
      log.info("Not emitting SNS event for domain event because domain-events.cas1.emit-enabled is not enabled")
      return
    }

    val typeDescription = eventType.typeDescription
    val crn = domainEvent.crn
    val nomsNumber = domainEvent.nomsNumber ?: "Unknown NOMS Number"
    val detailUrl = domainEventUrlConfig.getUrlForDomainEventId(eventType, domainEvent.id)

    domainEventWorker.emitEvent(
      SnsEvent(
        eventType = typeName,
        version = 1,
        description = typeDescription,
        detailUrl = detailUrl,
        occurredAt = domainEvent.occurredAt,
        additionalInformation = SnsEventAdditionalInformation(
          applicationId = domainEvent.applicationId,
        ),
        personReference = SnsEventPersonReferenceCollection(
          identifiers = listOf(
            SnsEventPersonReference("CRN", crn),
            SnsEventPersonReference("NOMS", nomsNumber),
          ),
        ),
      ),
      domainEvent.id,
    )
  }
}

data class GetCas1DomainEvent<T>(
  val id: UUID,
  val data: T,
  val schemaVersion: Int? = null,
)

data class Cas1DomainEvent<T>(
  val id: UUID,
  val applicationId: UUID? = null,
  val assessmentId: UUID? = null,
  val bookingId: UUID? = null,
  val cas1SpaceBookingId: UUID? = null,
  val cas1PlacementRequestId: UUID? = null,
  val crn: String,
  val nomsNumber: String?,
  val occurredAt: Instant,
  val data: T,
  val metadata: Map<MetaDataName, String?> = emptyMap(),
  val schemaVersion: Int? = null,
  val triggerSource: TriggerSourceType? = null,
  val emit: Boolean = true,
)
