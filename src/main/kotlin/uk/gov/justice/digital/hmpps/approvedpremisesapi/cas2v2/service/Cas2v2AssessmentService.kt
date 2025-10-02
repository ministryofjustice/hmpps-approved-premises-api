package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2v2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import java.time.OffsetDateTime
import java.util.UUID

@Service("Cas2v2AssessmentService")
class Cas2v2AssessmentService(
  private val cas2AssessmentRepository: Cas2AssessmentRepository,
) {

  @Transactional
  fun createCas2v2Assessment(cas2ApplicationEntity: Cas2ApplicationEntity): Cas2AssessmentEntity = cas2AssessmentRepository.save(
    Cas2AssessmentEntity(
      id = UUID.randomUUID(),
      createdAt = OffsetDateTime.now(),
      application = cas2ApplicationEntity,
      serviceOrigin = cas2ApplicationEntity.serviceOrigin,
    ),
  )

  fun updateAssessment(
    assessmentId: UUID,
    newAssessment: UpdateCas2v2Assessment,
  ): CasResult<Cas2AssessmentEntity> {
    val assessmentEntity = cas2AssessmentRepository.findByIdAndServiceOrigin(assessmentId, Cas2ServiceOrigin.BAIL)
      ?: return CasResult.NotFound("Cas2AssessmentEntity", assessmentId.toString())

    assessmentEntity.apply {
      this.nacroReferralId = newAssessment.nacroReferralId
      this.assessorName = newAssessment.assessorName
    }

    val savedAssessment = cas2AssessmentRepository.save(assessmentEntity)

    return CasResult.Success(savedAssessment)
  }

  fun getAssessment(assessmentId: UUID): CasResult<Cas2AssessmentEntity> {
    val assessmentEntity = cas2AssessmentRepository.findByIdAndServiceOrigin(assessmentId, Cas2ServiceOrigin.BAIL)
      ?: return CasResult.NotFound("Cas2AssessmentEntity", assessmentId.toString())

    return CasResult.Success(assessmentEntity)
  }
}
