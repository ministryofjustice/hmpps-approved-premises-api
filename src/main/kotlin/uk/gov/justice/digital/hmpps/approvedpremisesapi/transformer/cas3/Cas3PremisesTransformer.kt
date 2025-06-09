package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LocalAuthorityAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationDeliveryUnitTransformer

@Component
class Cas3PremisesTransformer(
  private val localAuthorityAreaTransformer: LocalAuthorityAreaTransformer,
  private val cas3CharacteristicTransformer: Cas3CharacteristicTransformer,
  private val probationDeliveryUnitTransformer: ProbationDeliveryUnitTransformer,
) {
  fun transformJpaToApi(jpa: Cas3PremisesEntity, totalBeds: Int, availableBedsForToday: Int) = Cas3Premises(
    id = jpa.id,
    name = jpa.name,
    addressLine1 = jpa.addressLine1,
    addressLine2 = jpa.addressLine2,
    town = jpa.town,
    postcode = jpa.postcode,
    bedCount = totalBeds,
    notes = jpa.notes,
    availableBedsForToday = availableBedsForToday,
    localAuthorityArea = jpa.localAuthorityArea?.let { localAuthorityAreaTransformer.transformJpaToApi(it) },
    characteristics = jpa.characteristics.map(cas3CharacteristicTransformer::transformJpaToApi),
    status = jpa.status,
    pdu = jpa.probationDeliveryUnit.name,
    probationDeliveryUnit = probationDeliveryUnitTransformer.transformJpaToApi(jpa.probationDeliveryUnit),
  )
}
