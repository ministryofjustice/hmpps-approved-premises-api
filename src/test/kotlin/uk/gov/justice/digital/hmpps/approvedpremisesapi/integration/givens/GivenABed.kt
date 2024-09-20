package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity

fun IntegrationTestBase.`Given an Approved Premises Bed`(
  block: (bed: BedEntity) -> Unit,
) {
  val premises = approvedPremisesEntityFactory.produceAndPersist {
    withProbationRegion(`Given a Probation Region`())
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
