package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.UsersCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1UpdateUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import java.util.UUID

class Cas1UsersController : UsersCas1Delegate {

  override fun deleteUser(id: UUID): ResponseEntity<Unit> {
    return super.deleteUser(id)
  }

  override fun getUser(id: UUID): ResponseEntity<ApprovedPremisesUser> {
    return super.getUser(id)
  }

  override fun updateUser(id: UUID, updateUser: Cas1UpdateUser): ResponseEntity<User> {
    return super.updateUser(id, updateUser)
  }
}
