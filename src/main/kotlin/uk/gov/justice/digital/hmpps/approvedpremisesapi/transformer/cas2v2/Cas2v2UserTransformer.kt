package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserEntity

@Component
class Cas2v2UserTransformer {

  fun transformJpaToApi(userEntity: Cas2v2UserEntity): Cas2v2User = Cas2v2User(
    id = userEntity.id,
    username = userEntity.username,
    name = userEntity.name,
    email = userEntity.email,
    isActive = userEntity.isActive,
    authSource = Cas2v2User.AuthSource.valueOf(userEntity.userType.authSource),
  )
}
