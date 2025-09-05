package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service

import java.util.UUID
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService

@Service
class Cas3UserAccessService(
  private val userService: UserService,
) {
  fun canViewVoidBedspaces(probationRegionId: UUID): Boolean {
    val user = userService.getUserForRequest()
    return user.hasRole(UserRole.CAS3_ASSESSOR) && userCanAccessRegion(user, probationRegionId)
  }

  fun userCanAccessRegion(user: UserEntity, probationRegionId: UUID) = (user.probationRegion.id == probationRegionId || user.hasRole(UserRole.CAS3_REPORTER))
  fun currentUserCanAccessRegion(probationRegionId: UUID): Boolean {
    val user = userService.getUserForRequest()
    return userCanAccessRegion(user, probationRegionId)
  }
}
