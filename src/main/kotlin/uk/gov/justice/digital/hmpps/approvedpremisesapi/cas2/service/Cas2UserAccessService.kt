package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType

@Service
class Cas2UserAccessService(
  private val cas2UserService: Cas2UserService,
) {
  fun userCanViewApplication(user: Cas2UserEntity, application: Cas2ApplicationEntity): Boolean {
    val isPrisonBailReferral = application.applicationOrigin == ApplicationOrigin.prisonBail &&
      application.submittedAt != null &&
      cas2UserService.userForRequestHasRole(
        listOf(SimpleGrantedAuthority("ROLE_CAS2_PRISON_BAIL_REFERRER")),
      )

    if (isPrisonBailReferral) return true
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
    } else if (user.userType == Cas2UserType.NOMIS) {
      offenderIsFromSamePrisonAsUser(application.referringPrisonCode, user.activeNomisCaseloadId)
    } else {
      false
    }
  }

  fun userCanAddNote(user: Cas2UserEntity, application: Cas2ApplicationEntity): Boolean {
    if (
      application.applicationOrigin == ApplicationOrigin.prisonBail &&
      cas2UserService.userForRequestHasRole(
        listOf(
          SimpleGrantedAuthority("ROLE_CAS2_PRISON_BAIL_REFERRER"),
        ),
      )
    ) {
      return true
    }

    return when (user.userType) {
      Cas2UserType.NOMIS ->
        user.id == application.createdByUser.id ||
          offenderIsFromSamePrisonAsUser(
            application.referringPrisonCode,
            user.activeNomisCaseloadId,
          )
      Cas2UserType.DELIUS -> user.id == application.createdByUser.id
      Cas2UserType.EXTERNAL -> true
    }
  }

  fun offenderIsFromSamePrisonAsUser(referringPrisonCode: String?, activeCaseloadId: String?): Boolean = if (referringPrisonCode !== null && activeCaseloadId !== null) {
    activeCaseloadId == referringPrisonCode
  } else {
    false
  }
}
