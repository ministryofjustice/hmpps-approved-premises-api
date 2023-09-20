package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2

import org.springframework.http.ResponseEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.EventsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Cas2ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import java.util.UUID

class Cas2DomainEventsController(private val domainEventService: DomainEventService) : EventsApiDelegate {
  override fun eventsCas2ApplicationSubmittedEventIdGet(eventId: UUID): ResponseEntity<Cas2ApplicationSubmittedEnvelope> {
    val event = domainEventService.getCas2ApplicationSubmittedDomainEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }
}
