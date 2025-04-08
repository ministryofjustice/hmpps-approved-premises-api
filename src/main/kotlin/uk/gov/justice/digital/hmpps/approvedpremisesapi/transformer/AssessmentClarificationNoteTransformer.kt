package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity

@Component
class AssessmentClarificationNoteTransformer {
  @Deprecated("Will be removed soon - use transformJpaToCas1ClarificationNote")
  fun transformJpaToApi(jpa: AssessmentClarificationNoteEntity) = ClarificationNote(
    id = jpa.id,
    createdAt = jpa.createdAt.toInstant(),
    createdByStaffMemberId = jpa.createdByUser.id,
    query = jpa.query,
    response = jpa.response,
    responseReceivedOn = jpa.responseReceivedOn,
  )

  fun transformJpaToCas1ClarificationNote(jpa: AssessmentClarificationNoteEntity) = Cas1ClarificationNote(
    id = jpa.id,
    createdAt = jpa.createdAt.toInstant(),
    createdByStaffMemberId = jpa.createdByUser.id,
    query = jpa.query,
    response = jpa.response,
    responseReceivedOn = jpa.responseReceivedOn,
  )
}
