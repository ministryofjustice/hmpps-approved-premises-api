package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2User

@Component
class Cas2UserTransformer {

  fun transformJpaToApi(cas2UserEntity: Cas2UserEntity): Cas2User = Cas2User(
    id = cas2UserEntity.id,
    username = cas2UserEntity.username,
    externalOrigin = cas2UserEntity.externalOrigin,
    name = cas2UserEntity.name,
    email = cas2UserEntity.email,
    deliusStaffCode = cas2UserEntity.deliusStaffCode,
    deliusTeamCodes = cas2UserEntity.deliusTeamCodes,
    isActive = cas2UserEntity.isActive,
    isEnabled = cas2UserEntity.isEnabled,
    nomisAccountType = cas2UserEntity.nomisAccountType,
    nomisStaffId = cas2UserEntity.nomisStaffId,
    userType = cas2UserEntity.userType,
  )
}
