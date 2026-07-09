package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TierFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase

fun IntegrationTestBase.givenACase(
  crn: String,
  tierV2: Tier? = TierFactory().produce(),
  tierV3: Tier? = TierFactory().produce(),
): CaseEntity {
  val case = caseEntityFactory.produceAndPersist {
    withCrn(crn)
    withTierV2(tierV2)
    withTierV3(tierV3)
  }

  return case
}
