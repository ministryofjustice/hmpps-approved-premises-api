package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate

fun IntegrationTestBase.givenAnApprovedPremisesBed(
  premises: ApprovedPremisesEntity? = null,
  bedCode: String = randomStringMultiCaseWithNumbers(6),
  characteristics: List<CharacteristicEntity> = emptyList(),
  endDate: LocalDate? = null,
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
    withCode(bedCode)
    withEndDate(endDate)
  }

  if (block != null) {
    block(bed)
  }

  return bed
}
