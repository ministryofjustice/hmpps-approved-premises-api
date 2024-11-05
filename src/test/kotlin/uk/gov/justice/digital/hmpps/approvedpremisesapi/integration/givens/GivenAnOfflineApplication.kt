package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

@SuppressWarnings("LongParameterList")
fun IntegrationTestBase.givenAnOfflineApplication(
  crn: String,
  eventNumber: String? = randomStringMultiCaseWithNumbers(6),
) = offlineApplicationEntityFactory.produceAndPersist {
  withCrn(crn)
  withEventNumber(eventNumber)
}
