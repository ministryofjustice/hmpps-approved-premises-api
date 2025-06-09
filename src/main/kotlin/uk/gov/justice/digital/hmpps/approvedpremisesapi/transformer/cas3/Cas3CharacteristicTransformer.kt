package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3PremisesCharacteristicEntity

@Component
class Cas3CharacteristicTransformer {
  fun transformJpaToApi(jpa: Cas3PremisesCharacteristicEntity) = Cas3Characteristic(
    id = jpa.id,
    name = jpa.name,
    code = jpa.code,
  )
}
