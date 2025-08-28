package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NomisUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity

@Component
class NomisUserTransformer {

  fun transformJpaToApi(jpa: Cas2ApplicationEntity): NomisUser = NomisUser(
    id = jpa.createdByUser.id,
    name = jpa.createdByUser.name,
    nomisUsername = jpa.createdByUser.username,
    isActive = jpa.createdByUser.isActive,
    email = jpa.createdByUser.email,
  )

  // BAIL-WIP overload so the transformer will take both entity types and still return the badly named nomis user
  fun transformJpaToApi(cas2UserEntity: Cas2UserEntity): NomisUser = NomisUser(
    id = cas2UserEntity.id,
    nomisUsername = cas2UserEntity.username,
    name = cas2UserEntity.name,
    email = cas2UserEntity.email,
    isActive = cas2UserEntity.isActive,
  )
}
