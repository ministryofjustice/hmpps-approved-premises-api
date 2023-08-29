package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistorySystemNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryUserNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentReferralHistorySystemNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentReferralHistoryUserNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentReferralHistoryNoteTransformer

class AssessmentReferralHistoryNoteTransformerTest {
  @Test
  fun `transformJpaToApi transforms correctly for a user note`() {
    val probationRegion = ProbationRegionEntityFactory()
      .withApArea(ApAreaEntityFactory().produce())
      .produce()

    val user = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(user)
      .withProbationRegion(probationRegion)
      .produce()

    val assessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(application)
      .produce()

    val note = AssessmentReferralHistoryUserNoteEntityFactory()
      .withAssessment(assessment)
      .withCreatedBy(user)
      .produce()

    val result = AssessmentReferralHistoryNoteTransformer().transformJpaToApi(note)

    assertThat(result is ReferralHistoryUserNote).isTrue
    result as ReferralHistoryUserNote
    assertThat(result.id).isEqualTo(note.id)
    assertThat(result.createdAt).isEqualTo(note.createdAt.toInstant())
    assertThat(result.createdByUserName).isEqualTo(user.name)
    assertThat(result.message).isEqualTo(note.message)
    assertThat(result.type).isEqualTo("user")
  }

  @ParameterizedTest
  @EnumSource
  fun `transformJpaToApi transforms correctly for a system note`(noteType: ReferralHistorySystemNoteType) {
    val probationRegion = ProbationRegionEntityFactory()
      .withApArea(ApAreaEntityFactory().produce())
      .produce()

    val user = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(user)
      .withProbationRegion(probationRegion)
      .produce()

    val assessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(application)
      .produce()

    val note = AssessmentReferralHistorySystemNoteEntityFactory()
      .withAssessment(assessment)
      .withCreatedBy(user)
      .withType(noteType)
      .produce()

    val result = AssessmentReferralHistoryNoteTransformer().transformJpaToApi(note)

    assertThat(result is ReferralHistorySystemNote).isTrue
    result as ReferralHistorySystemNote
    assertThat(result.id).isEqualTo(note.id)
    assertThat(result.createdAt).isEqualTo(note.createdAt.toInstant())
    assertThat(result.createdByUserName).isEqualTo(user.name)
    assertThat(result.message).isEqualTo(note.message)
    assertThat(result.type).isEqualTo("system")
    assertThat(result.category).isEqualTo(
      when (noteType) {
        ReferralHistorySystemNoteType.SUBMITTED -> ReferralHistorySystemNote.Category.submitted
        ReferralHistorySystemNoteType.UNALLOCATED -> ReferralHistorySystemNote.Category.unallocated
        ReferralHistorySystemNoteType.IN_REVIEW -> ReferralHistorySystemNote.Category.inReview
        ReferralHistorySystemNoteType.READY_TO_PLACE -> ReferralHistorySystemNote.Category.readyToPlace
        ReferralHistorySystemNoteType.REJECTED -> ReferralHistorySystemNote.Category.rejected
        ReferralHistorySystemNoteType.COMPLETED -> ReferralHistorySystemNote.Category.completed
      },
    )
  }
}
