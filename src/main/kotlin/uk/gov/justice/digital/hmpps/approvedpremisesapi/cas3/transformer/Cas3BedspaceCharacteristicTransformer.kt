package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspaceCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BedspaceCharacteristic

@Component
class Cas3BedspaceCharacteristicTransformer {

  fun transformJpaToApi(jpa: Cas3BedspaceCharacteristicEntity) = Cas3BedspaceCharacteristic(
    id = jpa.id,
    name = jpa.name,
    description = jpa.description,
  )
}
