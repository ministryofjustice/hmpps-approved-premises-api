package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

fun IntegrationTestBase.givenAnApprovedPremises(
  name: String = randomStringMultiCaseWithNumbers(8),
  supportsSpaceBookings: Boolean = false,
  region: ProbationRegionEntity? = null,
): ApprovedPremisesEntity = approvedPremisesEntityFactory
  .produceAndPersist {
    withName(name)
    withProbationRegion(region ?: probationRegionEntityFactory.produceAndPersist())
    withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
    withSupportsSpaceBookings(supportsSpaceBookings)
  }
