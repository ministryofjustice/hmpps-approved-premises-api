package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ExternalUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity

@Component
class ExternalUserTransformer {

  fun transformJpaToApi(cas2UserEntity: Cas2UserEntity): ExternalUser = ExternalUser(
    id = cas2UserEntity.id,
    username = cas2UserEntity.username,
    origin = cas2UserEntity.externalType,
    name = cas2UserEntity.name,
    email = cas2UserEntity.email!!,
  )
}
