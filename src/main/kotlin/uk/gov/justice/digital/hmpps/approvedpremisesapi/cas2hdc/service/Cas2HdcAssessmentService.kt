package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcUpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.ValidatableActionResult
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas2HdcAssessmentService(
  private val assessmentRepository: Cas2AssessmentRepository,
) {

  @Transactional
  fun createCas2HdcAssessment(cas2ApplicationEntity: Cas2ApplicationEntity): Cas2AssessmentEntity = assessmentRepository.save(
    Cas2AssessmentEntity(
      id = UUID.randomUUID(),
      createdAt = OffsetDateTime.now(),
      application = cas2ApplicationEntity,
      serviceOrigin = cas2ApplicationEntity.serviceOrigin,
    ),
  )

  fun updateAssessment(assessmentId: UUID, newAssessment: Cas2HdcUpdateAssessment): AuthorisableActionResult<ValidatableActionResult<Cas2AssessmentEntity>> {
    val assessmentEntity = assessmentRepository.findByIdAndServiceOrigin(assessmentId, Cas2ServiceOrigin.HDC)
      ?: return AuthorisableActionResult.NotFound()

    assessmentEntity.apply {
      this.nacroReferralId = newAssessment.nacroReferralId
      this.assessorName = newAssessment.assessorName
    }

    val savedAssessment = assessmentRepository.save(assessmentEntity)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedAssessment),
    )
  }

  fun getAssessment(assessmentId: UUID): AuthorisableActionResult<Cas2AssessmentEntity> {
    val assessmentEntity = assessmentRepository.findByIdAndServiceOrigin(assessmentId, Cas2ServiceOrigin.HDC)
      ?: return AuthorisableActionResult.NotFound()

    return AuthorisableActionResult.Success(assessmentEntity)
  }
}
