package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentClarificationNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1AssessmentClarificationNoteTransformer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1AssessmentClarificationNoteTransformerTest {

  private val cas1AssessmentClarificationNoteTransformer = Cas1AssessmentClarificationNoteTransformer()

  private val clarificationNoteEntity = AssessmentClarificationNoteEntityFactory()
    .withAssessment(ApprovedPremisesAssessmentEntityFactory().withDefaults().produce())
    .withResponse("Some response")
    .withId(UUID.randomUUID())
    .withResponseReceivedOn(LocalDate.of(2022, 10, 1))
    .withQuery("some query")
    .withCreatedBy(UserEntityFactory().withDefaults().produce())
    .withCreatedAt(OffsetDateTime.now())
    .produce()

  @Test
  fun `transform an clarificationNoteEntity to Cas1ClarificationNote`() {
    val transformedClarificationNote = cas1AssessmentClarificationNoteTransformer.transformJpaToCas1ClarificationNote(clarificationNoteEntity)

    Assertions.assertThat(transformedClarificationNote).isEqualTo(
      Cas1ClarificationNote(
        id = transformedClarificationNote.id,
        createdAt = transformedClarificationNote.createdAt,
        createdByStaffMemberId = transformedClarificationNote.createdByStaffMemberId,
        query = transformedClarificationNote.query,
        responseReceivedOn = transformedClarificationNote.responseReceivedOn,
        response = transformedClarificationNote.response,
      ),
    )
  }
}
