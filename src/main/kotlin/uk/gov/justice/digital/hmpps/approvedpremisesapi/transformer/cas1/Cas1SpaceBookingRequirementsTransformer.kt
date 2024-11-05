package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity

@Component
class Cas1SpaceBookingRequirementsTransformer {
  fun transformJpaToApi(spaceBooking: Cas1SpaceBookingEntity): Cas1SpaceBookingRequirements {
    val requirements = spaceBooking.placementRequest.placementRequirements
    return Cas1SpaceBookingRequirements(
      apType = requirements.apType,
      gender = requirements.gender,
      essentialCharacteristics = spaceBooking.criteria.map { it.asCas1SpaceCharacteristic() },
      desirableCharacteristics = emptyList(),
    )
  }

  private fun CharacteristicEntity.asCas1SpaceCharacteristic() = Cas1SpaceCharacteristic.valueOf(this.propertyName!!)
}
