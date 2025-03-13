package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ExternalUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserEntity

@Component
class ExternalUserTransformer {

  fun transformJpaToApi(externalUserEntity: ExternalUserEntity): ExternalUser = ExternalUser(
    id = externalUserEntity.id,
    username = externalUserEntity.username,
    origin = externalUserEntity.origin,
    name = externalUserEntity.name,
    email = externalUserEntity.email,
  )
}
