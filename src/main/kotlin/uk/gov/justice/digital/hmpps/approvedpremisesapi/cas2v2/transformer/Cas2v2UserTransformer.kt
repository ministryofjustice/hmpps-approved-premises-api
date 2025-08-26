package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
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
  email = userEntity.email,
  createdAt = OffsetDateTime.now(),
  nomisStaffId = userEntity.nomisStaffId ?: 0,
  accountType = userEntity.nomisAccountType ?: "TODO",
  isEnabled = userEntity.isEnabled,
  isActive = userEntity.isActive,
  activeCaseloadId = userEntity.activeNomisCaseloadId,
)
