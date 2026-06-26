package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2TypedUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity

@Service
class Cas2UserAccessService(
  private val cas2UserService: Cas2UserService,
) {
  fun userCanViewCas2Application(
    user: Cas2TypedUser,
    application: Cas2ApplicationEntity,
  ): Boolean {
    val isPrisonBailReferral = application.applicationOrigin == ApplicationOrigin.prisonBail &&
      application.submittedAt != null &&
      cas2UserService.userForRequestHasRole(
        listOf(SimpleGrantedAuthority("ROLE_CAS2_PRISON_BAIL_REFERRER")),
      )

    if (isPrisonBailReferral) return true

    if (user.id == application.createdByUser.id) return true

    if (application.submittedAt == null) return false

    return if (user is Cas2TypedUser.Nomis) {
      offenderIsFromSamePrisonAsUser(application.referringPrisonCode, user.activeNomisCaseloadId)
    } else {
      false
    }
  }

  fun userCanAddNote(user: Cas2TypedUser, application: Cas2ApplicationEntity): Boolean {
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

    return when (user) {
      is Cas2TypedUser.Nomis ->
        user.id == application.createdByUser.id ||
          offenderIsFromSamePrisonAsUser(
            application.referringPrisonCode,
            user.activeNomisCaseloadId,
          )
      is Cas2TypedUser.Delius -> user.id == application.createdByUser.id
      is Cas2TypedUser.External -> true
    }
  }

  fun offenderIsFromSamePrisonAsUser(referringPrisonCode: String?, activeCaseloadId: String?): Boolean = if (referringPrisonCode !== null && activeCaseloadId !== null) {
    activeCaseloadId == referringPrisonCode
  } else {
    false
  }
}
