package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingCancelledEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingCancelledUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingConfirmedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingProvisionallyMadeEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonArrivedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonArrivedUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonDepartedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonDepartureUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3ReferralSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import java.util.UUID

@RestController
@RequestMapping("\${api.base-path:}/events/cas3")
class Cas3DomainEventsController(
  private val cas3DomainEventService: Cas3DomainEventService,
) {

  @GetMapping("/booking-cancelled/{eventId}")
  fun eventsCas3BookingCancelledEventIdGet(@PathVariable eventId: UUID): ResponseEntity<CAS3BookingCancelledEvent> {
    val event = cas3DomainEventService.getBookingCancelledEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  @GetMapping("/booking-confirmed/{eventId}")
  fun eventsCas3BookingConfirmedEventIdGet(@PathVariable eventId: UUID): ResponseEntity<CAS3BookingConfirmedEvent> {
    val event = cas3DomainEventService.getBookingConfirmedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  @GetMapping("/booking-provisionally-made/{eventId}")
  fun eventsCas3BookingProvisionallyMadeEventIdGet(@PathVariable eventId: UUID): ResponseEntity<CAS3BookingProvisionallyMadeEvent> {
    val event = cas3DomainEventService.getBookingProvisionallyMadeEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  @GetMapping("/person-arrived/{eventId}")
  fun eventsCas3PersonArrivedEventIdGet(@PathVariable eventId: UUID): ResponseEntity<CAS3PersonArrivedEvent> {
    val event = cas3DomainEventService.getPersonArrivedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  @GetMapping("/person-arrived-updated/{eventId}")
  fun eventsCas3PersonArrivedUpdatedEventIdGet(@PathVariable eventId: UUID): ResponseEntity<CAS3PersonArrivedUpdatedEvent> {
    val event = cas3DomainEventService.getPersonArrivedUpdatedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  @GetMapping("/person-departed/{eventId}")
  fun eventsCas3PersonDepartedEventIdGet(@PathVariable eventId: UUID): ResponseEntity<CAS3PersonDepartedEvent> {
    val event = cas3DomainEventService.getPersonDepartedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  @GetMapping("referral-submitted/{eventId}")
  fun eventsCas3ReferralSubmittedEventIdGet(@PathVariable eventId: UUID): ResponseEntity<CAS3ReferralSubmittedEvent> {
    val event = cas3DomainEventService.getReferralSubmittedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  @GetMapping("/person-departure-updated/{eventId}")
  fun eventsCas3PersonDepartureUpdatedEventIdGet(@PathVariable eventId: UUID): ResponseEntity<CAS3PersonDepartureUpdatedEvent> {
    val event = cas3DomainEventService.getPersonDepartureUpdatedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  @GetMapping("/booking-cancelled-updated/{eventId}")
  fun eventsCas3BookingCancelledUpdatedEventIdGet(@PathVariable eventId: UUID): ResponseEntity<CAS3BookingCancelledUpdatedEvent> {
    val event = cas3DomainEventService.getBookingCancelledUpdatedEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }
}
