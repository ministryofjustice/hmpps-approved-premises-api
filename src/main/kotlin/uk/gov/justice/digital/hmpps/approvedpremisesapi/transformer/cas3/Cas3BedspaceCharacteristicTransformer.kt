package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3BedspaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BedspaceCharacteristicEntity

@Component
class Cas3BedspaceCharacteristicTransformer {

  fun transformJpaToApi(jpa: Cas3BedspaceCharacteristicEntity) = Cas3BedspaceCharacteristic(
    id = jpa.id,
    name = jpa.name,
    description = jpa.description,
  )
}
