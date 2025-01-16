package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserType

@Service("Cas2v2UserAccessService")
class Cas2v2UserAccessService {
  fun userCanViewCas2v2Application(user: Cas2v2UserEntity, application: Cas2v2ApplicationEntity): Boolean {
    return if (user.id == application.createdByUser.id) {
      true
    } else if (application.submittedAt == null) {
      false
    } else if (user.userType == Cas2v2UserType.NOMIS) {
      offenderIsFromSamePrisonAsUser(application.referringPrisonCode, user.activeNomisCaseloadId)
    } else {
      false
    }
  }

  fun userCanAddNote(user: Cas2v2UserEntity, application: Cas2v2ApplicationEntity): Boolean = when (user.userType) {
    Cas2v2UserType.NOMIS -> user.id == application.createdByUser.id || offenderIsFromSamePrisonAsUser(
      application.referringPrisonCode,
      user.activeNomisCaseloadId,
    )

    Cas2v2UserType.DELIUS -> user.id == application.createdByUser.id
    Cas2v2UserType.EXTERNAL -> true
  }

  fun offenderIsFromSamePrisonAsUser(referringPrisonCode: String?, activeCaseloadId: String?): Boolean {
    return if (referringPrisonCode !== null && activeCaseloadId !== null) {
      activeCaseloadId == referringPrisonCode
    } else {
      false
    }
  }
}
