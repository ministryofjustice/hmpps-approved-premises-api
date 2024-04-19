package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity

@Component
class AssessmentClarificationNoteTransformer {
  fun transformJpaToApi(jpa: AssessmentClarificationNoteEntity) = ClarificationNote(
    id = jpa.id,
    createdAt = jpa.createdAt.toInstant(),
    createdByStaffMemberId = jpa.createdByUser.id,
    query = jpa.query,
    response = jpa.response,
    responseReceivedOn = jpa.responseReceivedOn,
  )

  fun transformToTimelineEvent(jpa: AssessmentClarificationNoteEntity) = TimelineEvent(
    id = jpa.id.toString(),
    type = TimelineEventType.approvedPremisesInformationRequest,
    occurredAt = jpa.createdAt.toInstant(),
    associatedUrls = emptyList(),
    content = jpa.query,
  )
}
