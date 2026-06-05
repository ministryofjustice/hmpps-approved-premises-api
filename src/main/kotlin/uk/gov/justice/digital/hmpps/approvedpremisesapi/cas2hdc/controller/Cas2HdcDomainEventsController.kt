package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import java.util.UUID

@RestController
@RequestMapping("\${openapi.cAS2DomainEvents.base-path:}/events/cas2/")
class Cas2HdcDomainEventsController(private val domainEventService: Cas2HdcDomainEventService) {

  @GetMapping("application-submitted/{eventId}")
  fun eventsCas2HdcApplicationSubmittedEventIdGet(@PathVariable eventId: UUID): ResponseEntity<Cas2ApplicationSubmittedEvent> {
    val event = domainEventService.getCas2HdcApplicationSubmittedDomainEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  @GetMapping("application-status-updated/{eventId}")
  fun eventsCas2HdcApplicationStatusUpdatedEventIdGet(@PathVariable eventId: UUID): ResponseEntity<Cas2ApplicationStatusUpdatedEvent> {
    val event = domainEventService.getCas2HdcApplicationStatusUpdatedDomainEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }
}
