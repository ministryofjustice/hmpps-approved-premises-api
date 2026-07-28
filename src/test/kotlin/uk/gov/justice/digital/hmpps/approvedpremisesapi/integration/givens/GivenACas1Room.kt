package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

fun IntegrationTestBase.givenAnApprovedPremisesRoom(
  premises: ApprovedPremisesEntity? = null,
  code: String = randomStringMultiCaseWithNumbers(6),
  name: String = randomStringMultiCaseWithNumbers(8),
  bedCount: Int = 0,
  characteristics: List<CharacteristicEntity> = emptyList(),
): Cas1RoomEntity {
  val resolvedPremises = premises ?: approvedPremisesEntityFactory.produceAndPersist {
    withProbationRegion(givenAProbationRegion())
    withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
  }

  val room = cas1RoomEntityFactory.produceAndPersist {
    withPremises(resolvedPremises)
    withCode(code)
    withName(name)
    withCharacteristics(characteristics)
  }

  repeat(bedCount) {
    givenAnApprovedPremisesBed()
  }

  return room
}
