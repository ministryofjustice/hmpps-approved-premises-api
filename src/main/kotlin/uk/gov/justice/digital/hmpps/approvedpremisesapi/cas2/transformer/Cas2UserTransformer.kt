package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2DeliusUserInfoDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2TypedUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.ProbationAreaDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.StaffDetail

@Component
class Cas2UserTransformer {

  fun transformJpaToApi(userEntity: Cas2UserEntity): Cas2User = Cas2User(
    id = userEntity.id,
    username = userEntity.username,
    name = userEntity.name,
    email = userEntity.email,
    isActive = userEntity.isActive,
    authSource = Cas2User.AuthSource.valueOf(userEntity.userType.authSource),
  )

  fun transformJpaToTypedNomisUser(userEntity: Cas2UserEntity): Cas2TypedUser.Nomis = Cas2TypedUser.Nomis(
    id = userEntity.id,
    username = userEntity.username,
    name = userEntity.name,
    email = userEntity.email,
    isActive = userEntity.isActive,
    isEnabled = userEntity.isEnabled,
    nomisStaffId = userEntity.nomisStaffId,
    activeNomisCaseloadId = userEntity.activeNomisCaseloadId,
    serviceOrigin = userEntity.serviceOrigin,
  )

  fun transformJpaToTypedDeliusUser(userEntity: Cas2UserEntity, deliusUser: StaffDetail? = null): Cas2TypedUser.Delius = Cas2TypedUser.Delius(
    id = userEntity.id,
    username = userEntity.username,
    name = userEntity.name,
    email = userEntity.email,
    isActive = userEntity.isActive,
    isEnabled = userEntity.isEnabled,
    deliusStaffCode = userEntity.deliusStaffCode,
    deliusTeamCodes = userEntity.deliusTeamCodes,
    serviceOrigin = userEntity.serviceOrigin,
    deliusUserInfo = deliusUser?.let {
      Cas2DeliusUserInfoDto(
        ProbationAreaDto(
          it.probationArea.code,
          it.probationArea.description,
        ),
      )
    },
  )

  fun transformJpaToTypedExternalUser(userEntity: Cas2UserEntity): Cas2TypedUser.External = Cas2TypedUser.External(
    id = userEntity.id,
    username = userEntity.username,
    name = userEntity.name,
    email = userEntity.email,
    isActive = userEntity.isActive,
    isEnabled = userEntity.isEnabled,
    externalType = userEntity.externalType,
    serviceOrigin = userEntity.serviceOrigin,
  )
}
