package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventAdditionalInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReferenceCollection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
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
  @Value("\${url-templates.api.cas1.application-submitted-event-detail}") private val applicationSubmittedDetailUrlTemplate: String,
  @Value("\${url-templates.api.cas1.application-assessed-event-detail}") private val applicationAssessedDetailUrlTemplate: String,
  @Value("\${url-templates.api.cas1.booking-made-event-detail}") private val bookingMadeDetailUrlTemplate: String,
  @Value("\${url-templates.api.cas1.person-arrived-event-detail}") private val personArrivedDetailUrlTemplate: String,
  @Value("\${url-templates.api.cas1.person-not-arrived-event-detail}") private val personNotArrivedDetailUrlTemplate: String,
  @Value("\${url-templates.api.cas1.person-departed-event-detail}") private val personDepartedDetailUrlTemplate: String,
  @Value("\${url-templates.api.cas1.booking-not-made-event-detail}") private val bookingNotMadeDetailUrlTemplate: String,
  @Value("\${url-templates.api.cas1.booking-cancelled-event-detail}") private val bookingCancelledDetailUrlTemplate: String,
  @Value("\${url-templates.api.cas1.booking-changed-event-detail}") private val bookingChangedDetailUrlTemplate: String,
  @Value("\${url-templates.api.cas1.application-withdrawn-event-detail}") private val applicationWithdrawnDetailUrlTemplate: String,
  @Value("\${url-templates.api.cas1.assessment-appealed-event-detail}") private val assessmentAppealedDetailUrlTemplate: String,
  @Value("\${url-templates.api.cas1.placement-application-withdrawn-event-detail}") private val placementApplicationWithdrawnDetailUrlTemplate: UrlTemplate,
  @Value("\${url-templates.api.cas1.match-request-withdrawn-event-detail}") private val matchRequestWithdrawnDetailUrlTemplate: UrlTemplate,
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
  fun getMatchRequestWithdrawnEvent(id: UUID) = get<MatchRequestWithdrawnEnvelope>(id)
  fun getAssessmentAppealedEvent(id: UUID) = get<AssessmentAppealedEnvelope>(id)

  private inline fun <reified T> get(id: UUID): DomainEvent<T>? {
    val domainEventEntity = domainEventRepository.findByIdOrNull(id) ?: return null

    return domainEventEntity.toDomainEvent(objectMapper)
  }

  @Transactional
  fun saveApplicationSubmittedDomainEvent(domainEvent: DomainEvent<ApplicationSubmittedEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      typeName = "approved-premises.application.submitted",
      typeDescription = "An application has been submitted for an Approved Premises placement",
      detailUrl = applicationSubmittedDetailUrlTemplate.replace("#eventId", domainEvent.id.toString()),
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )

  @Transactional
  fun saveApplicationAssessedDomainEvent(domainEvent: DomainEvent<ApplicationAssessedEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      typeName = "approved-premises.application.assessed",
      typeDescription = "An application has been assessed for an Approved Premises placement",
      detailUrl = applicationAssessedDetailUrlTemplate.replace("#eventId", domainEvent.id.toString()),
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )

  @Transactional
  fun saveBookingMadeDomainEvent(domainEvent: DomainEvent<BookingMadeEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      typeName = "approved-premises.booking.made",
      typeDescription = "An Approved Premises booking has been made",
      detailUrl = bookingMadeDetailUrlTemplate.replace("#eventId", domainEvent.id.toString()),
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      bookingId = domainEvent.bookingId,
    )

  @Transactional
  fun savePersonArrivedEvent(domainEvent: DomainEvent<PersonArrivedEnvelope>, emit: Boolean) =
    saveAndEmit(
      domainEvent = domainEvent,
      typeName = "approved-premises.person.arrived",
      typeDescription = "Someone has arrived at an Approved Premises for their Booking",
      detailUrl = personArrivedDetailUrlTemplate.replace("#eventId", domainEvent.id.toString()),
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      bookingId = domainEvent.bookingId,
      emit = emit,
    )

  @Transactional
  fun savePersonNotArrivedEvent(domainEvent: DomainEvent<PersonNotArrivedEnvelope>, emit: Boolean) =
    saveAndEmit(
      domainEvent = domainEvent,
      typeName = "approved-premises.person.not-arrived",
      typeDescription = "Someone has failed to arrive at an Approved Premises for their Booking",
      detailUrl = personNotArrivedDetailUrlTemplate.replace("#eventId", domainEvent.id.toString()),
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      bookingId = domainEvent.bookingId,
      emit = emit,
    )

  @Transactional
  fun savePersonDepartedEvent(domainEvent: DomainEvent<PersonDepartedEnvelope>, emit: Boolean) =
    saveAndEmit(
      domainEvent = domainEvent,
      typeName = "approved-premises.person.departed",
      typeDescription = "Someone has left an Approved Premises",
      detailUrl = personDepartedDetailUrlTemplate.replace("#eventId", domainEvent.id.toString()),
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      bookingId = domainEvent.bookingId,
      emit = emit,
    )

  @Transactional
  fun saveBookingNotMadeEvent(domainEvent: DomainEvent<BookingNotMadeEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      typeName = "approved-premises.booking.not-made",
      typeDescription = "It was not possible to create a Booking on this attempt",
      detailUrl = bookingNotMadeDetailUrlTemplate.replace("#eventId", domainEvent.id.toString()),
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )

  @Transactional
  fun saveBookingCancelledEvent(domainEvent: DomainEvent<BookingCancelledEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      typeName = "approved-premises.booking.cancelled",
      typeDescription = "An Approved Premises Booking has been cancelled",
      detailUrl = bookingCancelledDetailUrlTemplate.replace("#eventId", domainEvent.id.toString()),
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      bookingId = domainEvent.bookingId,
    )

  @Transactional
  fun saveBookingChangedEvent(domainEvent: DomainEvent<BookingChangedEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      typeName = "approved-premises.booking.changed",
      typeDescription = "An Approved Premises Booking has been changed",
      detailUrl = bookingChangedDetailUrlTemplate.replace("#eventId", domainEvent.id.toString()),
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
      bookingId = domainEvent.bookingId,
    )

  @Transactional
  fun saveApplicationWithdrawnEvent(domainEvent: DomainEvent<ApplicationWithdrawnEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      typeName = "approved-premises.application.withdrawn",
      typeDescription = "An Approved Premises Application has been withdrawn",
      detailUrl = applicationWithdrawnDetailUrlTemplate.replace("#eventId", domainEvent.id.toString()),
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )

  @Transactional
  fun saveAssessmentAppealedEvent(domainEvent: DomainEvent<AssessmentAppealedEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      typeName = "approved-premises.assessment.appealed",
      typeDescription = "An Approved Premises Assessment has been appealed",
      detailUrl = assessmentAppealedDetailUrlTemplate.replace("#eventId", domainEvent.id.toString()),
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )

  @Transactional
  fun savePlacementApplicationWithdrawnEvent(domainEvent: DomainEvent<PlacementApplicationWithdrawnEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      typeName = "approved-premises.placement-application.withdrawn",
      typeDescription = "An Approved Premises Request for Placement has been withdrawn",
      detailUrl = placementApplicationWithdrawnDetailUrlTemplate.resolve("eventId", domainEvent.id.toString()),
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )

  @Transactional
  fun saveMatchRequestWithdrawnEvent(domainEvent: DomainEvent<MatchRequestWithdrawnEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      typeName = "approved-premises.match-request.withdrawn",
      typeDescription = "An Approved Premises Match Request has been withdrawn",
      detailUrl = matchRequestWithdrawnDetailUrlTemplate.resolve("eventId", domainEvent.id.toString()),
      crn = domainEvent.data.eventDetails.personReference.crn,
      nomsNumber = domainEvent.data.eventDetails.personReference.noms,
    )

  fun getAllDomainEventsForApplication(applicationId: UUID) =
    domainEventRepository.findAllTimelineEventsByApplicationId(applicationId).distinctBy { it.id }

  private fun saveAndEmit(
    domainEvent: DomainEvent<*>,
    typeName: String,
    typeDescription: String,
    detailUrl: String,
    crn: String,
    nomsNumber: String,
    bookingId: UUID? = null,
    emit: Boolean = true,
  ) {
    domainEventRepository.save(
      DomainEventEntity(
        id = domainEvent.id,
        applicationId = domainEvent.applicationId,
        assessmentId = domainEvent.assessmentId,
        bookingId = bookingId,
        crn = domainEvent.crn,
        type = enumTypeFromDataType(domainEvent.data!!::class.java),
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

  @SuppressWarnings("CyclomaticComplexMethod")
  private fun <T> enumTypeFromDataType(type: Class<T>) = when (type) {
    ApplicationSubmittedEnvelope::class.java -> DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED
    ApplicationAssessedEnvelope::class.java -> DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED
    BookingMadeEnvelope::class.java -> DomainEventType.APPROVED_PREMISES_BOOKING_MADE
    PersonArrivedEnvelope::class.java -> DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED
    PersonNotArrivedEnvelope::class.java -> DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED
    PersonDepartedEnvelope::class.java -> DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED
    BookingNotMadeEnvelope::class.java -> DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE
    BookingCancelledEnvelope::class.java -> DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED
    BookingChangedEnvelope::class.java -> DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED
    ApplicationWithdrawnEnvelope::class.java -> DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN
    AssessmentAppealedEnvelope::class.java -> DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED
    PlacementApplicationWithdrawnEnvelope::class.java -> DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN
    MatchRequestWithdrawnEnvelope::class.java -> DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN
    else -> throw RuntimeException("Unrecognised domain event type: ${type.name}")
  }
}
