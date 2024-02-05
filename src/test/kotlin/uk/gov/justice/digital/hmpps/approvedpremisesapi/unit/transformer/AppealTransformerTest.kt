package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AppealEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AppealTransformer
import java.time.Instant
import java.time.temporal.ChronoUnit

class AppealTransformerTest {
  private val appealTransformer = AppealTransformer()

  @Test
  fun `A newly created appeal is transformed correctly`() {
    val appealEntity = AppealEntityFactory()
      .produce()

    val transformedAppeal = appealTransformer.transformJpaToApi(appealEntity)

    assertThat(transformedAppeal.id).isEqualTo(appealEntity.id)
    assertThat(transformedAppeal.appealDate).isEqualTo(appealEntity.appealDate)
    assertThat(transformedAppeal.appealDetail).isEqualTo(appealEntity.appealDetail)
    assertThat(transformedAppeal.reviewer).isEqualTo(appealEntity.reviewer)
    assertThat(transformedAppeal.createdAt).isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS))
    assertThat(transformedAppeal.applicationId).isEqualTo(appealEntity.application.id)
    assertThat(transformedAppeal.createdByUserId).isEqualTo(appealEntity.createdBy.id)
    assertThat(transformedAppeal.decision.value).isEqualTo(appealEntity.decision)
    assertThat(transformedAppeal.decisionDetail).isEqualTo(appealEntity.decisionDetail)
    assertThat(transformedAppeal.assessmentId).isNull()
  }

  @Test
  fun `An appeal with an assessment linked is transformed correctly`() {
    val appealEntity = AppealEntityFactory()
      .produce()

    appealEntity.assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(appealEntity.application)
      .produce()

    val transformedAppeal = appealTransformer.transformJpaToApi(appealEntity)

    assertThat(transformedAppeal.id).isEqualTo(appealEntity.id)
    assertThat(transformedAppeal.appealDate).isEqualTo(appealEntity.appealDate)
    assertThat(transformedAppeal.appealDetail).isEqualTo(appealEntity.appealDetail)
    assertThat(transformedAppeal.reviewer).isEqualTo(appealEntity.reviewer)
    assertThat(transformedAppeal.createdAt).isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS))
    assertThat(transformedAppeal.applicationId).isEqualTo(appealEntity.application.id)
    assertThat(transformedAppeal.createdByUserId).isEqualTo(appealEntity.createdBy.id)
    assertThat(transformedAppeal.decision.value).isEqualTo(appealEntity.decision)
    assertThat(transformedAppeal.decisionDetail).isEqualTo(appealEntity.decisionDetail)
    assertThat(transformedAppeal.assessmentId).isEqualTo(appealEntity.assessment!!.id)
  }
}
