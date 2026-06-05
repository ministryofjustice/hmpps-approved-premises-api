package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.unit.transformer

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcAssessmentsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcStatusUpdateTransformer

class AssessmentsTransformerTest {
  private val mockStatusUpdateEntity = mockk<Cas2StatusUpdateEntity>()
  private val assessmentEntity = Cas2AssessmentEntityFactory()
    .withNacroReferralId("NACRO_ID")
    .withAssessorName("Firsty Lasty")
    .withStatusUpdates(mutableListOf(mockStatusUpdateEntity, mockStatusUpdateEntity))
    .produce()
  private val mockCas2HdcStatusUpdateTransformer = mockk<Cas2HdcStatusUpdateTransformer>()
  private val mockStatusUpdateApi = mockk<Cas2StatusUpdate>()
  private val cas2HdcAssessmentsTransformer = Cas2HdcAssessmentsTransformer(mockCas2HdcStatusUpdateTransformer)

  @Test
  fun `transforms an assessment entity`() {
    every { mockCas2HdcStatusUpdateTransformer.transformJpaToApi(mockStatusUpdateEntity) } returns mockStatusUpdateApi
    val transformation = cas2HdcAssessmentsTransformer.transformJpaToApiRepresentation(assessmentEntity)

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
