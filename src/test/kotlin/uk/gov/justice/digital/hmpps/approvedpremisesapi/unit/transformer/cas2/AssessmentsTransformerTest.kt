package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas2

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.AssessmentsTransformer

class AssessmentsTransformerTest {
  private val assessmentEntity = Cas2AssessmentEntityFactory().produce()

  private val assessmentsTransformer = AssessmentsTransformer()

  @Test
  fun `transforms an assessment entity`() {
    val transformation = assessmentsTransformer.transformJpaToApiRepresentation(assessmentEntity)

    Assertions.assertThat(transformation).isEqualTo(
      Cas2Assessment(
        assessmentEntity.id,
        assessmentEntity.nacroReferralId,
        assessmentEntity.assessorName,
      ),
    )
  }
}
