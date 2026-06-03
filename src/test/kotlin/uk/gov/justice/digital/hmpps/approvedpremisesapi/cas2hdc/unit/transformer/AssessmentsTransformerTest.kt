package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.unit.transformer

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.AssessmentsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.StatusUpdateTransformer

class AssessmentsTransformerTest {
  private val mockStatusUpdateEntity = mockk<Cas2StatusUpdateEntity>()
  private val assessmentEntity = Cas2AssessmentEntityFactory()
    .withNacroReferralId("NACRO_ID")
    .withAssessorName("Firsty Lasty")
    .withStatusUpdates(mutableListOf(mockStatusUpdateEntity, mockStatusUpdateEntity))
    .produce()
  private val mockStatusUpdateTransformer = mockk<StatusUpdateTransformer>()
  private val mockStatusUpdateApi = mockk<Cas2StatusUpdate>()
  private val assessmentsTransformer = AssessmentsTransformer(mockStatusUpdateTransformer)

  @Test
  fun `transforms an assessment entity`() {
    every { mockStatusUpdateTransformer.transformJpaToApi(mockStatusUpdateEntity) } returns mockStatusUpdateApi
    val transformation = assessmentsTransformer.transformJpaToApiRepresentation(assessmentEntity)

    Assertions.assertThat(transformation).isEqualTo(
      Cas2Assessment(
        assessmentEntity.id,
        assessmentEntity.nacroReferralId,
        assessmentEntity.assessorName,
        listOf(mockStatusUpdateApi, mockStatusUpdateApi),
      ),
    )
  }
}
