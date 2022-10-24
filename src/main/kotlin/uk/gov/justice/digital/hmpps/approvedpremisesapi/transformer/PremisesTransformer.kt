package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity

@Component
class PremisesTransformer(
  private val probationRegionTransformer: ProbationRegionTransformer,
  private val apAreaTransformer: ApAreaTransformer,
  private val localAuthorityAreaTransformer: LocalAuthorityAreaTransformer
) {
  fun transformJpaToApi(jpa: PremisesEntity, availableBedsForToday: Int) = Premises(
    id = jpa.id,
    name = jpa.name,
    apCode = jpa.apCode,
    addressLine1 = jpa.addressLine1,
    postcode = jpa.postcode,
    bedCount = jpa.totalBeds,
    service = jpa.service,
    notes = jpa.notes,
    availableBedsForToday = availableBedsForToday,
    probationRegion = probationRegionTransformer.transformJpaToApi(jpa.probationRegion),
    apArea = apAreaTransformer.transformJpaToApi(jpa.probationRegion.apArea),
    localAuthorityArea = localAuthorityAreaTransformer.transformJpaToApi(jpa.localAuthorityArea)
  )
}
