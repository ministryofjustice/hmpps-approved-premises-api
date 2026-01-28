package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity

@Service
class Cas2UserAccessService {
  fun userCanViewApplication(user: Cas2UserEntity, application: Cas2ApplicationEntity): Boolean {
    /*this needs to be refactored, but it involves refactoring other classes. This will be done
    in CAS-1598.
    For now, we'll only use the new logic if the application has been transferred (so more than one assignment), otherwise use existing.
     */
    if (application.isTransferredApplication()) {
      return user.id == application.currentPomUserId ||
        // user is currently assigned POM
        (user.activeNomisCaseloadId != null && user.activeNomisCaseloadId == application.currentPrisonCode) // user is in same prison
    }

    return if (user.id == application.createdByUser.id) {
      true
    } else if (application.submittedAt == null) {
      false
    } else {
      offenderIsFromSamePrisonAsUser(application.referringPrisonCode, user.activeNomisCaseloadId)
    }
  }

  fun offenderIsFromSamePrisonAsUser(referringPrisonCode: String?, activeNomisCaseloadId: String?): Boolean = if (referringPrisonCode !== null && activeNomisCaseloadId !== null) {
    activeNomisCaseloadId == referringPrisonCode
  } else {
    false
  }
}
