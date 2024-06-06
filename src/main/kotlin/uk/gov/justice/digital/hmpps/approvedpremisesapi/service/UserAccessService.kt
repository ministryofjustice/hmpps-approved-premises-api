package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_REPORTER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import java.util.UUID

@Service
class UserAccessService(
  private val userService: UserService,
  private val offenderService: OffenderService,
  private val requestContextService: RequestContextService,
) {
  fun currentUserCanAccessRegion(probationRegionId: UUID?) =
    userCanAccessRegion(userService.getUserForRequest(), probationRegionId)

  fun userCanAccessRegion(user: UserEntity, probationRegionId: UUID?) =
    userHasAllRegionsAccess(user) || user.probationRegion.id == probationRegionId

  fun currentUserHasAllRegionsAccess() = userHasAllRegionsAccess(userService.getUserForRequest())

  fun userHasAllRegionsAccess(user: UserEntity) =
    when (requestContextService.getServiceForRequest()) {
      // TODO: Revisit once Temporary Accommodation introduces user roles
      ServiceName.temporaryAccommodation -> user.hasRole(CAS3_REPORTER)
      // TODO: Revisit if Approved Premises introduces region-limited access
      else -> true
    }

  fun currentUserCanViewPremises(premises: PremisesEntity) =
    userCanViewPremises(userService.getUserForRequest(), premises)

  fun userCanViewPremises(user: UserEntity, premises: PremisesEntity) = when (premises) {
    is ApprovedPremisesEntity -> true
    is TemporaryAccommodationPremisesEntity -> userCanAccessRegion(user, premises.probationRegion.id) && user.hasRole(UserRole.CAS3_ASSESSOR)
    else -> false
  }

  fun currentUserCanManagePremises(premises: PremisesEntity) =
    userCanManagePremises(userService.getUserForRequest(), premises)

  fun userCanManagePremises(user: UserEntity, premises: PremisesEntity) = when (premises) {
    is ApprovedPremisesEntity -> true
    is TemporaryAccommodationPremisesEntity -> userCanAccessRegion(user, premises.probationRegion.id) && user.hasRole(UserRole.CAS3_ASSESSOR)
    else -> false
  }

  fun currentUserCanManagePremisesBookings(premises: PremisesEntity) =
    userCanManagePremisesBookings(userService.getUserForRequest(), premises)

  fun userCanViewBooking(user: UserEntity, booking: BookingEntity) = when (booking.premises) {
    is ApprovedPremisesEntity -> true
    is TemporaryAccommodationPremisesEntity -> userCanManagePremisesBookings(user, booking.premises)
    else -> false
  }

  fun userCanManagePremisesBookings(user: UserEntity, premises: PremisesEntity) = when (premises) {
    is ApprovedPremisesEntity -> user.hasAnyRole(UserRole.CAS1_FUTURE_MANAGER, UserRole.CAS1_LEGACY_MANAGER, UserRole.CAS1_MANAGER, UserRole.CAS1_MATCHER, UserRole.CAS1_WORKFLOW_MANAGER)
    is TemporaryAccommodationPremisesEntity -> userCanAccessRegion(user, premises.probationRegion.id) && user.hasRole(UserRole.CAS3_ASSESSOR)
    else -> false
  }

  /**
   * This function only checks if the user has the correct permissions to cancel the given booking.
   *
   * It doesn't consider if the booking is in a cancellable state
   */
  fun userMayCancelBooking(user: UserEntity, booking: BookingEntity) = when (booking.premises) {
    is ApprovedPremisesEntity -> user.hasAnyRole(UserRole.CAS1_WORKFLOW_MANAGER)
    is TemporaryAccommodationPremisesEntity -> userCanManagePremisesBookings(user, booking.premises)
    else -> false
  }

  /**
   * Currently delegates to the lost bed logic. May depend on different roles in the future.
   */
  fun currentUserCanManagePremisesOutOfServiceBed(premises: PremisesEntity) =
    userCanManagePremisesOutOfServiceBed(userService.getUserForRequest(), premises)

  /**
   * Currently delegates to the lost bed logic. May depend on different roles in the future.
   */
  fun userCanManagePremisesOutOfServiceBed(user: UserEntity, premises: PremisesEntity) =
    userCanManagePremisesLostBeds(user, premises)

  fun currentUserCanViewOutOfServiceBeds() =
    userCanViewOutOfServiceBeds(userService.getUserForRequest())

  fun userCanViewOutOfServiceBeds(user: UserEntity) =
    user.hasRole(UserRole.CAS1_WORKFLOW_MANAGER)

  fun currentUserCanManagePremisesLostBeds(premises: PremisesEntity) =
    userCanManagePremisesLostBeds(userService.getUserForRequest(), premises)

  fun userCanManagePremisesLostBeds(user: UserEntity, premises: PremisesEntity) = when (premises) {
    is ApprovedPremisesEntity -> user.hasAnyRole(UserRole.CAS1_FUTURE_MANAGER, UserRole.CAS1_LEGACY_MANAGER, UserRole.CAS1_MANAGER, UserRole.CAS1_MATCHER)
    is TemporaryAccommodationPremisesEntity -> userCanAccessRegion(user, premises.probationRegion.id) && user.hasRole(UserRole.CAS3_ASSESSOR)
    else -> false
  }

  fun currentUserCanViewPremisesCapacity(premises: PremisesEntity) =
    userCanViewPremisesCapacity(userService.getUserForRequest(), premises)

  fun userCanViewPremisesCapacity(user: UserEntity, premises: PremisesEntity) = when (premises) {
    is ApprovedPremisesEntity -> user.hasAnyRole(UserRole.CAS1_FUTURE_MANAGER, UserRole.CAS1_LEGACY_MANAGER, UserRole.CAS1_MANAGER, UserRole.CAS1_MATCHER)
    is TemporaryAccommodationPremisesEntity -> userCanAccessRegion(user, premises.probationRegion.id) && user.hasRole(UserRole.CAS3_ASSESSOR)
    else -> false
  }

  fun currentUserCanViewPremisesStaff(premises: PremisesEntity) =
    userCanViewPremisesStaff(userService.getUserForRequest(), premises)

  fun userCanViewPremisesStaff(user: UserEntity, premises: PremisesEntity) = when (premises) {
    is ApprovedPremisesEntity -> user.hasAnyRole(UserRole.CAS1_FUTURE_MANAGER, UserRole.CAS1_LEGACY_MANAGER, UserRole.CAS1_MANAGER, UserRole.CAS1_MATCHER)
    is TemporaryAccommodationPremisesEntity -> userCanAccessRegion(user, premises.probationRegion.id) && user.hasRole(UserRole.CAS3_ASSESSOR)
    else -> false
  }

  fun currentUserCanViewReport() =
    userCanViewReport(userService.getUserForRequest())

  fun userCanViewReport(user: UserEntity) =
    when (requestContextService.getServiceForRequest()) {
      ServiceName.temporaryAccommodation -> user.hasAnyRole(UserRole.CAS3_ASSESSOR, CAS3_REPORTER)
      ServiceName.approvedPremises -> user.hasAnyRole(UserRole.CAS1_REPORT_VIEWER, UserRole.CAS1_WORKFLOW_MANAGER, UserRole.CAS1_ADMIN)
      else -> false
    }

  fun getTemporaryAccommodationApplicationAccessLevelForCurrentUser(): TemporaryAccommodationApplicationAccessLevel =
    getTemporaryAccommodationApplicationAccessLevelForUser(userService.getUserForRequest())

  fun getTemporaryAccommodationApplicationAccessLevelForUser(user: UserEntity): TemporaryAccommodationApplicationAccessLevel = when {
    user.hasRole(UserRole.CAS3_ASSESSOR) -> TemporaryAccommodationApplicationAccessLevel.SUBMITTED_IN_REGION
    user.hasRole(UserRole.CAS3_REFERRER) -> TemporaryAccommodationApplicationAccessLevel.SELF
    else -> TemporaryAccommodationApplicationAccessLevel.NONE
  }

  fun userCanViewApplication(user: UserEntity, application: ApplicationEntity): Boolean {
    if (user.id == application.createdByUser.id) {
      return true
    }

    return when (application) {
      is ApprovedPremisesApplicationEntity -> userCanViewApprovedPremisesApplicationCreatedBySomeoneElse(user, application)
      is TemporaryAccommodationApplicationEntity -> userCanViewTemporaryAccommodationApplicationCreatedBySomeoneElse(user, application)
      else -> false
    }
  }

  private fun userCanViewApprovedPremisesApplicationCreatedBySomeoneElse(
    user: UserEntity,
    application: ApprovedPremisesApplicationEntity,
  ) = offenderService.getOffenderByCrn(application.crn, user.deliusUsername, user.hasQualification(UserQualification.LAO)) is AuthorisableActionResult.Success

  private fun userCanViewTemporaryAccommodationApplicationCreatedBySomeoneElse(
    user: UserEntity,
    application: TemporaryAccommodationApplicationEntity,
  ): Boolean {
    return userCanAccessRegion(user, application.probationRegion.id) &&
      user.hasRole(UserRole.CAS3_ASSESSOR) &&
      application.submittedAt != null
  }

  fun currentUserCanReallocateTask() = userCanReallocateTask(userService.getUserForRequest())

  fun userCanReallocateTask(user: UserEntity): Boolean = when (requestContextService.getServiceForRequest()) {
    ServiceName.temporaryAccommodation -> user.hasRole(UserRole.CAS3_ASSESSOR)
    ServiceName.approvedPremises -> user.hasRole(UserRole.CAS1_WORKFLOW_MANAGER)
    else -> false
  }

  fun currentUserCanDeallocateTask() = userCanDeallocateTask(userService.getUserForRequest())

  fun userCanDeallocateTask(user: UserEntity): Boolean = when (requestContextService.getServiceForRequest()) {
    ServiceName.temporaryAccommodation -> user.hasRole(UserRole.CAS3_ASSESSOR)
    else -> false
  }

  fun currentUserCanViewAssessment(assessment: AssessmentEntity): Boolean =
    userCanViewAssessment(userService.getUserForRequest(), assessment)

  fun userCanViewAssessment(user: UserEntity, assessment: AssessmentEntity): Boolean = when (assessment) {
    is ApprovedPremisesAssessmentEntity ->
      true

    is TemporaryAccommodationAssessmentEntity ->
      user.hasRole(UserRole.CAS3_ASSESSOR) &&
        userCanAccessRegion(user, (assessment.application as TemporaryAccommodationApplicationEntity).probationRegion.id)

    else -> false
  }

  /**
   * This function only checks if the user has the correct permissions to withdraw the given application.
   *
   * It doesn't consider if the application is in a withdrawable state
   */
  fun userMayWithdrawApplication(user: UserEntity, application: ApplicationEntity): Boolean = when (application) {
    is ApprovedPremisesApplicationEntity ->
      application.createdByUser == user || (
        application.isSubmitted() && user.hasRole(UserRole.CAS1_WORKFLOW_MANAGER)
        )
    else -> false
  }

  /**
   * This function only checks if the user has the correct permissions to withdraw the given placement request.
   *
   * It doesn't consider if the placement request is in a withdrawable state
   */
  fun userMayWithdrawPlacementRequest(user: UserEntity, placementRequest: PlacementRequestEntity) =
    placementRequest.application.createdByUser == user ||
      user.hasRole(UserRole.CAS1_WORKFLOW_MANAGER)

  /**
   * This function only checks if the user has the correct permissions to withdraw the given placement application.
   *
   * It doesn't consider if the placement request is in a withdrawable state
   */
  fun userMayWithdrawPlacementApplication(user: UserEntity, placementApplication: PlacementApplicationEntity) =
    placementApplication.createdByUser == user ||
      (placementApplication.isSubmitted() && user.hasRole(UserRole.CAS1_WORKFLOW_MANAGER))
}

enum class ApprovedPremisesApplicationAccessLevel {
  ALL,
  TEAM,
}

enum class TemporaryAccommodationApplicationAccessLevel {
  SUBMITTED_IN_REGION,
  SELF,
  NONE,
}
