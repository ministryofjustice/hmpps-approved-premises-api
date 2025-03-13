package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NomisUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity

@Component
class NomisUserTransformer {

  fun transformJpaToApi(nomisUserEntity: NomisUserEntity): NomisUser = NomisUser(
    id = nomisUserEntity.id,
    nomisUsername = nomisUserEntity.nomisUsername,
    name = nomisUserEntity.name,
    email = nomisUserEntity.email,
    isActive = nomisUserEntity.isActive,
  )
}
