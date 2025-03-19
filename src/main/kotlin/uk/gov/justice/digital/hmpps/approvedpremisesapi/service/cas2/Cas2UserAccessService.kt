package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity

@Service
class Cas2UserAccessService {
  fun userCanViewApplication(user: NomisUserEntity, application: Cas2ApplicationEntity) = isCreatedByUser(user, application) ||
    (
      application.isSubmitted() &&
        (
          isAssignedUser(user, application) ||
            offenderIsFromSamePrisonAsUser(
              application.referringPrisonCode,
              user.activeCaseloadId,
            )
          )
      )

  fun isCreatedByUser(user: NomisUserEntity, application: Cas2ApplicationEntity): Boolean = user.id == application.createdByUser.id

  fun isAssignedUser(user: NomisUserEntity, application: Cas2ApplicationEntity): Boolean = user.id == application.allocatedPomUserId

  fun offenderIsFromSamePrisonAsUser(referringPrisonCode: String?, activeCaseloadId: String?): Boolean = referringPrisonCode?.let { it == activeCaseloadId } ?: false
}
