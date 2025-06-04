package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2.NomisUserEntity

@Service
class Cas2UserAccessService {
  fun userCanViewApplication(user: NomisUserEntity, application: Cas2ApplicationEntity): Boolean {
    /*this needs to be refactored, but it involves refactoring other classes. This will be done
    in CAS-1598.
    For now, we'll only use the new logic if the application has been transferred (so more than one assignment), otherwise use existing.
     */
    if (application.isTransferredApplication()) {
      return user.id == application.currentPomUserId ||
        // user is currently assigned POM
        (user.activeCaseloadId != null && user.activeCaseloadId == application.currentPrisonCode) // user is in same prison
    }

    return if (user.id == application.getCreatedById()) {
      true
    } else if (application.submittedAt == null) {
      false
    } else {
      offenderIsFromSamePrisonAsUser(application.referringPrisonCode, user.activeCaseloadId)
    }
  }

  fun offenderIsFromSamePrisonAsUser(referringPrisonCode: String?, activeCaseloadId: String?): Boolean = if (referringPrisonCode !== null && activeCaseloadId !== null) {
    activeCaseloadId == referringPrisonCode
  } else {
    false
  }
}
