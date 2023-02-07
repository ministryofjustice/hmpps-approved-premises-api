package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.EventsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import java.util.UUID

@Service
class DomainEventsController(
  private val domainEventService: DomainEventService
) : EventsApiDelegate {
  override fun eventsApplicationSubmittedEventIdGet(eventId: UUID): ResponseEntity<ApplicationSubmittedEnvelope> {
    val event = domainEventService.getApplicationSubmittedDomainEvent(eventId)
      ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data)
  }
}
