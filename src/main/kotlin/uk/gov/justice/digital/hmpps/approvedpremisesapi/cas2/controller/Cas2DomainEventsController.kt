package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import java.util.UUID
import org.springframework.web.bind.annotation.PathVariable

@RestController
@RequestMapping("\${openapi.cAS2DomainEvents.base-path:}/events/cas2/")
class Cas2DomainEventsController(private val domainEventService: Cas2DomainEventService) {

  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["application-submitted/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsCas2ApplicationSubmittedEventIdGet(@PathVariable eventId: UUID): ResponseEntity<Cas2ApplicationSubmittedEvent> {
    val event = domainEventService.getCas2ApplicationSubmittedDomainEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["application-status-updated/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsCas2ApplicationStatusUpdatedEventIdGet(@PathVariable eventId: UUID): ResponseEntity<Cas2ApplicationStatusUpdatedEvent> {
    val event = domainEventService.getCas2ApplicationStatusUpdatedDomainEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }
}
