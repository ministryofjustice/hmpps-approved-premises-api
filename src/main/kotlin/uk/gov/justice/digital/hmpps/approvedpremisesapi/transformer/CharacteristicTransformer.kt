package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity

@Component
class CharacteristicTransformer {

  fun transformJpaToApi(jpa: CharacteristicEntity) = Characteristic(
    id = jpa.id,
    name = jpa.name,
    serviceScope = jpa.serviceScope,
    modelScope = jpa.modelScope
  )
}
