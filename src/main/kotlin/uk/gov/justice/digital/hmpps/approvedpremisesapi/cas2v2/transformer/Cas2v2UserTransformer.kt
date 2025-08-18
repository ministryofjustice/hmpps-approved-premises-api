package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserEntity
import java.time.OffsetDateTime

@Component
class Cas2v2UserTransformer {

  fun transformJpaToApi(userEntity: Cas2UserEntity): Cas2v2User = Cas2v2User(
    id = userEntity.id,
    username = userEntity.username,
    name = userEntity.name,
    email = userEntity.email,
    isActive = userEntity.isActive,
    authSource = Cas2v2User.AuthSource.valueOf(userEntity.userType.authSource),
  )
}

// TODO besscerule all the below must be removed once we have narrowed down to one user type - Cas2User

fun transformCas2UserEntityToNomisUserEntity(userEntity: Cas2UserEntity): NomisUserEntity = NomisUserEntity(
  id = userEntity.id,
  nomisUsername = userEntity.username,
  name = userEntity.name,
  email = userEntity.email!!,
  createdAt = OffsetDateTime.now(),
  nomisStaffId = userEntity.nomisStaffId!!,
  accountType = "TODO",
  isEnabled = userEntity.isEnabled,
  isActive = userEntity.isActive,
  applications = userEntity.applications,
  activeCaseloadId = userEntity.activeNomisCaseloadId,
)

fun transformNomisUserEntityToCas2UserEntity(userEntity: NomisUserEntity): Cas2UserEntity = Cas2UserEntity(
  id = userEntity.id,
  username = userEntity.nomisUsername,
  name = userEntity.name,
  email = userEntity.email,
  activeNomisCaseloadId = userEntity.activeCaseloadId,
  createdAt = OffsetDateTime.now(),
  deliusStaffCode = null,
  deliusTeamCodes = null,
  userType = Cas2UserType.NOMIS,
  isActive = userEntity.isActive,
  isEnabled = userEntity.isEnabled,
  nomisStaffId = userEntity.nomisStaffId,
  applications = userEntity.applications,
)

fun transformCas2UserEntityToExternalUserEntity(userEntity: Cas2UserEntity): ExternalUserEntity = ExternalUserEntity(
  id = userEntity.id,
  name = userEntity.name,
  isEnabled = userEntity.isEnabled,
  username = userEntity.username,
  origin = "TODO",
  email = userEntity.email!!,
  createdAt = OffsetDateTime.now(),
)

fun transformExternalUserEntityToCas2UserEntity(userEntity: ExternalUserEntity): Cas2UserEntity = Cas2UserEntity(
  id = userEntity.id,
  username = userEntity.username,
  name = userEntity.name,
  email = userEntity.email,
  activeNomisCaseloadId = null,
  createdAt = OffsetDateTime.now(),
  deliusStaffCode = null,
  deliusTeamCodes = null,
  userType = Cas2UserType.EXTERNAL,
  isActive = true,
  isEnabled = userEntity.isEnabled,
  nomisStaffId = null,
  applications = mutableListOf(),
)
