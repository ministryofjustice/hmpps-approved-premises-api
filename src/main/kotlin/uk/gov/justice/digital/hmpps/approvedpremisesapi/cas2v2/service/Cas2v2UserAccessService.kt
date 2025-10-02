package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType

@Service("Cas2v2UserAccessService")
class Cas2v2UserAccessService(
  private val cas2v2UserService: Cas2v2UserService,
) {
  fun userCanViewCas2v2Application(
    user: Cas2UserEntity,
    application: Cas2ApplicationEntity,
  ): Boolean {
    val isPrisonBailReferral = application.applicationOrigin == ApplicationOrigin.prisonBail &&
      application.submittedAt != null &&
      cas2v2UserService.userForRequestHasRole(
        listOf(SimpleGrantedAuthority("ROLE_CAS2_PRISON_BAIL_REFERRER")),
      )

    if (isPrisonBailReferral) return true

    if (user.id == application.createdByUser?.id) return true

    if (application.submittedAt == null) return false

    return if (user.userType == Cas2UserType.NOMIS) {
      offenderIsFromSamePrisonAsUser(application.referringPrisonCode, user.activeNomisCaseloadId)
    } else {
      false
    }
  }

  fun userCanAddNote(user: Cas2UserEntity, application: Cas2ApplicationEntity): Boolean {
    if (
      application.applicationOrigin == ApplicationOrigin.prisonBail &&
      cas2v2UserService.userForRequestHasRole(
        listOf(
          SimpleGrantedAuthority("ROLE_CAS2_PRISON_BAIL_REFERRER"),
        ),
      )
    ) {
      return true
    }

    return when (user.userType) {
      Cas2UserType.NOMIS ->
        user.id == application.createdByUser?.id ||
          offenderIsFromSamePrisonAsUser(
            application.referringPrisonCode,
            user.activeNomisCaseloadId,
          )
      Cas2UserType.DELIUS -> user.id == application.createdByUser!!.id
      Cas2UserType.EXTERNAL -> true
    }
  }

  fun offenderIsFromSamePrisonAsUser(referringPrisonCode: String?, activeCaseloadId: String?): Boolean = if (referringPrisonCode !== null && activeCaseloadId !== null) {
    activeCaseloadId == referringPrisonCode
  } else {
    false
  }
}
