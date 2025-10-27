package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.events.Cas2ApplicationStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.events.Cas2ApplicationSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import java.util.UUID

@RestController
@RequestMapping("\${openapi.cAS2DomainEvents.base-path:}/events/cas2/")
class Cas2DomainEventsController(private val domainEventService: Cas2DomainEventService) {

  @GetMapping("application-submitted/{eventId}")
  fun eventsCas2ApplicationSubmittedEventIdGet(@PathVariable eventId: UUID): ResponseEntity<Cas2ApplicationSubmittedEvent> {
    val event = domainEventService.getCas2ApplicationSubmittedDomainEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  @GetMapping("application-status-updated/{eventId}")
  fun eventsCas2ApplicationStatusUpdatedEventIdGet(@PathVariable eventId: UUID): ResponseEntity<Cas2ApplicationStatusUpdatedEvent> {
    val event = domainEventService.getCas2ApplicationStatusUpdatedDomainEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }
}
