package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcTimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity

@Component
class Cas2v2TimelineEventsTransformer {

  fun transformApplicationToTimelineEvents(jpa: Cas2ApplicationEntity): List<Cas2HdcTimelineEvent> {
    val timelineEvents: MutableList<Cas2HdcTimelineEvent> = mutableListOf()

    addSubmittedEvent(jpa, timelineEvents)

    addStatusUpdateEvents(jpa, timelineEvents)

    addNoteEvents(jpa, timelineEvents)

    return timelineEvents.sortedByDescending { it.occurredAt }
  }

  private fun addNoteEvents(jpa: Cas2ApplicationEntity, timelineEvents: MutableList<Cas2HdcTimelineEvent>) {
    jpa.notes?.forEach {
      timelineEvents += Cas2HdcTimelineEvent(
        type = TimelineEventType.cas2Note,
        occurredAt = it.createdAt.toInstant(),
        label = "Note",
        createdByName = it.createdByUser.name,
        body = it.body,
      )
    }
  }

  private fun addStatusUpdateEvents(jpa: Cas2ApplicationEntity, timelineEvents: MutableList<Cas2HdcTimelineEvent>) {
    jpa.statusUpdates?.forEach {
      timelineEvents += Cas2HdcTimelineEvent(
        type = TimelineEventType.cas2StatusUpdate,
        occurredAt = it.createdAt.toInstant(),
        label = it.label,
        createdByName = it.assessor.name,
        body = it.statusUpdateDetails?.joinToString { detail -> detail.label },
      )
    }
  }

  private fun addSubmittedEvent(jpa: Cas2ApplicationEntity, timelineEvents: MutableList<Cas2HdcTimelineEvent>) {
    if (jpa.submittedAt !== null) {
      val submittedAtEvent = Cas2HdcTimelineEvent(
        type = TimelineEventType.cas2ApplicationSubmitted,
        occurredAt = jpa.submittedAt?.toInstant()!!,
        label = "Application submitted",
        createdByName = jpa.createdByUser.name,
      )
      timelineEvents += submittedAtEvent
    }
  }
}
