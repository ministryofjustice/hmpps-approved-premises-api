package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelineNoteEntity

@Component
class ApplicationTimelineNoteTransformer(
  private val userTransformer: UserTransformer,
) {

  fun transformJpaToApi(jpa: ApplicationTimelineNoteEntity) = ApplicationTimelineNote(
    id = jpa.id,
    createdAt = jpa.createdAt.toInstant(),
    createdByUser = jpa.createdBy?.let { userTransformer.transformJpaToApi(it, ServiceName.approvedPremises) },
    note = jpa.body,
  )

  fun transformToCas1TimelineEvents(jpa: ApplicationTimelineNoteEntity) = Cas1TimelineEvent(
    type = Cas1TimelineEventType.applicationTimelineNote,
    id = jpa.id.toString(),
    occurredAt = jpa.createdAt.toInstant(),
    content = jpa.body,
    associatedUrls = emptyList(),
    triggerSource = null,
  )
}
