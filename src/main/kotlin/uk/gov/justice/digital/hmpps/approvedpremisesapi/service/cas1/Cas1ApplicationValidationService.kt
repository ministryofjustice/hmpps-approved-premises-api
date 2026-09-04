package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.TierService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import java.util.UUID

@Service
class Cas1ApplicationValidationService(
  private val applicationRepository: ApprovedPremisesApplicationRepository,
  private val tierService: TierService,
) {

  @SuppressWarnings("ReturnCount")
  fun validateApplicationSubmission(
    applicationId: UUID,
    user: UserEntity,
    submitApplication: SubmitApprovedPremisesApplication,
  ): CasResult<ApprovedPremisesApplicationEntity> {
    val application = applicationRepository.findByIdOrNull(applicationId)
      ?: return CasResult.NotFound("ApprovedPremisesApplicationEntity", applicationId.toString())

    if (application.createdByUser.id != user.id) {
      return CasResult.Unauthorised()
    }

    if (application.status != ApprovedPremisesApplicationStatus.STARTED) {
      return CasResult.GeneralValidationError("Only an application with the 'STARTED' status can be submitted")
    }

    if (application.submittedAt != null) {
      return CasResult.GeneralValidationError("This application has already been submitted")
    }

    if (submitApplication.caseManagerIsNotApplicant == true && submitApplication.caseManagerUserDetails == null) {
      return CasResult.GeneralValidationError("caseManagerUserDetails must be provided if caseManagerIsNotApplicant is true")
    }

    if (application.data == null) {
      return CasResult.FieldValidationError(mapOf("$.data" to "empty"))
    }

    val requestedDuration = submitApplication.requestedDuration()

    if (tierService.useTierV2() && requestedDuration == null) {
      return CasResult.GeneralValidationError(
        "Either duration or requestedPlacementDuration should be provided",
      )
    }

    if (
      submitApplication.requestedPlacementPeriod != null &&
      requestedDuration != submitApplication.requestedPlacementPeriod.duration
    ) {
      return CasResult.GeneralValidationError(
        "The requested placement period duration must match the duration specified in the application.",
      )
    }

    return CasResult.Success(application)
  }
}
