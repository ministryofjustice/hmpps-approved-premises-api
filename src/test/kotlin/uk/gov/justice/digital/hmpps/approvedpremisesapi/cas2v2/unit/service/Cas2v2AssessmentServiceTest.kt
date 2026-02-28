package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.unit.service

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import java.time.OffsetDateTime
import java.util.UUID

class Cas2v2AssessmentServiceTest {

  private val mockCas2AssessmentRepository = mockk<Cas2AssessmentRepository>()

  private val cas2AssessmentService = Cas2AssessmentService(
    mockCas2AssessmentRepository,
  )

  @Nested
  inner class CreateAssessment {

    @Test
    fun `saves and returns entity from db`() {
      val cas2v2Application = Cas2ApplicationEntityFactory()
        .withServiceOrigin(Cas2ServiceOrigin.BAIL)
        .withCreatedByUser(
          Cas2UserEntityFactory()
            .withServiceOrigin(Cas2ServiceOrigin.BAIL)
            .produce(),
        ).produce()
      val assessEntity = Cas2AssessmentEntity(
        id = UUID.randomUUID(),
        application = cas2v2Application,
        createdAt = OffsetDateTime.now(),
        serviceOrigin = cas2v2Application.serviceOrigin,
      )

      every { mockCas2AssessmentRepository.save(any()) } answers
        {
          assessEntity
        }

      val result = cas2AssessmentService.createCas2Assessment(
        cas2v2Application,
      )
      Assertions.assertThat(result).isEqualTo(assessEntity)

      verify(exactly = 1) {
        mockCas2AssessmentRepository.save(
          match { it.application == cas2v2Application },
        )
      }
    }
  }

  @Nested
  inner class UpdateAssessment {

    @Test
    fun `saves and returns entity from db`() {
      val assessmentId = UUID.randomUUID()
      val cas2v2Application = Cas2ApplicationEntityFactory()
        .withServiceOrigin(Cas2ServiceOrigin.BAIL)
        .withCreatedByUser(
          Cas2UserEntityFactory()
            .withServiceOrigin(Cas2ServiceOrigin.BAIL)
            .produce(),
        ).produce()
      val assessEntity = Cas2AssessmentEntity(
        id = assessmentId,
        application = cas2v2Application,
        createdAt = OffsetDateTime.now(),
        serviceOrigin = cas2v2Application.serviceOrigin,
      )

      val newAssessmentData = UpdateCas2Assessment(
        nacroReferralId = "1234OH",
        assessorName = "Anne Assessor",
      )

      every { mockCas2AssessmentRepository.save(any()) } answers
        {
          assessEntity
        }

      every { mockCas2AssessmentRepository.findByIdAndServiceOrigin(assessmentId, Cas2ServiceOrigin.BAIL) } answers
        {
          assessEntity
        }

      val result = cas2AssessmentService.updateAssessment(
        assessmentId = assessmentId,
        newAssessment = newAssessmentData,
        serviceOrigin = Cas2ServiceOrigin.BAIL,
        )
      Assertions.assertThat(result).isEqualTo(

        CasResult.Success(assessEntity),

      )

      verify(exactly = 1) {
        mockCas2AssessmentRepository.save(
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

      every { mockCas2AssessmentRepository.findByIdAndServiceOrigin(assessmentId, Cas2ServiceOrigin.BAIL) } answers
        {
          null
        }

      val result = cas2AssessmentService.updateAssessment(
        assessmentId = assessmentId,
        newAssessment = newAssessmentData,
        serviceOrigin = Cas2ServiceOrigin.BAIL,
      )

      Assertions.assertThat(result is CasResult.NotFound).isTrue
    }
  }
}
