package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity

@Component
class Cas1SpaceBookingRequirementsTransformer {
  fun transformJpaToApi(cas1SpaceBookingEntity: Cas1SpaceBookingEntity) = Cas1SpaceBookingRequirements(
    essentialCharacteristics = cas1SpaceBookingEntity.criteria.map { it.asCas1SpaceBookingCharacteristic() },
  )

  private fun CharacteristicEntity.asCas1SpaceBookingCharacteristic() =
    Cas1SpaceBookingCharacteristic.entries.first { it.value == this.propertyName }
}
