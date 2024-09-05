package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3AssessmentUpdatedField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.findAssessmentById
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormat
import java.time.LocalDate
import java.util.UUID

@Service
class Cas3AssessmentService(
  private val assessmentRepository: AssessmentRepository,
  private val userAccessService: UserAccessService,
  private val domainEventService: DomainEventService,
  private val domainEventBuilder: DomainEventBuilder,
) {
  @Suppress("ReturnCount")
  fun updateAssessment(
    user: UserEntity,
    assessmentId: UUID,
    updateAssessment: UpdateAssessment,
  ): CasResult<TemporaryAccommodationAssessmentEntity> {
    val assessment: TemporaryAccommodationAssessmentEntity = (
      assessmentRepository.findAssessmentById(assessmentId)
        ?: return CasResult.NotFound(TemporaryAccommodationAssessmentEntity::class.simpleName, assessmentId.toString())
      )

    if (!userAccessService.userCanViewAssessment(user, assessment)) {
      return CasResult.Unauthorised()
    }

    if (updateAssessment.releaseDate != null && updateAssessment.accommodationRequiredFromDate != null) {
      return CasResult.GeneralValidationError("Cannot update both dates")
    }

    updateAssessment.releaseDate?.let { newReleaseDate ->
      val currentAccommodationReqDate = assessment.currentAccommodationRequiredFromDate()

      if (newReleaseDate.isAfter(currentAccommodationReqDate)) {
        return notAfterValidationResult(currentAccommodationReqDate)
      }

      val domainEvent =
        domainEventBuilder.buildAssessmentUpdatedDomainEvent(
          assessment = assessment,
          listOf(
            CAS3AssessmentUpdatedField(
              fieldName = "releaseDate",
              updatedFrom = assessment.currentReleaseDate().toUiFormat(),
              updatedTo = newReleaseDate.toUiFormat(),
            ),
          ),
        )
      domainEventService.saveAssessmentUpdatedEvent(domainEvent)
      assessment.releaseDate = newReleaseDate
    }

    updateAssessment.accommodationRequiredFromDate?.let { newAccommodationRequiredFromDate ->
      val currentReleaseDate = assessment.currentReleaseDate()

      if (newAccommodationRequiredFromDate.isBefore(currentReleaseDate)) {
        return notBeforeValidationResult(currentReleaseDate)
      }

      val domainEvent =
        domainEventBuilder.buildAssessmentUpdatedDomainEvent(
          assessment = assessment,
          listOf(
            CAS3AssessmentUpdatedField(
              fieldName = "accommodationRequiredFromDate",
              updatedFrom = assessment.currentAccommodationRequiredFromDate().toUiFormat(),
              updatedTo = newAccommodationRequiredFromDate.toUiFormat(),
            ),
          ),
        )
      domainEventService.saveAssessmentUpdatedEvent(domainEvent)
      assessment.accommodationRequiredFromDate = newAccommodationRequiredFromDate
    }

    return CasResult.Success(assessmentRepository.save(assessment))
  }

  private fun notBeforeValidationResult(existingDate: LocalDate) =
    CasResult.GeneralValidationError<TemporaryAccommodationAssessmentEntity>(
      "Accommodation required from date cannot be before release date: $existingDate",
    )

  private fun notAfterValidationResult(existingDate: LocalDate) =
    CasResult.GeneralValidationError<TemporaryAccommodationAssessmentEntity>(
      "Release date cannot be after accommodation required from date: $existingDate",
    )
}
