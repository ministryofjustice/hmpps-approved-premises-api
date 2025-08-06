package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.OffsetDateTime

@SuppressWarnings("LongParameterList")
fun IntegrationTestBase.givenAnOfflineApplication(
  crn: String,
  eventNumber: String? = randomStringMultiCaseWithNumbers(6),
  eventNumberSet: Boolean = true,
  name: String? = null,
  createdAt: OffsetDateTime = OffsetDateTime.now(),
) = offlineApplicationEntityFactory.produceAndPersist {
  withCrn(crn)
  withEventNumber(
    if (eventNumberSet) {
      eventNumber
    } else {
      null
    },
  )
  withName(name)
  withCreatedAt(createdAt)
}
