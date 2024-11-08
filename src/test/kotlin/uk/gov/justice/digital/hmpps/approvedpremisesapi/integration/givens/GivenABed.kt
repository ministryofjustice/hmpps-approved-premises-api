package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity

fun IntegrationTestBase.givenAnApprovedPremisesBed(
  block: (bed: BedEntity) -> Unit,
) {
  val premises = approvedPremisesEntityFactory.produceAndPersist {
    withProbationRegion(givenAProbationRegion())
    withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
  }

  val room = roomEntityFactory.produceAndPersist {
    withPremises(premises)
  }

  val bed = bedEntityFactory.produceAndPersist {
    withRoom(room)
  }

  block(bed)
}
