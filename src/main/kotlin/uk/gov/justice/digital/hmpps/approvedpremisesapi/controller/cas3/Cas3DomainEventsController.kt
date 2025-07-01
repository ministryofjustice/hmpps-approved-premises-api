package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingCancelledEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingCancelledUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingConfirmedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingProvisionallyMadeEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonArrivedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonArrivedUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonDepartedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonDepartureUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3ReferralSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3DomainEventService
import java.util.UUID

@RestController
class Cas3DomainEventsController(
  private val cas3DomainEventService: Cas3DomainEventService,
) {

  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/cas3/booking-cancelled/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsCas3BookingCancelledEventIdGet(eventId: UUID): ResponseEntity<CAS3BookingCancelledEvent> {
    val event = cas3DomainEventService.getBookingCancelledEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/cas3/booking-confirmed/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsCas3BookingConfirmedEventIdGet(eventId: UUID): ResponseEntity<CAS3BookingConfirmedEvent> {
    val event = cas3DomainEventService.getBookingConfirmedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/cas3/booking-provisionally-made/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsCas3BookingProvisionallyMadeEventIdGet(eventId: UUID): ResponseEntity<CAS3BookingProvisionallyMadeEvent> {
    val event = cas3DomainEventService.getBookingProvisionallyMadeEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/cas3/person-arrived/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsCas3PersonArrivedEventIdGet(eventId: UUID): ResponseEntity<CAS3PersonArrivedEvent> {
    val event = cas3DomainEventService.getPersonArrivedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/cas3/person-arrived-updated/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsCas3PersonArrivedUpdatedEventIdGet(eventId: UUID): ResponseEntity<CAS3PersonArrivedUpdatedEvent> {
    val event = cas3DomainEventService.getPersonArrivedUpdatedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/cas3/person-departed/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsCas3PersonDepartedEventIdGet(eventId: UUID): ResponseEntity<CAS3PersonDepartedEvent> {
    val event = cas3DomainEventService.getPersonDepartedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/cas3/referral-submitted/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsCas3ReferralSubmittedEventIdGet(eventId: UUID): ResponseEntity<CAS3ReferralSubmittedEvent> {
    val event = cas3DomainEventService.getReferralSubmittedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/cas3/person-departure-updated/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsCas3PersonDepartureUpdatedEventIdGet(eventId: UUID): ResponseEntity<CAS3PersonDepartureUpdatedEvent> {
    val event = cas3DomainEventService.getPersonDepartureUpdatedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/cas3/booking-cancelled-updated/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsCas3BookingCancelledUpdatedEventIdGet(eventId: UUID): ResponseEntity<CAS3BookingCancelledUpdatedEvent> {
    val event = cas3DomainEventService.getBookingCancelledUpdatedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }
}
