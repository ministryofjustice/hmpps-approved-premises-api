package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationTimelineNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ApplicationTimelineTransformer
import java.util.UUID
import kotlin.collections.plusAssign

@Service
class Cas1TimelineService(
  private val cas1ApplicationTimelineNoteService: Cas1ApplicationTimelineNoteService,
  private val applicationTimelineNoteTransformer: ApplicationTimelineNoteTransformer,
  private val domainEventService: Cas1DomainEventService,
  private val cas1applicationTimelineTransformer: Cas1ApplicationTimelineTransformer,
) {
  fun getSpaceBookingTimeline(bookingId: UUID): List<Cas1TimelineEvent> = toTimelineEvents(domainEventService.getAllDomainEventsForSpaceBooking(bookingId))

  fun getApplicationTimelineEvents(applicationId: UUID): List<Cas1TimelineEvent> {
    val cas1TimelineEvents = mutableListOf<Cas1TimelineEvent>()
    cas1TimelineEvents += getTimelineEvents(applicationId)
    cas1TimelineEvents += getNotesTimelineEvents(applicationId)
    return cas1TimelineEvents
  }

  private fun getTimelineEvents(applicationId: UUID) = toTimelineEvents(domainEventService.getAllDomainEventsForApplication(applicationId))

  private fun getNotesTimelineEvents(applicationId: UUID): List<Cas1TimelineEvent> {
    val noteEntities = cas1ApplicationTimelineNoteService.getApplicationTimelineNotesByApplicationId(applicationId)
    return noteEntities.map {
      applicationTimelineNoteTransformer.transformToCas1TimelineEvents(it)
    }
  }

  private fun toTimelineEvents(domainEvents: List<DomainEventSummary>) = domainEvents.map {
    cas1applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(it)
  }
}
