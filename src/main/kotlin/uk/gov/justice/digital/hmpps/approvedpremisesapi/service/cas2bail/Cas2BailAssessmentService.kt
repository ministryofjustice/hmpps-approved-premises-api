package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2bail

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2Assessment

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailAssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import java.time.OffsetDateTime
import java.util.*


@Service("Cas2BailAssessmentService")
class Cas2BailAssessmentService (
  private val assessmentRepository: Cas2BailAssessmentRepository,
) {


  @Transactional
  fun createCas2BailAssessment(cas2BailApplicationEntity: Cas2BailApplicationEntity): Cas2BailAssessmentEntity =
    assessmentRepository.save(
      Cas2BailAssessmentEntity(
        id = UUID.randomUUID(),
        createdAt = OffsetDateTime.now(),
        application = cas2BailApplicationEntity,
      ),
    )

  fun updateAssessment(assessmentId: UUID, newAssessment: UpdateCas2Assessment): CasResult<Cas2BailAssessmentEntity> {
    val assessmentEntity = assessmentRepository.findByIdOrNull(assessmentId)
      ?: return CasResult.NotFound()

    assessmentEntity.apply {
      this.nacroReferralId = newAssessment.nacroReferralId
      this.assessorName = newAssessment.assessorName
    }

    val savedAssessment = assessmentRepository.save(assessmentEntity)

    return CasResult.Success(savedAssessment)
  }

  fun getAssessment(assessmentId: UUID): CasResult<Cas2BailAssessmentEntity> {
    val assessmentEntity = assessmentRepository.findByIdOrNull(assessmentId)
      ?: return CasResult.NotFound()

    return CasResult.Success(assessmentEntity)
  }
}