package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate

fun IntegrationTestBase.givenAnApprovedPremisesBed(
  premises: ApprovedPremisesEntity? = null,
  bedCode: String = randomStringMultiCaseWithNumbers(6),
  characteristics: List<CharacteristicEntity> = emptyList(),
  endDate: LocalDate? = null,
  block: ((bed: Cas1BedEntity) -> Unit)? = null,
): Cas1BedEntity {
  val resolvedPremises = premises ?: givenAnApprovedPremises()

  val room = roomEntityFactory.produceAndPersist {
    withPremises(resolvedPremises)
    withCharacteristics(characteristics.toMutableList())
  }

  val bed = cas1BedEntityFactory.produceAndPersist {
    withRoom(room)
    withCode(bedCode)
    withEndDate(endDate)
  }

  if (block != null) {
    block(bed)
  }

  return bed
}
