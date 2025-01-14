package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas2v2

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2.Cas2v2AssessmentsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2.Cas2v2StatusUpdateTransformer

class Cas2v2AssessmentsTransformerTest {
  private val mockCas2v2StatusUpdateEntity = mockk<Cas2v2StatusUpdateEntity>()
  private val cas2v2AssessmentEntity = Cas2v2AssessmentEntityFactory()
    .withNacroReferralId("NACRO_ID")
    .withAssessorName("Firsty Lasty")
    .withStatusUpdates(mutableListOf(mockCas2v2StatusUpdateEntity, mockCas2v2StatusUpdateEntity))
    .produce()
  private val mockCas2v2StatusUpdateTransformer = mockk<Cas2v2StatusUpdateTransformer>()
  private val mockStatusUpdateApi = mockk<Cas2v2StatusUpdate>()
  private val cas2v2AssessmentsTransformer = Cas2v2AssessmentsTransformer(mockCas2v2StatusUpdateTransformer)

  @Test
  fun `transforms a cas2v2Assessment entity`() {
    every { mockCas2v2StatusUpdateTransformer.transformJpaToApi(mockCas2v2StatusUpdateEntity) } returns mockStatusUpdateApi
    val transformation = cas2v2AssessmentsTransformer.transformJpaToApiRepresentation(cas2v2AssessmentEntity)

    Assertions.assertThat(transformation).isEqualTo(
      Cas2v2Assessment(
        cas2v2AssessmentEntity.id,
        cas2v2AssessmentEntity.nacroReferralId,
        cas2v2AssessmentEntity.assessorName,
        listOf(mockStatusUpdateApi, mockStatusUpdateApi),
      ),
    )
  }
}
