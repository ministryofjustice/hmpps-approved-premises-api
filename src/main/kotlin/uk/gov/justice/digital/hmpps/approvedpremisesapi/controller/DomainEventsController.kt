package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.EventsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAllocatedEnvelope
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import java.util.UUID

@Service
class DomainEventsController(
  private val domainEventService: DomainEventService,
) : EventsApiDelegate {
  override fun eventsApplicationSubmittedEventIdGet(eventId: UUID): ResponseEntity<ApplicationSubmittedEnvelope> {
    val event = domainEventService.getApplicationSubmittedDomainEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsBookingCancelledEventIdGet(eventId: UUID): ResponseEntity<BookingCancelledEnvelope> {
    val event = domainEventService.getBookingCancelledEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsBookingChangedEventIdGet(eventId: UUID): ResponseEntity<BookingChangedEnvelope> {
    val event = domainEventService.getBookingChangedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsApplicationAssessedEventIdGet(eventId: UUID): ResponseEntity<ApplicationAssessedEnvelope> {
    val event = domainEventService.getApplicationAssessedDomainEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsBookingMadeEventIdGet(eventId: UUID): ResponseEntity<BookingMadeEnvelope> {
    val event = domainEventService.getBookingMadeEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsPersonArrivedEventIdGet(eventId: UUID): ResponseEntity<PersonArrivedEnvelope> {
    val event = domainEventService.getPersonArrivedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsPersonNotArrivedEventIdGet(eventId: UUID): ResponseEntity<PersonNotArrivedEnvelope> {
    val event = domainEventService.getPersonNotArrivedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsPersonDepartedEventIdGet(eventId: UUID): ResponseEntity<PersonDepartedEnvelope> {
    val event = domainEventService.getPersonDepartedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsBookingNotMadeEventIdGet(eventId: UUID): ResponseEntity<BookingNotMadeEnvelope> {
    val event = domainEventService.getBookingNotMadeEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsApplicationWithdrawnEventIdGet(eventId: UUID): ResponseEntity<ApplicationWithdrawnEnvelope> {
    val event = domainEventService.getApplicationWithdrawnEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsPlacementApplicationWithdrawnEventIdGet(eventId: UUID): ResponseEntity<PlacementApplicationWithdrawnEnvelope> {
    val event = domainEventService.getPlacementApplicationWithdrawnEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsMatchRequestWithdrawnEventIdGet(eventId: UUID): ResponseEntity<MatchRequestWithdrawnEnvelope> {
    val event = domainEventService.getMatchRequestWithdrawnEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsAssessmentAppealedEventIdGet(eventId: UUID): ResponseEntity<AssessmentAppealedEnvelope> {
    val event = domainEventService.getAssessmentAppealedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsAssessmentAllocatedEventIdGet(eventId: UUID): ResponseEntity<AssessmentAllocatedEnvelope> {
    val event = domainEventService.getAssessmentAllocatedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }
}
