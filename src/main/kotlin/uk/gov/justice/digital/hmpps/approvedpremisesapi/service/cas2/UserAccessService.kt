package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity

@Service("Cas2UserAccessService")
class UserAccessService() {
  fun userCanViewApplication(user: UserEntity, application: Cas2ApplicationEntity): Boolean {
    return (user.id == application.createdByUser.id)
  }
}
