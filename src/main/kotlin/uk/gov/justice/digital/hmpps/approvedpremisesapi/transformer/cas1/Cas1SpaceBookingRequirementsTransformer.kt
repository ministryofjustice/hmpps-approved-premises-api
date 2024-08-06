package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsEntity

@Component
class Cas1SpaceBookingRequirementsTransformer {
  fun transformJpaToApi(jpa: PlacementRequirementsEntity) = Cas1SpaceBookingRequirements(
    apType = jpa.apType,
    gender = jpa.gender,
    essentialCharacteristics = jpa.essentialCriteria.mapNotNull { it.asCas1SpaceCharacteristic() },
    desirableCharacteristics = jpa.desirableCriteria.mapNotNull { it.asCas1SpaceCharacteristic() },
  )

  private fun CharacteristicEntity.asCas1SpaceCharacteristic() = try {
    Cas1SpaceCharacteristic.valueOf(this.propertyName!!)
  } catch (_: Exception) {
    null
  }
}
