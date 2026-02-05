package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.UpdateCas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import java.time.OffsetDateTime
import java.util.UUID

class Cas2AssessmentServiceTest {
  private val mockAssessmentRepository = mockk<Cas2AssessmentRepository>()

  private val assessmentService = Cas2AssessmentService(
    mockAssessmentRepository,
  )

  @Nested
  inner class CreateAssessment {

    @Test
    fun `saves and returns entity from db`() {
      val application = Cas2ApplicationEntityFactory()
        .withCreatedByUser(
          Cas2UserEntityFactory()
            .produce(),
        ).produce()
      val assessEntity = Cas2AssessmentEntity(
        id = UUID.randomUUID(),
        application = application,
        createdAt = OffsetDateTime.now(),
        serviceOrigin = application.serviceOrigin,
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

  @Nested
  inner class UpdateAssessment {

    @Test
    fun `saves and returns entity from db`() {
      val assessmentId = UUID.randomUUID()
      val application = Cas2ApplicationEntityFactory()
        .withCreatedByUser(
          Cas2UserEntityFactory()
            .produce(),
        ).produce()
      val assessEntity = Cas2AssessmentEntity(
        id = assessmentId,
        application = application,
        createdAt = OffsetDateTime.now(),
        serviceOrigin = application.serviceOrigin,
      )

      val newAssessmentData = UpdateCas2Assessment(
        nacroReferralId = "1234OH",
        assessorName = "Anne Assessor",
      )

      every { mockAssessmentRepository.save(any()) } answers
        {
          assessEntity
        }

      every { mockAssessmentRepository.findByIdAndServiceOrigin(assessmentId, Cas2ServiceOrigin.HDC) } answers
        {
          assessEntity
        }

      val result = assessmentService.updateAssessment(
        assessmentId = assessmentId,
        newAssessment = newAssessmentData,
      )
      Assertions.assertThat(result).isEqualTo(
        AuthorisableActionResult.Success(
          ValidatableActionResult.Success(assessEntity),
        ),
      )

      verify(exactly = 1) {
        mockAssessmentRepository.save(
          match {
            it.id == assessEntity.id &&
              it.nacroReferralId == newAssessmentData.nacroReferralId &&
              it.assessorName == newAssessmentData.assessorName
          },
        )
      }
    }

    @Test
    fun `returns NotFound if entity is not found`() {
      val assessmentId = UUID.randomUUID()
      val newAssessmentData = UpdateCas2Assessment(
        nacroReferralId = "1234OH",
        assessorName = "Anne Assessor",
      )

      every { mockAssessmentRepository.findByIdAndServiceOrigin(assessmentId, Cas2ServiceOrigin.HDC) } answers
        {
          null
        }

      val result = assessmentService.updateAssessment(
        assessmentId = assessmentId,
        newAssessment = newAssessmentData,
      )

      Assertions.assertThat(result is AuthorisableActionResult.NotFound).isTrue
    }
  }
}
