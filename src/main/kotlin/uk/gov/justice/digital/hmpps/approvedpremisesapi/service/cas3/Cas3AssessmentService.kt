package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.findAssessmentById
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import java.time.LocalDate
import java.util.UUID

@Service
class Cas3AssessmentService(
  private val assessmentRepository: AssessmentRepository,
  private val userAccessService: UserAccessService,
) {

  @Suppress("ReturnCount")
  fun updateAssessment(
    user: UserEntity,
    assessmentId: UUID,
    updateAssessment: UpdateAssessment,
  ): AuthorisableActionResult<ValidatableActionResult<AssessmentEntity>> {
    val assessment: TemporaryAccommodationAssessmentEntity = (
      assessmentRepository.findAssessmentById(assessmentId)
        ?: return AuthorisableActionResult.NotFound()
      )

    if (!userAccessService.userCanViewAssessment(user, assessment)) {
      return AuthorisableActionResult.Unauthorised()
    }
    if (updateAssessment.releaseDate != null && updateAssessment.accommodationRequiredFromDate != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("Cannot update both dates"),
      )
    }

    updateAssessment.releaseDate?.apply {
      if (isNotValidateReleaseDate(this, assessment)) return notAfterValidationResult()
      assessment.releaseDate = this
    } ?: updateAssessment.accommodationRequiredFromDate?.apply {
      if (isNotValidAccommodationRequiredFromDate(this, assessment)) return notBeforeValidationResult()
      assessment.accommodationRequiredFromDate = this
    }

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(assessmentRepository.save(assessment)),
    )
  }

  private fun notBeforeValidationResult(): AuthorisableActionResult.Success<ValidatableActionResult<AssessmentEntity>> =
    AuthorisableActionResult.Success(ValidatableActionResult.GeneralValidationError("Accommodation required from date cannot be before release date."))

  private fun isNotValidAccommodationRequiredFromDate(
    accommodationRequiredFromDate: LocalDate,
    assessment: TemporaryAccommodationAssessmentEntity,
  ): Boolean {
    return accommodationRequiredFromDate.isBefore(
      assessment.releaseDate
        ?: assessment.typedApplication<TemporaryAccommodationApplicationEntity>().arrivalDate?.toLocalDate(),
    )
  }

  private fun notAfterValidationResult(): AuthorisableActionResult.Success<ValidatableActionResult<AssessmentEntity>> =
    AuthorisableActionResult.Success(ValidatableActionResult.GeneralValidationError("Release date cannot be after accommodation required from date."))

  private fun isNotValidateReleaseDate(
    releaseDate: LocalDate,
    assessment: TemporaryAccommodationAssessmentEntity,
  ): Boolean {
    return releaseDate.isAfter(
      assessment.accommodationRequiredFromDate
        ?: assessment.typedApplication<TemporaryAccommodationApplicationEntity>().arrivalDate?.toLocalDate(),
    )
  }
}
