package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AppealEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AppealTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import java.time.Instant
import java.time.temporal.ChronoUnit

class AppealTransformerTest {
  private val mockUserTransformer = mockk<UserTransformer>()
  private val appealTransformer = AppealTransformer(mockUserTransformer)

  private val mockUser = mockk<User>()

  @BeforeEach
  fun setup() {
    every { mockUserTransformer.transformJpaToApi(any(), any()) } returns mockUser
  }

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
    assertThat(transformedAppeal.createdByUser).isEqualTo(mockUser)
    assertThat(transformedAppeal.decision.value).isEqualTo(appealEntity.decision)
    assertThat(transformedAppeal.decisionDetail).isEqualTo(appealEntity.decisionDetail)
    assertThat(transformedAppeal.assessmentId).isEqualTo(appealEntity.assessment.id)

    verify(exactly = 1) { mockUserTransformer.transformJpaToApi(appealEntity.createdBy, ServiceName.approvedPremises) }
  }
}
