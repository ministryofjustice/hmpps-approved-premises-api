package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserType

@Service("Cas2v2UserAccessService")
class Cas2v2UserAccessService(
  private val cas2v2UserService: Cas2v2UserService,
) {
  fun userCanViewCas2v2Application(user: Cas2v2UserEntity, application: Cas2v2ApplicationEntity): Boolean = if (user.id == application.createdByUser.id) {
    true
  } else if (application.applicationOrigin == ApplicationOrigin.prisonBail &&
    cas2v2UserService.userForRequestHasRole(
      listOf(
        SimpleGrantedAuthority("ROLE_CAS2_PRISON_BAIL_REFERRER"),
      ),
    )
  ) {
    true
  } else if (application.submittedAt == null) {
    false
  } else if (user.userType == Cas2v2UserType.NOMIS) {
    offenderIsFromSamePrisonAsUser(application.referringPrisonCode, user.activeNomisCaseloadId)
  } else {
    false
  }

  fun userCanAddNote(user: Cas2v2UserEntity, application: Cas2v2ApplicationEntity): Boolean = when (user.userType) {
    Cas2v2UserType.NOMIS ->
      user.id == application.createdByUser.id ||
        offenderIsFromSamePrisonAsUser(
          application.referringPrisonCode,
          user.activeNomisCaseloadId,
        )

    Cas2v2UserType.DELIUS -> user.id == application.createdByUser.id
    Cas2v2UserType.EXTERNAL -> true
  }

  fun offenderIsFromSamePrisonAsUser(referringPrisonCode: String?, activeCaseloadId: String?): Boolean = if (referringPrisonCode !== null && activeCaseloadId !== null) {
    activeCaseloadId == referringPrisonCode
  } else {
    false
  }
}
