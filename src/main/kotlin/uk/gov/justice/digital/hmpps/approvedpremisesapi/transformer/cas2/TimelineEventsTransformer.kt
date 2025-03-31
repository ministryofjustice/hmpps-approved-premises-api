package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity

@Component("Cas2TimelineEventsTransformer")
class TimelineEventsTransformer(
  private val prisonsApiClient: PrisonsApiClient,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun transformApplicationToTimelineEvents(jpa: Cas2ApplicationEntity): List<Cas2TimelineEvent> {
    val timelineEvents: MutableList<Cas2TimelineEvent> = mutableListOf()

    addSubmittedEvent(jpa, timelineEvents)

    addStatusUpdateEvents(jpa, timelineEvents)

    addNoteEvents(jpa, timelineEvents)

    addPrisonAndPomEvents(jpa, timelineEvents)

    return timelineEvents.sortedByDescending { it.occurredAt }
  }

  private fun addNoteEvents(jpa: Cas2ApplicationEntity, timelineEvents: MutableList<Cas2TimelineEvent>) {
    jpa.notes?.forEach {
      timelineEvents += Cas2TimelineEvent(
        type = TimelineEventType.cas2Note,
        occurredAt = it.createdAt.toInstant(),
        label = "Note",
        createdByName = it.getUser().name,
        body = it.body,
      )
    }
  }

  private fun addStatusUpdateEvents(jpa: Cas2ApplicationEntity, timelineEvents: MutableList<Cas2TimelineEvent>) {
    jpa.statusUpdates?.forEach {
      timelineEvents += Cas2TimelineEvent(
        type = TimelineEventType.cas2StatusUpdate,
        occurredAt = it.createdAt.toInstant(),
        label = it.label,
        createdByName = it.assessor.name,
        body = it.statusUpdateDetails?.joinToString { detail -> detail.label },
      )
    }
  }

  private fun addSubmittedEvent(jpa: Cas2ApplicationEntity, timelineEvents: MutableList<Cas2TimelineEvent>) {
    if (jpa.submittedAt !== null) {
      val submittedAtEvent = Cas2TimelineEvent(
        type = TimelineEventType.cas2ApplicationSubmitted,
        occurredAt = jpa.submittedAt?.toInstant()!!,
        label = "Application submitted",
        createdByName = jpa.createdByUser.name,
      )
      timelineEvents += submittedAtEvent
    }
  }

  private fun addPrisonAndPomEvents(cas2Application: Cas2ApplicationEntity, timelineEvents: MutableList<Cas2TimelineEvent>) {
    val applicationAssignments = cas2Application.applicationAssignments.sortedBy { a -> a.createdAt }
    val prisonNames = mutableMapOf<String, String>()

    for (index in 1..<applicationAssignments.size) {
      val applicationAssignment = applicationAssignments[index]
      timelineEvents += when (applicationAssignment.allocatedPomUser) {
        null -> {
          val transferringPrisonName = getPrisonName(applicationAssignments[index - 1].prisonCode, prisonNames)
          val receivingPrisonName = getPrisonName(applicationAssignment.prisonCode, prisonNames)
          Cas2TimelineEvent(
            type = TimelineEventType.cas2PrisonTransfer,
            occurredAt = applicationAssignment.createdAt.toInstant(),
            label = "Prison transfer from $transferringPrisonName to $receivingPrisonName",
            createdByName = null,
          )
        }

        else ->
          Cas2TimelineEvent(
            type = TimelineEventType.cas2NewPomAssigned,
            occurredAt = applicationAssignment.createdAt.toInstant(),
            label = "New Prison offender manager ${applicationAssignment.allocatedPomUser?.name} allocated",
            createdByName = null,
          )
      }
    }
  }

  private fun getPrisonName(prisonCode: String, prisonNames: MutableMap<String, String>) = prisonNames[prisonCode] ?: when (val agency = prisonsApiClient.getAgencyDetails(prisonCode)) {
    is ClientResult.Success -> agency.body.description.also { prisonNames[prisonCode] = it }
    else -> {
      log.warn("Unknown prison name for prison code '$prisonCode'.")
      prisonCode
    }
  }
}
