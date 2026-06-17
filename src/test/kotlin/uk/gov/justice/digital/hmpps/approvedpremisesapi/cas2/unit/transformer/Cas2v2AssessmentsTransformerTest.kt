package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.transformer

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.Cas2AssessmentsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.Cas2StatusUpdateTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2StatusUpdateEntity

class Cas2v2AssessmentsTransformerTest {
  private val mockCas2StatusUpdateEntity = mockk<Cas2StatusUpdateEntity>()
  private val cas2AssessmentEntity = Cas2AssessmentEntityFactory()
    .withNacroReferralId("NACRO_ID")
    .withAssessorName("Firsty Lasty")
    .withStatusUpdates(mutableListOf(mockCas2StatusUpdateEntity, mockCas2StatusUpdateEntity))
    .withServiceOrigin(Cas2ServiceOrigin.BAIL)
    .produce()
  private val mockCas2StatusUpdateTransformer = mockk<Cas2StatusUpdateTransformer>()
  private val mockStatusUpdateApi = mockk<Cas2StatusUpdate>()
  private val cas2AssessmentsTransformer = Cas2AssessmentsTransformer(mockCas2StatusUpdateTransformer)

  @Test
  fun `transforms a cas2v2Assessment entity`() {
    every { mockCas2StatusUpdateTransformer.transformJpaToApi(mockCas2StatusUpdateEntity) } returns mockStatusUpdateApi
    val transformation = cas2AssessmentsTransformer.transformJpaToApiRepresentation(cas2AssessmentEntity)

    Assertions.assertThat(transformation).isEqualTo(
      Cas2Assessment(
        cas2AssessmentEntity.id,
        cas2AssessmentEntity.nacroReferralId,
        cas2AssessmentEntity.assessorName,
        listOf(mockStatusUpdateApi, mockStatusUpdateApi),
      ),
    )
  }
}
