package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import java.util.UUID

fun IntegrationTestBase.`Given a Probation Region`(
  id: UUID = UUID.randomUUID(),
  name: String? = null,
  apArea: ApAreaEntity? = null,
  deliusCode: String? = null,
  block: ((probationRegion: ProbationRegionEntity) -> Unit)? = null,
): ProbationRegionEntity {
  val probationRegion = probationRegionEntityFactory.produceAndPersist {
    withId(id)
    if (name != null) {
      withName(name)
    }
    if (apArea != null) {
      withApArea(apArea)
    } else {
      withYieldedApArea {
        `Given an AP Area`()
      }
    }
    if (deliusCode != null) {
      withDeliusCode(deliusCode)
    }
  }

  if (block != null) {
    block(probationRegion)
  }

  return probationRegion
}
