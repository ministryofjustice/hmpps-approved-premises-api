package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationExpired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationExpiredManually
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmitted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationWithdrawn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAllocated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAppealed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingCancelled
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingChanged
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingKeyWorkerAssigned
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingKeyWorkerAssignedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.FurtherInformationRequested
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.FurtherInformationRequestedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.MatchRequestWithdrawn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonArrived
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonDeparted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonNotArrived
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationAllocated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationWithdrawn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementAssessed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementCreated
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
  @param:Value("\${domain-events.cas1.emit-enabled}") private val emitDomainEventsEnabled: Boolean,
  private val domainEventUrlConfig: DomainEventUrlConfig,
  private val cas1DomainEventMigrationService: Cas1DomainEventMigrationService,
  private val sentryService: SentryService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getApplicationSubmittedDomainEvent(id: UUID) = get(id, ApplicationSubmitted::class)
  fun getApplicationAssessedDomainEvent(id: UUID) = get(id, ApplicationAssessed::class)
  fun getBookingMadeEvent(id: UUID) = get(id, BookingMade::class)
  fun getPersonArrivedEvent(id: UUID) = get(id, PersonArrived::class)
  fun getPersonNotArrivedEvent(id: UUID) = get(id, PersonNotArrived::class)
  fun getPersonDepartedEvent(id: UUID) = get(id, PersonDeparted::class)
  fun getBookingNotMadeEvent(id: UUID) = get(id, BookingNotMade::class)
  fun getBookingCancelledEvent(id: UUID) = get(id, BookingCancelled::class)
  fun getBookingChangedEvent(id: UUID) = get(id, BookingChanged::class)
  fun getBookingKeyWorkerAssignedEvent(id: UUID) = get(id, BookingKeyWorkerAssigned::class)
  fun getApplicationWithdrawnEvent(id: UUID) = get(id, ApplicationWithdrawn::class)
  fun getApplicationExpiredEvent(id: UUID) = get(id, ApplicationExpired::class)
  fun getApplicationExpiredManuallyEvent(id: UUID) = get(id, ApplicationExpiredManually::class)
  fun getPlacementApplicationWithdrawnEvent(id: UUID) = get(id, PlacementApplicationWithdrawn::class)
  fun getPlacementApplicationAllocatedEvent(id: UUID) = get(id, PlacementApplicationAllocated::class)
  fun getMatchRequestWithdrawnEvent(id: UUID) = get(id, MatchRequestWithdrawn::class)
  fun getAssessmentAppealedEvent(id: UUID) = get(id, AssessmentAppealed::class)
  fun getAssessmentAllocatedEvent(id: UUID) = get(id, AssessmentAllocated::class)
  fun getRequestForPlacementCreatedEvent(id: UUID) = get(id, RequestForPlacementCreated::class)
  fun getRequestForPlacementAssessedEvent(id: UUID) = get(id, RequestForPlacementAssessed::class)
  fun getFurtherInformationRequestMadeEvent(id: UUID) = get(id, FurtherInformationRequested::class)

  fun <T : Cas1DomainEventPayload> get(id: UUID, payloadType: KClass<T>): GetCas1DomainEvent<Cas1DomainEventEnvelope<T>>? {
    val entity = domainEventRepository.findByIdOrNull(id) ?: return null
    return toDomainEvent(entity, payloadType)
  }

  @SuppressWarnings("CyclomaticComplexMethod", "TooGenericExceptionThrown")
  fun <T : Cas1DomainEventPayload> toDomainEvent(entity: DomainEventEntity, payloadType: KClass<T>): GetCas1DomainEvent<Cas1DomainEventEnvelope<T>> {
    checkNotNull(entity.applicationId) { "application id should not be null" }

    if (entity.type.cas1Info!!.payloadType != payloadType) {
      error(
        "Entity with id ${entity.id} has type ${entity.type}, which contains data of type ${entity.type.cas1Info!!.payloadType}. " +
          "This is incompatible with the requested payload type $payloadType.",
      )
    }

    val dataJson = when (entity.type) {
      DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED -> cas1DomainEventMigrationService.bookingCancelledJson(entity)
      DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED -> cas1DomainEventMigrationService.personArrivedJson(entity)
      DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED -> cas1DomainEventMigrationService.personDepartedJson(entity)
      else -> entity.data
    }

    val data = objectMapper.readValue<Cas1DomainEventEnvelope<T>>(
      dataJson,
      objectMapper.typeFactory.constructParametricType(
        Cas1DomainEventEnvelope::class.java,
        payloadType.java,
      ),
    )

    return GetCas1DomainEvent(
      id = entity.id,
      data = data,
      schemaVersion = entity.schemaVersion,
      spaceBookingId = entity.cas1SpaceBookingId,
    )
  }

  @Transactional
  fun saveApplicationSubmittedDomainEvent(domainEvent: SaveCas1DomainEvent<ApplicationSubmittedEnvelope>) = saveAndEmitForEnvelope(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED,
  )

  @Transactional
  fun saveApplicationAssessedDomainEvent(domainEvent: SaveCas1DomainEvent<ApplicationAssessedEnvelope>) = saveAndEmitForEnvelope(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED,
  )

  @Transactional
  fun saveBookingMadeDomainEvent(domainEvent: SaveCas1DomainEvent<BookingMadeEnvelope>) = saveAndEmitForEnvelope(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_BOOKING_MADE,
  )

  @Transactional
  fun savePersonArrivedEvent(domainEvent: SaveCas1DomainEvent<PersonArrivedEnvelope>) = saveAndEmitForEnvelope(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED,
  )

  @Transactional
  fun savePersonNotArrivedEvent(domainEvent: SaveCas1DomainEvent<PersonNotArrivedEnvelope>) = saveAndEmitForEnvelope(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED,
  )

  @Transactional
  fun savePersonDepartedEvent(domainEvent: SaveCas1DomainEvent<PersonDepartedEnvelope>) = saveAndEmitForEnvelope(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED,
  )

  @Transactional
  fun saveBookingNotMadeEvent(domainEvent: SaveCas1DomainEvent<BookingNotMadeEnvelope>) = saveAndEmitForEnvelope(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE,
  )

  @Transactional
  fun saveBookingCancelledEvent(domainEvent: SaveCas1DomainEvent<BookingCancelledEnvelope>) = saveAndEmitForEnvelope(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED,
  )

  @Transactional
  fun saveBookingChangedEvent(domainEvent: SaveCas1DomainEvent<BookingChangedEnvelope>) = saveAndEmitForEnvelope(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED,
  )

  @Transactional
  fun saveApplicationWithdrawnEvent(domainEvent: SaveCas1DomainEvent<ApplicationWithdrawnEnvelope>) = saveAndEmitForEnvelope(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN,
  )

  @Transactional
  fun saveAssessmentAppealedEvent(domainEvent: SaveCas1DomainEvent<AssessmentAppealedEnvelope>) = saveAndEmitForEnvelope(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED,
  )

  @Transactional
  fun savePlacementApplicationWithdrawnEvent(domainEvent: SaveCas1DomainEvent<PlacementApplicationWithdrawnEnvelope>) = saveAndEmitForEnvelope(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN,
  )

  @Transactional
  fun savePlacementApplicationAllocatedEvent(domainEvent: SaveCas1DomainEvent<PlacementApplicationAllocatedEnvelope>) = saveAndEmitForEnvelope(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED,
  )

  @Transactional
  fun saveMatchRequestWithdrawnEvent(domainEvent: SaveCas1DomainEvent<MatchRequestWithdrawnEnvelope>) = saveAndEmitForEnvelope(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN,
  )

  @Transactional
  fun saveRequestForPlacementCreatedEvent(domainEvent: SaveCas1DomainEvent<RequestForPlacementCreatedEnvelope>) = saveAndEmitForEnvelope(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED,
  )

  @Transactional
  fun saveRequestForPlacementAssessedEvent(domainEvent: SaveCas1DomainEvent<RequestForPlacementAssessedEnvelope>) = saveAndEmitForEnvelope(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED,
  )

  @Transactional
  fun saveAssessmentAllocatedEvent(domainEvent: SaveCas1DomainEvent<AssessmentAllocatedEnvelope>) = saveAndEmitForEnvelope(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED,
  )

  @Transactional
  fun saveFurtherInformationRequestedEvent(domainEvent: SaveCas1DomainEvent<FurtherInformationRequestedEnvelope>) = saveAndEmitForEnvelope(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED,
  )

  @Transactional
  fun saveKeyWorkerAssignedEvent(domainEvent: SaveCas1DomainEvent<BookingKeyWorkerAssignedEnvelope>) = saveAndEmitForEnvelope(
    domainEvent = domainEvent,
    eventType = DomainEventType.APPROVED_PREMISES_BOOKING_KEYWORKER_ASSIGNED,
  )

  fun getAllDomainEventsForApplication(applicationId: UUID) = getAllDomainEventsById(applicationId = applicationId)

  fun getAllDomainEventsForSpaceBooking(spaceBookingId: UUID) = getAllDomainEventsById(spaceBookingId = spaceBookingId)

  private fun getAllDomainEventsById(applicationId: UUID? = null, spaceBookingId: UUID? = null) = domainEventRepository.findAllTimelineEventsByIds(applicationId, spaceBookingId).distinctBy { it.id }

  fun save(saveWithPayload: SaveCas1DomainEventWithPayload<*>) {
    val id = saveWithPayload.id
    val type = saveWithPayload.type
    val expectedPayloadType = type.cas1Info!!.payloadType
    val actualPayloadType = saveWithPayload.data::class

    if (actualPayloadType != expectedPayloadType) {
      error("expected payload type of $expectedPayloadType, but got $actualPayloadType")
    }

    val envelope = Cas1DomainEventEnvelope(
      id = id,
      timestamp = saveWithPayload.occurredAt,
      eventType = type.cas1Info.apiType,
      eventDetails = saveWithPayload.data,
    )

    val saveWithEnvelope = SaveCas1DomainEvent(
      id = id,
      applicationId = saveWithPayload.applicationId,
      assessmentId = saveWithPayload.assessmentId,
      bookingId = saveWithPayload.bookingId,
      cas1SpaceBookingId = saveWithPayload.cas1SpaceBookingId,
      crn = saveWithPayload.crn,
      nomsNumber = saveWithPayload.nomsNumber,
      occurredAt = saveWithPayload.occurredAt,
      data = envelope,
      metadata = saveWithPayload.metadata,
      schemaVersion = saveWithPayload.schemaVersion,
      triggerSource = saveWithPayload.triggerSource,
      emit = saveWithPayload.emit,
    )

    saveAndEmitForEnvelope(
      saveWithEnvelope,
      type,
    )
  }

  @Deprecated("Instead use [save]")
  @Transactional
  fun saveAndEmitForEnvelope(
    domainEvent: SaveCas1DomainEvent<*>,
    eventType: DomainEventType,
  ) {
    val domainEventEntity = domainEventRepository.save(
      DomainEventEntity(
        id = domainEvent.id,
        applicationId = domainEvent.applicationId,
        assessmentId = domainEvent.assessmentId,
        bookingId = domainEvent.bookingId,
        cas1SpaceBookingId = domainEvent.cas1SpaceBookingId,
        cas3PremisesId = null,
        cas3BedspaceId = null,
        crn = domainEvent.crn,
        type = eventType,
        occurredAt = domainEvent.occurredAt.atOffset(ZoneOffset.UTC),
        createdAt = OffsetDateTime.now(),
        cas3CancelledAt = null,
        data = objectMapper.writeValueAsString(domainEvent.data),
        service = "CAS1",
        triggerSource = domainEvent.triggerSource,
        triggeredByUserId = userService.getUserForRequestOrNull()?.id,
        nomsNumber = domainEvent.nomsNumber,
        metadata = domainEvent.metadata,
        schemaVersion = domainEvent.schemaVersion,
        cas3TransactionId = null,
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
          identifiers = listOfNotNull(
            crn?.let { SnsEventPersonReference("CRN", it) },
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
  val schemaVersion: Int?,
  val spaceBookingId: UUID?,
)

@Deprecated("Use [SaveCas1DomainEventWithPayload]")
data class SaveCas1DomainEvent<T>(
  val id: UUID,
  val applicationId: UUID? = null,
  val assessmentId: UUID? = null,
  val bookingId: UUID? = null,
  val cas1SpaceBookingId: UUID? = null,
  val crn: String,
  val nomsNumber: String?,
  val occurredAt: Instant,
  val data: T,
  val metadata: Map<MetaDataName, String?> = emptyMap(),
  val schemaVersion: Int? = null,
  val triggerSource: TriggerSourceType? = null,
  val emit: Boolean = true,
)

data class SaveCas1DomainEventWithPayload<T : Cas1DomainEventPayload>(
  val id: UUID = UUID.randomUUID(),
  val type: DomainEventType,
  val applicationId: UUID? = null,
  val assessmentId: UUID? = null,
  val bookingId: UUID? = null,
  val cas1SpaceBookingId: UUID? = null,
  val crn: String,
  val nomsNumber: String?,
  val occurredAt: Instant,
  val data: T,
  val metadata: Map<MetaDataName, String?> = emptyMap(),
  val schemaVersion: Int? = null,
  val triggerSource: TriggerSourceType? = null,
  val emit: Boolean = true,
)
