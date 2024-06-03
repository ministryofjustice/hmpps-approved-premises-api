package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import java.util.UUID

fun IntegrationTestBase.givenAnApArea(
  id: UUID = UUID.randomUUID(),
): ApAreaEntity {
  val probationRegion = apAreaEntityFactory.produceAndPersist {
    withId(id)
  }

  return probationRegion
}
