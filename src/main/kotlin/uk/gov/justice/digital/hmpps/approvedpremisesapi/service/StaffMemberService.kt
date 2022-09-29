package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffMember

@Service
class StaffMemberService {
  fun getStaffMemberById(id: Long): AuthorisableActionResult<StaffMember> {
    return AuthorisableActionResult.Success(
      StaffMember(
        staffCode = "TODO",
        staffIdentifier = id,
        staffInfo = StaffInfo(
          forenames = "TODO",
          surname = "TODO"
        )
      )
    )
  }
}
