package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ClarificationNote
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
}
