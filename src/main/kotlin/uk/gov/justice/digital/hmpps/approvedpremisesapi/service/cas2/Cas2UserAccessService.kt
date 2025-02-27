package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity

@Service
class Cas2UserAccessService {
  fun userCanViewApplication(user: NomisUserEntity, application: Cas2ApplicationEntity): Boolean = if (user.id == application.createdByUser.id) {
    true
  } else if (application.submittedAt == null) {
    false
  } else {
    offenderIsFromSamePrisonAsUser(application.referringPrisonCode, user.activeCaseloadId)
  }

  fun offenderIsFromSamePrisonAsUser(referringPrisonCode: String?, activeCaseloadId: String?): Boolean = if (referringPrisonCode !== null && activeCaseloadId !== null) {
    activeCaseloadId == referringPrisonCode
  } else {
    false
  }
}
