package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity

@Component
class PremisesTransformer(
  private val probationRegionTransformer: ProbationRegionTransformer,
  private val apAreaTransformer: ApAreaTransformer,
  private val localAuthorityAreaTransformer: LocalAuthorityAreaTransformer,
  private val characteristicTransformer: CharacteristicTransformer,
  private val probationDeliveryUnitTransformer: ProbationDeliveryUnitTransformer,
) {
  fun transformJpaToApi(jpa: PremisesEntity, totalBeds: Int, availableBedsForToday: Int): Premises = when (jpa) {
    is ApprovedPremisesEntity -> ApprovedPremises(
      id = jpa.id,
      name = jpa.name,
      apCode = jpa.apCode,
      addressLine1 = jpa.addressLine1,
      addressLine2 = jpa.addressLine2,
      town = jpa.town,
      postcode = jpa.postcode,
      bedCount = totalBeds,
      service = ServiceName.approvedPremises.value,
      notes = jpa.notes,
      availableBedsForToday = availableBedsForToday,
      probationRegion = probationRegionTransformer.transformJpaToApi(jpa.probationRegion),
      apArea = apAreaTransformer.transformJpaToApi(jpa.probationRegion.apArea!!),
      localAuthorityArea = localAuthorityAreaTransformer.transformJpaToApi(jpa.localAuthorityArea!!),
      characteristics = jpa.characteristics.map(characteristicTransformer::transformJpaToApi),
      status = jpa.status,
    )
    is TemporaryAccommodationPremisesEntity -> TemporaryAccommodationPremises(
      id = jpa.id,
      name = jpa.name,
      addressLine1 = jpa.addressLine1,
      addressLine2 = jpa.addressLine2,
      town = jpa.town,
      postcode = jpa.postcode,
      bedCount = totalBeds,
      service = ServiceName.temporaryAccommodation.value,
      notes = jpa.notes,
      availableBedsForToday = availableBedsForToday,
      probationRegion = probationRegionTransformer.transformJpaToApi(jpa.probationRegion),
      apArea = apAreaTransformer.transformJpaToApi(jpa.probationRegion.apArea!!),
      localAuthorityArea = jpa.localAuthorityArea?.let { localAuthorityAreaTransformer.transformJpaToApi(it) },
      characteristics = jpa.characteristics.map(characteristicTransformer::transformJpaToApi),
      status = jpa.status,
      pdu = jpa.probationDeliveryUnit?.name ?: "Not specified",
      probationDeliveryUnit = jpa.probationDeliveryUnit?.let { probationDeliveryUnitTransformer.transformJpaToApi(it) },
      turnaroundWorkingDayCount = jpa.turnaroundWorkingDayCount,
    )
    else -> throw RuntimeException("Unsupported PremisesEntity type: ${jpa::class.qualifiedName}")
  }
}
