package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.EventsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationExpiredEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingKeyWorkerAssignedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.FurtherInformationRequestedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementCreatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import java.util.UUID

@SuppressWarnings("TooManyFunctions")
@Service
class DomainEventsController(
  private val domainEventService: DomainEventService,
) : EventsApiDelegate {
  override fun eventsApplicationSubmittedEventIdGet(eventId: UUID) = getDomainEvent<ApplicationSubmittedEnvelope>(eventId)

  override fun eventsBookingCancelledEventIdGet(eventId: UUID) = getDomainEvent<BookingCancelledEnvelope>(eventId)

  override fun eventsBookingChangedEventIdGet(eventId: UUID) = getDomainEvent<BookingChangedEnvelope>(eventId)

  override fun eventsBookingKeyworkerAssignedEventIdGet(eventId: UUID) = getDomainEvent<BookingKeyWorkerAssignedEnvelope>(eventId)

  override fun eventsApplicationAssessedEventIdGet(eventId: UUID) = getDomainEvent<ApplicationAssessedEnvelope>(eventId)

  override fun eventsBookingMadeEventIdGet(eventId: UUID) = getDomainEvent<BookingMadeEnvelope>(eventId)

  override fun eventsPersonArrivedEventIdGet(eventId: UUID) = getDomainEvent<PersonArrivedEnvelope>(eventId)

  override fun eventsPersonNotArrivedEventIdGet(eventId: UUID) = getDomainEvent<PersonNotArrivedEnvelope>(eventId)

  override fun eventsPersonDepartedEventIdGet(eventId: UUID) = getDomainEvent<PersonDepartedEnvelope>(eventId)

  override fun eventsBookingNotMadeEventIdGet(eventId: UUID) = getDomainEvent<BookingNotMadeEnvelope>(eventId)

  override fun eventsApplicationWithdrawnEventIdGet(eventId: UUID) = getDomainEvent<ApplicationWithdrawnEnvelope>(eventId)

  override fun eventsApplicationExpiredEventIdGet(eventId: UUID) = getDomainEvent<ApplicationExpiredEnvelope>(eventId)

  override fun eventsPlacementApplicationWithdrawnEventIdGet(eventId: UUID) = getDomainEvent<PlacementApplicationWithdrawnEnvelope>(eventId)

  override fun eventsPlacementApplicationAllocatedEventIdGet(eventId: UUID) = getDomainEvent<PlacementApplicationAllocatedEnvelope>(eventId)

  override fun eventsMatchRequestWithdrawnEventIdGet(eventId: UUID) = getDomainEvent<MatchRequestWithdrawnEnvelope>(eventId)

  override fun eventsAssessmentAppealedEventIdGet(eventId: UUID) = getDomainEvent<AssessmentAppealedEnvelope>(eventId)

  override fun eventsAssessmentAllocatedEventIdGet(eventId: UUID) = getDomainEvent<AssessmentAllocatedEnvelope>(eventId)

  override fun eventsFurtherInformationRequestedEventIdGet(eventId: UUID) = getDomainEvent<FurtherInformationRequestedEnvelope>(eventId)

  override fun eventsRequestForPlacementCreatedEventIdGet(eventId: UUID) = getDomainEvent<RequestForPlacementCreatedEnvelope>(eventId)

  override fun eventsRequestForPlacementAssessedEventIdGet(eventId: UUID) = getDomainEvent<RequestForPlacementAssessedEnvelope>(eventId)

  @Suppress("UNCHECKED_CAST") // Safe as the return type is constant and not likely to change at runtime
  private inline fun <reified T> getDomainEvent(eventId: UUID): ResponseEntity<T> {
    val serviceMethod = when (T::class) {
      AssessmentAllocatedEnvelope::class -> domainEventService::getAssessmentAllocatedEvent
      AssessmentAppealedEnvelope::class -> domainEventService::getAssessmentAppealedEvent
      MatchRequestWithdrawnEnvelope::class -> domainEventService::getMatchRequestWithdrawnEvent
      ApplicationWithdrawnEnvelope::class -> domainEventService::getApplicationWithdrawnEvent
      ApplicationExpiredEnvelope::class -> domainEventService::getApplicationExpiredEvent
      ApplicationAssessedEnvelope::class -> domainEventService::getApplicationAssessedDomainEvent
      BookingMadeEnvelope::class -> domainEventService::getBookingMadeEvent
      ApplicationSubmittedEnvelope::class -> domainEventService::getApplicationSubmittedDomainEvent
      PlacementApplicationWithdrawnEnvelope::class -> domainEventService::getPlacementApplicationWithdrawnEvent
      BookingCancelledEnvelope::class -> domainEventService::getBookingCancelledEvent
      BookingChangedEnvelope::class -> domainEventService::getBookingChangedEvent
      BookingKeyWorkerAssignedEnvelope::class -> domainEventService::getBookingKeyWorkerAssignedEvent
      BookingNotMadeEnvelope::class -> domainEventService::getBookingNotMadeEvent
      PersonArrivedEnvelope::class -> domainEventService::getPersonArrivedEvent
      PersonDepartedEnvelope::class -> domainEventService::getPersonDepartedEvent
      PlacementApplicationAllocatedEnvelope::class -> domainEventService::getPlacementApplicationAllocatedEvent
      PersonNotArrivedEnvelope::class -> domainEventService::getPersonNotArrivedEvent
      FurtherInformationRequestedEnvelope::class -> domainEventService::getFurtherInformationRequestMadeEvent
      RequestForPlacementCreatedEnvelope::class -> domainEventService::getRequestForPlacementCreatedEvent
      RequestForPlacementAssessedEnvelope::class -> domainEventService::getRequestForPlacementAssessedEvent
      else -> throw RuntimeException("Only CAS1 events are supported")
    }

    val event = serviceMethod(eventId) ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data) as ResponseEntity<T>
  }
}
