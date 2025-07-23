package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.util.UUID

@Service
class Cas3UserAccessService(
  private val userAccessService: UserAccessService,
  private val userService: UserService,
) {
  fun canViewVoidBedspaces(probationRegionId: UUID): Boolean {
    val user = userService.getUserForRequest()
    return user.hasRole(UserRole.CAS3_ASSESSOR) &&
      userAccessService.userCanAccessRegion(
        user,
        ServiceName.temporaryAccommodation,
        probationRegionId,
      )
  }
}
