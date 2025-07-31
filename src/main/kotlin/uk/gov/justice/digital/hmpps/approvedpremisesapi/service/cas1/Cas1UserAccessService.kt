package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EnvironmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService

@Service
class Cas1UserAccessService(
  private val userService: UserService,
  private val environmentService: EnvironmentService,
) {

  fun ensureCurrentUserHasPermission(permission: UserPermission) {
    if (!currentUserHasPermission(permission)) {
      throw ForbiddenProblem("Permission ${permission.name} is required")
    }
  }

  fun currentUserHasPermission(permission: UserPermission): Boolean {
    if (!permission.isAvailable(environmentService)) {
      return false
    }
    return userService.getUserForRequest().hasPermission(permission)
  }

  /**
   * This function only checks if the user has the correct permissions to withdraw the given application.
   *
   * It doesn't consider if the application is in a withdrawable state
   */
  fun userMayWithdrawApplication(user: UserEntity, application: ApplicationEntity): Boolean = when (application) {
    is ApprovedPremisesApplicationEntity ->
      application.createdByUser == user ||
        (
          application.isSubmitted() && user.hasPermission(UserPermission.CAS1_APPLICATION_WITHDRAW_OTHERS)
          )
    else -> false
  }

  /**
   * This function only checks if the user has the correct permissions to withdraw the given placement request.
   *
   * It doesn't consider if the placement request is in a withdrawable state
   */
  fun userMayWithdrawPlacementRequest(user: UserEntity, placementRequest: PlacementRequestEntity) = placementRequest.application.createdByUser == user ||
    user.hasPermission(UserPermission.CAS1_REQUEST_FOR_PLACEMENT_WITHDRAW_OTHERS)

  /**
   * This function only checks if the user has the correct permissions to withdraw the given placement application.
   *
   * It doesn't consider if the placement request is in a withdrawable state
   */
  fun userMayWithdrawPlacementApplication(user: UserEntity, placementApplication: PlacementApplicationEntity) = placementApplication.createdByUser == user ||
    (placementApplication.isSubmitted() && user.hasPermission(UserPermission.CAS1_REQUEST_FOR_PLACEMENT_WITHDRAW_OTHERS))
}
