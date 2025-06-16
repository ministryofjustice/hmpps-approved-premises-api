package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.CAS2EventsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import java.util.UUID

@Service(
  "uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2.DomainEventsController",
)
class DomainEventsController(private val domainEventService: Cas2DomainEventService) : CAS2EventsApiDelegate {

  override fun eventsCas2ApplicationSubmittedEventIdGet(eventId: UUID): ResponseEntity<Cas2ApplicationSubmittedEvent> {
    val event = domainEventService.getCas2ApplicationSubmittedDomainEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }

  override fun eventsCas2ApplicationStatusUpdatedEventIdGet(eventId: UUID): ResponseEntity<Cas2ApplicationStatusUpdatedEvent> {
    val event = domainEventService.getCas2ApplicationStatusUpdatedDomainEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }
}
