package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import java.util.UUID
import javax.servlet.http.HttpServletRequest

@Service
class UserAccessService(
  private val userService: UserService,
  private val currentRequest: HttpServletRequest,
) {
  fun currentUserCanAccessRegion(probationRegionId: UUID) =
    userCanAccessRegion(userService.getUserForRequest(), probationRegionId)

  fun userCanAccessRegion(user: UserEntity, probationRegionId: UUID) =
    userHasAllRegionsAccess(user) || user.probationRegion.id == probationRegionId

  fun currentUserHasAllRegionsAccess() = userHasAllRegionsAccess(userService.getUserForRequest())

  fun userHasAllRegionsAccess(user: UserEntity) =
    when (currentRequest.getHeader("X-Service-Name")) {
      // TODO: Revisit once Temporary Accommodation introduces user roles
      ServiceName.temporaryAccommodation.value -> false
      // TODO: Revisit if Approved Premises introduces region-limited access
      else -> true
    }
}
