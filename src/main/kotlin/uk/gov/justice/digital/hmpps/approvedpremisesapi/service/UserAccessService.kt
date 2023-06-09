package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import java.util.UUID
import javax.servlet.http.HttpServletRequest

@Service
class UserAccessService(
  private val userService: UserService,
  private val currentRequest: HttpServletRequest,
) {
  fun currentUserCanAccessRegion(probationRegionId: UUID?) =
    userCanAccessRegion(userService.getUserForRequest(), probationRegionId)

  fun userCanAccessRegion(user: UserEntity, probationRegionId: UUID?) =
    userHasAllRegionsAccess(user) || user.probationRegion.id == probationRegionId

  fun currentUserHasAllRegionsAccess() = userHasAllRegionsAccess(userService.getUserForRequest())

  fun userHasAllRegionsAccess(user: UserEntity) =
    when (currentRequest.getHeader("X-Service-Name")) {
      // TODO: Revisit once Temporary Accommodation introduces user roles
      ServiceName.temporaryAccommodation.value -> false
      // TODO: Revisit if Approved Premises introduces region-limited access
      else -> true
    }

  fun currentUserCanViewPremises(premises: PremisesEntity) =
    userCanViewPremises(userService.getUserForRequest(), premises)

  fun userCanViewPremises(user: UserEntity, premises: PremisesEntity) = when (premises) {
    is ApprovedPremisesEntity -> true
    is TemporaryAccommodationPremisesEntity -> userCanAccessRegion(user, premises.probationRegion.id)
    else -> false
  }

  fun currentUserCanManagePremises(premises: PremisesEntity) =
    userCanManagePremises(userService.getUserForRequest(), premises)

  fun userCanManagePremises(user: UserEntity, premises: PremisesEntity) = when (premises) {
    is ApprovedPremisesEntity -> true
    is TemporaryAccommodationPremisesEntity -> userCanAccessRegion(user, premises.probationRegion.id)
    else -> false
  }

  fun currentUserCanManagePremisesBookings(premises: PremisesEntity) =
    userCanManagePremisesBookings(userService.getUserForRequest(), premises)

  fun userCanManagePremisesBookings(user: UserEntity, premises: PremisesEntity) = when (premises) {
    is ApprovedPremisesEntity -> user.hasAnyRole(UserRole.CAS1_MANAGER, UserRole.CAS1_MATCHER)
    is TemporaryAccommodationPremisesEntity -> userCanAccessRegion(user, premises.probationRegion.id)
    else -> false
  }

  fun currentUserCanManagePremisesLostBeds(premises: PremisesEntity) =
    userCanManagePremisesLostBeds(userService.getUserForRequest(), premises)

  fun userCanManagePremisesLostBeds(user: UserEntity, premises: PremisesEntity) = when (premises) {
    is ApprovedPremisesEntity -> user.hasAnyRole(UserRole.CAS1_MANAGER, UserRole.CAS1_MATCHER)
    is TemporaryAccommodationPremisesEntity -> userCanAccessRegion(user, premises.probationRegion.id)
    else -> false
  }

  fun currentUserCanViewPremisesCapacity(premises: PremisesEntity) =
    userCanViewPremisesCapacity(userService.getUserForRequest(), premises)

  fun userCanViewPremisesCapacity(user: UserEntity, premises: PremisesEntity) = when (premises) {
    is ApprovedPremisesEntity -> user.hasAnyRole(UserRole.CAS1_MANAGER, UserRole.CAS1_MATCHER)
    is TemporaryAccommodationPremisesEntity -> userCanAccessRegion(user, premises.probationRegion.id)
    else -> false
  }

  fun currentUserCanViewPremisesStaff(premises: PremisesEntity) =
    userCanViewPremisesStaff(userService.getUserForRequest(), premises)

  fun userCanViewPremisesStaff(user: UserEntity, premises: PremisesEntity) = when (premises) {
    is ApprovedPremisesEntity -> user.hasAnyRole(UserRole.CAS1_MANAGER, UserRole.CAS1_MATCHER)
    is TemporaryAccommodationPremisesEntity -> userCanAccessRegion(user, premises.probationRegion.id)
    else -> false
  }

  fun getApprovedPremisesApplicationAccessLevelForCurrentUser(): ApprovedPremisesApplicationAccessLevel =
    getApprovedPremisesApplicationAccessLevelForUser(userService.getUserForRequest())

  fun getApprovedPremisesApplicationAccessLevelForUser(user: UserEntity): ApprovedPremisesApplicationAccessLevel = when {
    user.hasAnyRole(UserRole.CAS1_WORKFLOW_MANAGER, UserRole.CAS1_ASSESSOR, UserRole.CAS1_MATCHER, UserRole.CAS1_MANAGER) -> ApprovedPremisesApplicationAccessLevel.ALL
    else -> ApprovedPremisesApplicationAccessLevel.TEAM
  }

  fun getTemporaryAccommodationApplicationAccessLevelForCurrentUser(): TemporaryAccommodationApplicationAccessLevel =
    getTemporaryAccommodationApplicationAccessLevelForUser(userService.getUserForRequest())

  fun getTemporaryAccommodationApplicationAccessLevelForUser(user: UserEntity): TemporaryAccommodationApplicationAccessLevel = when {
    user.hasRole(UserRole.CAS3_ASSESSOR) -> TemporaryAccommodationApplicationAccessLevel.SUBMITTED_IN_REGION
    user.hasRole(UserRole.CAS3_REFERRER) -> TemporaryAccommodationApplicationAccessLevel.SELF
    else -> TemporaryAccommodationApplicationAccessLevel.NONE
  }
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
