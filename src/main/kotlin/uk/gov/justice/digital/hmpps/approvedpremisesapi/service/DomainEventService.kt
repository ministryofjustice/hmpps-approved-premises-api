package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.FurtherInformationRequestedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementCreatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.DomainEventUrlConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventAdditionalInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReferenceCollection
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.transaction.Transactional

@Service
class DomainEventService(
  private val objectMapper: ObjectMapper,
  private val domainEventRepository: DomainEventRepository,
  val domainEventWorker: ConfiguredDomainEventWorker,
  private val userService: UserService,
  @Value("\${domain-events.cas1.emit-enabled}") private val emitDomainEventsEnabled: Boolean,
  private val domainEventUrlConfig: DomainEventUrlConfig,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getApplicationSubmittedDomainEvent(id: UUID) = get<ApplicationSubmittedEnvelope>(id)
  fun getApplicationAssessedDomainEvent(id: UUID) = get<ApplicationAssessedEnvelope>(id)
  fun getBookingMadeEvent(id: UUID) = get<BookingMadeEnvelope>(id)
  fun getPersonArrivedEvent(id: UUID) = get<PersonArrivedEnvelope>(id)
  fun getPersonNotArrivedEvent(id: UUID) = get<PersonNotArrivedEnvelope>(id)
  fun getPersonDepartedEvent(id: UUID) = get<PersonDepartedEnvelope>(id)
  fun getBookingNotMadeEvent(id: UUID) = get<BookingNotMadeEnvelope>(id)
  fun getBookingCancelledEvent(id: UUID) = get<BookingCancelledEnvelope>(id)
  fun getBookingChangedEvent(id: UUID) = get<BookingChangedEnvelope>(id)
  fun getApplicationWithdrawnEvent(id: UUID) = get<ApplicationWithdrawnEnvelope>(id)
  fun getPlacementApplicationWithdrawnEvent(id: UUID) = get<PlacementApplicationWithdrawnEnvelope>(id)
  fun getPlacementApplicationAllocatedEvent(id: UUID) = get<PlacementApplicationAllocatedEnvelope>(id)
  fun getMatchRequestWithdrawnEvent(id: UUID) = get<MatchRequestWithdrawnEnvelope>(id)
  fun getAssessmentAppealedEvent(id: UUID) = get<AssessmentAppealedEnvelope>(id)
  fun getAssessmentAllocatedEvent(id: UUID) = get<AssessmentAllocatedEnvelope>(id)
  fun getRequestForPlacementCreatedEvent(id: UUID) = get<RequestForPlacementCreatedEnvelope>(id)
  fun getFurtherInformationRequestMadeEvent(id: UUID) = get<FurtherInformationRequestedEnvelope>(id)

  private inline fun <reified T> get(id: UUID): DomainEvent<T>? {
    val domainEventEntity = domainEventRepository.findByIdOrNull(id) ?: return null

    return domainEventEntity.toDomainEvent(objectMapper)
  }

  @Transactional
  fun saveApplicationSubmittedDomainEvent(domainEvent: DomainEvent<ApplicationSubmittedEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )

  @Transactional
  fun saveApplicationAssessedDomainEvent(domainEvent: DomainEvent<ApplicationAssessedEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )

  @Transactional
  fun saveBookingMadeDomainEvent(domainEvent: DomainEvent<BookingMadeEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_BOOKING_MADE,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      bookingId = domainEvent.bookingId,
    )

  @Transactional
  fun savePersonArrivedEvent(domainEvent: DomainEvent<PersonArrivedEnvelope>, emit: Boolean) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      bookingId = domainEvent.bookingId,
      emit = emit,
    )

  @Transactional
  fun savePersonNotArrivedEvent(domainEvent: DomainEvent<PersonNotArrivedEnvelope>, emit: Boolean) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      bookingId = domainEvent.bookingId,
      emit = emit,
    )

  @Transactional
  fun savePersonDepartedEvent(domainEvent: DomainEvent<PersonDepartedEnvelope>, emit: Boolean) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      bookingId = domainEvent.bookingId,
      emit = emit,
    )

  @Transactional
  fun saveBookingNotMadeEvent(domainEvent: DomainEvent<BookingNotMadeEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )

  @Transactional
  fun saveBookingCancelledEvent(domainEvent: DomainEvent<BookingCancelledEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      bookingId = domainEvent.bookingId,
    )

  @Transactional
  fun saveBookingChangedEvent(domainEvent: DomainEvent<BookingChangedEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      bookingId = domainEvent.bookingId,
    )

  @Transactional
  fun saveApplicationWithdrawnEvent(domainEvent: DomainEvent<ApplicationWithdrawnEnvelope>, emit: Boolean) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      emit = emit,
    )

  @Transactional
  fun saveAssessmentAppealedEvent(domainEvent: DomainEvent<AssessmentAppealedEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )

  @Transactional
  fun savePlacementApplicationWithdrawnEvent(domainEvent: DomainEvent<PlacementApplicationWithdrawnEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )

  @Transactional
  fun savePlacementApplicationAllocatedEvent(domainEvent: DomainEvent<PlacementApplicationAllocatedEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )

  @Transactional
  fun saveMatchRequestWithdrawnEvent(domainEvent: DomainEvent<MatchRequestWithdrawnEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )

  @Transactional
  fun saveRequestForPlacementCreatedEvent(domainEvent: DomainEvent<RequestForPlacementCreatedEnvelope>, emit: Boolean) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      emit = emit,
    )

  @Transactional
  fun saveAssessmentAllocatedEvent(domainEvent: DomainEvent<AssessmentAllocatedEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )

  @Transactional
  fun saveFurtherInformationRequestedEvent(domainEvent: DomainEvent<FurtherInformationRequestedEnvelope>, emit: Boolean = true) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED,
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      emit = emit,
    )

  fun getAllDomainEventsForApplication(applicationId: UUID) =
    domainEventRepository.findAllTimelineEventsByApplicationId(applicationId).distinctBy { it.id }

  @Transactional
  fun saveAndEmit(
    domainEvent: DomainEvent<*>,
    eventType: DomainEventType,
    crn: String,
    nomsNumber: String,
    bookingId: UUID? = null,
    emit: Boolean = true,
  ) {
    val detailUrl = domainEventUrlConfig.getUrlForDomainEventId(eventType, domainEvent.id)
    val typeName = eventType.typeName
    val typeDescription = eventType.typeDescription

    domainEventRepository.save(
      DomainEventEntity(
        id = domainEvent.id,
        applicationId = domainEvent.applicationId,
        assessmentId = domainEvent.assessmentId,
        bookingId = bookingId,
        crn = domainEvent.crn,
        type = eventType,
        occurredAt = domainEvent.occurredAt.atOffset(ZoneOffset.UTC),
        createdAt = OffsetDateTime.now(),
        data = objectMapper.writeValueAsString(domainEvent.data),
        service = "CAS1",
        triggeredByUserId = userService.getUserForRequestOrNull()?.id,
      ),
    )

    if (!emit) {
      return
    }

    if (!emitDomainEventsEnabled) {
      log.info("Not emitting SNS event for domain event because domain-events.cas1.emit-enabled is not enabled")
      return
    }

    domainEventWorker.emitEvent(
      SnsEvent(
        eventType = typeName,
        version = 1,
        description = typeDescription,
        detailUrl = detailUrl,
        occurredAt = domainEvent.occurredAt.atOffset(ZoneOffset.UTC),
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
