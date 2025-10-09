package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.unit.transformer

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.Cas2v2AssessmentsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.Cas2v2StatusUpdateTransformer

class Cas2v2AssessmentsTransformerTest {
  private val mockCas2StatusUpdateEntity = mockk<Cas2StatusUpdateEntity>()
  private val cas2AssessmentEntity = Cas2AssessmentEntityFactory()
    .withNacroReferralId("NACRO_ID")
    .withAssessorName("Firsty Lasty")
    .withStatusUpdates(mutableListOf(mockCas2StatusUpdateEntity, mockCas2StatusUpdateEntity))
    .produce()
  private val mockCas2v2StatusUpdateTransformer = mockk<Cas2v2StatusUpdateTransformer>()
  private val mockStatusUpdateApi = mockk<Cas2v2StatusUpdate>()
  private val cas2v2AssessmentsTransformer = Cas2v2AssessmentsTransformer(mockCas2v2StatusUpdateTransformer)

  @Test
  fun `transforms a cas2v2Assessment entity`() {
    every { mockCas2v2StatusUpdateTransformer.transformJpaToApi(mockCas2StatusUpdateEntity) } returns mockStatusUpdateApi
    val transformation = cas2v2AssessmentsTransformer.transformJpaToApiRepresentation(cas2AssessmentEntity)

    Assertions.assertThat(transformation).isEqualTo(
      Cas2v2Assessment(
        cas2AssessmentEntity.id,
        cas2AssessmentEntity.nacroReferralId,
        cas2AssessmentEntity.assessorName,
        listOf(mockStatusUpdateApi, mockStatusUpdateApi),
      ),
    )
  }
}
