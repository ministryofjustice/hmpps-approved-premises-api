package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

fun IntegrationTestBase.givenAnApprovedPremises(
  name: String = randomStringMultiCaseWithNumbers(8),
  qCode: String = randomStringUpperCase(4),
  supportsSpaceBookings: Boolean = false,
  region: ProbationRegionEntity? = null,
): ApprovedPremisesEntity = approvedPremisesEntityFactory
  .produceAndPersist {
    withName(name)
    withQCode(qCode)
    withProbationRegion(region ?: probationRegionEntityFactory.produceAndPersist())
    withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
    withSupportsSpaceBookings(supportsSpaceBookings)
  }
