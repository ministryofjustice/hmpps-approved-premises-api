package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationTimelineNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineTransformer
import java.util.UUID

@Service
class Cas1TimelineService(
  private val applicationTimelineNoteService: ApplicationTimelineNoteService,
  private val applicationTimelineNoteTransformer: ApplicationTimelineNoteTransformer,
  private val domainEventService: Cas1DomainEventService,
  private val applicationTimelineTransformer: ApplicationTimelineTransformer,
) {
  fun getApplicationTimeline(applicationId: UUID): List<TimelineEvent> {
    val timelineEvents = mutableListOf<TimelineEvent>()
    timelineEvents += getDomainEventsForApplication(applicationId)
    timelineEvents += getApplicationNotesForApplication(applicationId)
    return timelineEvents
  }

  fun getSpaceBookingTimeline(bookingId: UUID): List<TimelineEvent> =
    toTimelineEvent(domainEventService.getAllDomainEventsForSpaceBooking(bookingId))

  private fun getDomainEventsForApplication(applicationId: UUID) =
    toTimelineEvent(domainEventService.getAllDomainEventsForApplication(applicationId))

  private fun getApplicationNotesForApplication(applicationId: UUID): List<TimelineEvent> {
    val noteEntities = applicationTimelineNoteService.getApplicationTimelineNotesByApplicationId(applicationId)
    return noteEntities.map {
      applicationTimelineNoteTransformer.transformToTimelineEvents(it)
    }
  }

  private fun toTimelineEvent(domainEvents: List<DomainEventSummary>) = domainEvents.map {
    applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(it)
  }
}
