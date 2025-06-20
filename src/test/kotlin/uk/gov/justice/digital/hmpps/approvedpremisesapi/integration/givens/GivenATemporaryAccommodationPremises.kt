package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity

fun IntegrationTestBase.givenATemporaryAccommodationPremises(
  region: ProbationRegionEntity = givenAProbationRegion(),
) = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
  withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
  withYieldedProbationRegion { region }
}
