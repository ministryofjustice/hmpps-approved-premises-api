package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity

@Component
class CharacteristicTransformer {

  fun transformJpaToApi(jpa: CharacteristicEntity) = Characteristic(
    id = jpa.id,
    name = jpa.name,
    propertyName = jpa.propertyName,
    serviceScope = when (jpa.serviceScope) {
      "approved-premises" -> Characteristic.ServiceScope.APPROVED_MINUS_PREMISES
      "temporary-accommodation" -> Characteristic.ServiceScope.TEMPORARY_MINUS_ACCOMMODATION
      "*" -> Characteristic.ServiceScope.STAR
      else -> throw RuntimeException("Unsupported service scope: ${jpa.serviceScope}")
    },
    modelScope = when (jpa.modelScope) {
      "premises" -> Characteristic.ModelScope.PREMISES
      "room" -> Characteristic.ModelScope.ROOM
      "*" -> Characteristic.ModelScope.STAR
      else -> throw RuntimeException("Unsupported service scope: ${jpa.modelScope}")
    },
  )
}
