package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity

@Component
class PremisesTransformer(
  private val probationRegionTransformer: ProbationRegionTransformer,
  private val apAreaTransformer: ApAreaTransformer,
  private val localAuthorityAreaTransformer: LocalAuthorityAreaTransformer
) {
  fun transformJpaToApi(jpa: PremisesEntity, availableBedsForToday: Int) = Premises(
    id = jpa.id,
    name = jpa.name,
    apCode = jpa.approvedPremisesOnlyProperty { apCode },
    addressLine1 = jpa.addressLine1,
    postcode = jpa.postcode,
    bedCount = jpa.totalBeds,
    service = if (jpa is TemporaryAccommodationPremisesEntity) "CAS3" else "CAS1",
    notes = jpa.notes,
    availableBedsForToday = availableBedsForToday,
    probationRegion = probationRegionTransformer.transformJpaToApi(jpa.probationRegion),
    apArea = apAreaTransformer.transformJpaToApi(jpa.probationRegion.apArea),
    localAuthorityArea = localAuthorityAreaTransformer.transformJpaToApi(jpa.localAuthorityArea)
  )

  private fun <T> PremisesEntity.approvedPremisesOnlyProperty(selector: ApprovedPremisesEntity.() -> T?) = if (this is ApprovedPremisesEntity) {
    selector()
  } else {
    null
  }

  private fun <T> PremisesEntity.temporaryAccommodationOnlyProperty(selector: TemporaryAccommodationPremisesEntity.() -> T?) = if (this is TemporaryAccommodationPremisesEntity) {
    selector()
  } else {
    null
  }
}
