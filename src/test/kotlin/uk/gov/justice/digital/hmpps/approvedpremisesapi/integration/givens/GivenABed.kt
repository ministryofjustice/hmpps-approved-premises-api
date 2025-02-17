package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity

fun IntegrationTestBase.givenAnApprovedPremisesBed(
  premises: ApprovedPremisesEntity? = null,
  characteristics: List<CharacteristicEntity> = emptyList(),
  block: ((bed: BedEntity) -> Unit)? = null,
): BedEntity {
  val resolvedPremises = premises ?: approvedPremisesEntityFactory.produceAndPersist {
    withProbationRegion(givenAProbationRegion())
    withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
  }

  val room = roomEntityFactory.produceAndPersist {
    withPremises(resolvedPremises)
    withCharacteristics(characteristics.toMutableList())
  }

  val bed = bedEntityFactory.produceAndPersist {
    withRoom(room)
  }

  if (block != null) {
    block(bed)
  }

  return bed
}
