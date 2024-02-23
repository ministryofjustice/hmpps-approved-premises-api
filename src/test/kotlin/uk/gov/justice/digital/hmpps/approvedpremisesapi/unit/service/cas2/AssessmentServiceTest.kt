package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatcher
import org.mockito.kotlin.times
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentRepository
import java.time.OffsetDateTime
import java.util.UUID

class AssessmentServiceTest {
  private val mockAssessmentRepository = mockk<Cas2AssessmentRepository>()

  private val assessmentService = uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.AssessmentService(
    mockAssessmentRepository,
  )

  @Nested
  inner class CreateAssessment {

    @Test
    fun `saves and returns entity from db`() {
      val application = Cas2ApplicationEntityFactory()
        .withCreatedByUser(
          NomisUserEntityFactory()
            .produce(),
        ).produce()
      val assessEntity = Cas2AssessmentEntity(
        id = UUID.randomUUID(),
        application = application,
        createdAt = OffsetDateTime.now(),
      )

      every { mockAssessmentRepository.save(any()) } answers
        {
          assessEntity
        }

      val result = assessmentService.createCas2Assessment(
        application,
      )
      Assertions.assertThat(result).isEqualTo(assessEntity)

      verify(exactly = 1) {
        mockAssessmentRepository.save(
          match { it.application == application },
        )
      }
    }
  }

  class AssessmentMatcher(var application: Cas2ApplicationEntity) : ArgumentMatcher<Cas2AssessmentEntity> {

    override fun matches(assessment: Cas2AssessmentEntity): Boolean {
      return assessment.application.id == application.id
    }
  }
}
