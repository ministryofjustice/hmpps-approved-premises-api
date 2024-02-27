package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import java.time.OffsetDateTime
import java.util.UUID
import javax.transaction.Transactional

@Service("Cas2AssessmentService")
class AssessmentService(
  private val assessmentRepository: Cas2AssessmentRepository,
) {

  @Transactional
  fun createCas2Assessment(cas2ApplicationEntity: Cas2ApplicationEntity): Cas2AssessmentEntity =
    assessmentRepository.save(
      Cas2AssessmentEntity(
        id = UUID.randomUUID(),
        createdAt = OffsetDateTime.now(),
        application = cas2ApplicationEntity,
      ),
    )

  fun updateAssessment(assessmentId: UUID, newAssessment: UpdateCas2Assessment): AuthorisableActionResult<ValidatableActionResult<Cas2AssessmentEntity>> {
    val assessmentEntity = assessmentRepository.findByIdOrNull(assessmentId)
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
}
