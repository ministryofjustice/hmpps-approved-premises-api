package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.FurtherInformationRequestedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementCreatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import java.util.UUID

@SuppressWarnings("TooManyFunctions")
@Controller
class DomainEventsController(
  private val domainEventService: Cas1DomainEventService,
) {
  fun eventsApplicationSubmittedEventIdGet(eventId: UUID) = getDomainEvent<ApplicationSubmittedEnvelope>(eventId)

  fun eventsBookingCancelledEventIdGet(eventId: UUID) = getDomainEvent<BookingCancelledEnvelope>(eventId)

  fun eventsBookingChangedEventIdGet(eventId: UUID) = getDomainEvent<BookingChangedEnvelope>(eventId)

  fun eventsBookingKeyworkerAssignedEventIdGet(eventId: UUID) = getDomainEvent<BookingKeyWorkerAssignedEnvelope>(eventId)

  fun eventsApplicationAssessedEventIdGet(eventId: UUID) = getDomainEvent<ApplicationAssessedEnvelope>(eventId)

  fun eventsBookingMadeEventIdGet(eventId: UUID) = getDomainEvent<BookingMadeEnvelope>(eventId)

  fun eventsPersonArrivedEventIdGet(eventId: UUID) = getDomainEvent<PersonArrivedEnvelope>(eventId)

  fun eventsPersonNotArrivedEventIdGet(eventId: UUID) = getDomainEvent<PersonNotArrivedEnvelope>(eventId)

  fun eventsPersonDepartedEventIdGet(eventId: UUID) = getDomainEvent<PersonDepartedEnvelope>(eventId)

  fun eventsBookingNotMadeEventIdGet(eventId: UUID) = getDomainEvent<BookingNotMadeEnvelope>(eventId)

  fun eventsApplicationWithdrawnEventIdGet(eventId: UUID) = getDomainEvent<ApplicationWithdrawnEnvelope>(eventId)

  fun eventsApplicationExpiredEventIdGet(eventId: UUID) = getDomainEvent<ApplicationExpiredEnvelope>(eventId)

  fun eventsPlacementApplicationWithdrawnEventIdGet(eventId: UUID) = getDomainEvent<PlacementApplicationWithdrawnEnvelope>(eventId)

  fun eventsPlacementApplicationAllocatedEventIdGet(eventId: UUID) = getDomainEvent<PlacementApplicationAllocatedEnvelope>(eventId)

  fun eventsMatchRequestWithdrawnEventIdGet(eventId: UUID) = getDomainEvent<MatchRequestWithdrawnEnvelope>(eventId)

  fun eventsAssessmentAppealedEventIdGet(eventId: UUID) = getDomainEvent<AssessmentAppealedEnvelope>(eventId)

  fun eventsAssessmentAllocatedEventIdGet(eventId: UUID) = getDomainEvent<AssessmentAllocatedEnvelope>(eventId)

  fun eventsFurtherInformationRequestedEventIdGet(eventId: UUID) = getDomainEvent<FurtherInformationRequestedEnvelope>(eventId)

  fun eventsRequestForPlacementCreatedEventIdGet(eventId: UUID) = getDomainEvent<RequestForPlacementCreatedEnvelope>(eventId)

  fun eventsRequestForPlacementAssessedEventIdGet(eventId: UUID) = getDomainEvent<RequestForPlacementAssessedEnvelope>(eventId)

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
