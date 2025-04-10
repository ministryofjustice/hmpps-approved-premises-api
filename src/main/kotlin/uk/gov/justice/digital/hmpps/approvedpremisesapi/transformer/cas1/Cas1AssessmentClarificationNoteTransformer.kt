package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity

@Component
class Cas1AssessmentClarificationNoteTransformer {
  fun transformJpaToCas1ClarificationNote(jpa: AssessmentClarificationNoteEntity) = Cas1ClarificationNote(
    id = jpa.id,
    createdAt = jpa.createdAt.toInstant(),
    createdByStaffMemberId = jpa.createdByUser.id,
    query = jpa.query,
    response = jpa.response,
    responseReceivedOn = jpa.responseReceivedOn,
  )
}
