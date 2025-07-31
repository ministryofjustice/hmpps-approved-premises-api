package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
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
}
