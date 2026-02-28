package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.UpdateCas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas2AssessmentService(
  private val assessmentRepository: Cas2AssessmentRepository,
) {

  @Transactional
  fun createCas2Assessment(cas2ApplicationEntity: Cas2ApplicationEntity): Cas2AssessmentEntity = assessmentRepository.save(
    Cas2AssessmentEntity(
      id = UUID.randomUUID(),
      createdAt = OffsetDateTime.now(),
      application = cas2ApplicationEntity,
      serviceOrigin = cas2ApplicationEntity.serviceOrigin,
    ),
  )

  fun updateAssessment(assessmentId: UUID, newAssessment: UpdateCas2Assessment, serviceOrigin: Cas2ServiceOrigin): CasResult<Cas2AssessmentEntity> {
    val assessmentEntity = assessmentRepository.findByIdAndServiceOrigin(assessmentId, serviceOrigin)
      ?: return CasResult.NotFound("Cas2AssessmentEntity", assessmentId.toString())

    assessmentEntity.apply {
      this.nacroReferralId = newAssessment.nacroReferralId
      this.assessorName = newAssessment.assessorName
    }

    val savedAssessment = assessmentRepository.save(assessmentEntity)

    return CasResult.Success(savedAssessment)
  }


  fun getAssessmentForHdc(assessmentId: UUID): CasResult<Cas2AssessmentEntity> {
    val assessmentEntity = assessmentRepository.findByIdAndServiceOrigin(assessmentId, Cas2ServiceOrigin.HDC)
      ?: return CasResult.NotFound("Cas2AssessmentEntity", assessmentId.toString())

    return CasResult.Success(assessmentEntity)
  }

  fun getAssessmentForBail(assessmentId: UUID): CasResult<Cas2AssessmentEntity> {
    val assessmentEntity = assessmentRepository.findByIdAndServiceOrigin(assessmentId, Cas2ServiceOrigin.BAIL)
      ?: return CasResult.NotFound("Cas2AssessmentEntity", assessmentId.toString())

    return CasResult.Success(assessmentEntity)
  }
}
