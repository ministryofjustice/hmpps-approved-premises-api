package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

fun IntegrationTestBase.givenAnApprovedPremisesRoom(
  premises: ApprovedPremisesEntity? = null,
  code: String = randomStringMultiCaseWithNumbers(6),
  name: String = randomStringMultiCaseWithNumbers(8),
  bedCount: Int = 0,
): RoomEntity {
  val resolvedPremises = premises ?: approvedPremisesEntityFactory.produceAndPersist {
    withProbationRegion(givenAProbationRegion())
    withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
  }

  val room = roomEntityFactory.produceAndPersist {
    withPremises(resolvedPremises)
    withCode(code)
    withName(name)
  }

  repeat(bedCount) {
    givenAnApprovedPremisesBed()
  }

  return room
}
