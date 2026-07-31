package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1CharacteristicEntity

@Component
class Cas1CharacteristicTransformer {

  fun transformJpaToApi(jpa: Cas1CharacteristicEntity) = Characteristic(
    id = jpa.id,
    name = jpa.name,
    propertyName = jpa.propertyName,
    serviceScope = Characteristic.ServiceScope.approvedMinusPremises,
    modelScope = when (jpa.modelScope) {
      "premises" -> Characteristic.ModelScope.premises
      "room" -> Characteristic.ModelScope.room
      "*" -> Characteristic.ModelScope.star
      else -> throw IllegalArgumentException("Unsupported model scope: ${jpa.modelScope}")
    },
  )
}
