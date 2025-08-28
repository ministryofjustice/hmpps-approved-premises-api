package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ExternalUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity

@Component
class ExternalUserTransformer {

  fun transformJpaToApi(externalUserEntity: Cas2UserEntity): ExternalUser = ExternalUser(
    id = externalUserEntity.id,
    username = externalUserEntity.username,
    origin = externalUserEntity.externalType,
    name = externalUserEntity.name,
    email = externalUserEntity.email!!,
  )
}
