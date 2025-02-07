package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingCancelledEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingCancelledUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingConfirmedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingProvisionallyMadeEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonArrivedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonArrivedUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonDepartedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonDepartureUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3ReferralSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.CAS3EventsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventService
import java.util.UUID

@Service(
  "uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3.DomainEventsController",
)
class DomainEventsController(
  private val domainEventService: DomainEventService,
) : CAS3EventsApiDelegate {
  override fun eventsCas3BookingCancelledEventIdGet(eventId: UUID): ResponseEntity<CAS3BookingCancelledEvent> {
    val event = domainEventService.getBookingCancelledEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsCas3BookingConfirmedEventIdGet(eventId: UUID): ResponseEntity<CAS3BookingConfirmedEvent> {
    val event = domainEventService.getBookingConfirmedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsCas3BookingProvisionallyMadeEventIdGet(eventId: UUID): ResponseEntity<CAS3BookingProvisionallyMadeEvent> {
    val event = domainEventService.getBookingProvisionallyMadeEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsCas3PersonArrivedEventIdGet(eventId: UUID): ResponseEntity<CAS3PersonArrivedEvent> {
    val event = domainEventService.getPersonArrivedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsCas3PersonArrivedUpdatedEventIdGet(eventId: UUID): ResponseEntity<CAS3PersonArrivedUpdatedEvent> {
    val event = domainEventService.getPersonArrivedUpdatedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsCas3PersonDepartedEventIdGet(eventId: UUID): ResponseEntity<CAS3PersonDepartedEvent> {
    val event = domainEventService.getPersonDepartedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsCas3ReferralSubmittedEventIdGet(eventId: UUID): ResponseEntity<CAS3ReferralSubmittedEvent> {
    val event = domainEventService.getReferralSubmittedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsCas3PersonDepartureUpdatedEventIdGet(eventId: UUID): ResponseEntity<CAS3PersonDepartureUpdatedEvent> {
    val event = domainEventService.getPersonDepartureUpdatedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsCas3BookingCancelledUpdatedEventIdGet(eventId: UUID): ResponseEntity<CAS3BookingCancelledUpdatedEvent> {
    val event = domainEventService.getBookingCancelledUpdatedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }
}
